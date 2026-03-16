package org.jellyfin.playback.media3.exoplayer.compat

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.FormatHolder
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [MediaCodecVideoRenderer] that rewrites Dolby Vision Profile 7 streams as Profile 8.1
 * before presenting to MediaCodec.
 *
 * Profile 7 (DVHE.DTB) is a dual-layer format (BL + EL) rarely supported by Android TV decoders.
 * Profile 8.1 (DVHE.ST) is single-layer and broadly supported.
 *
 * This renderer patches the 4-byte DOVIDecoderConfigurationRecord (DVCC) to:
 *   - Set dv_profile  : 7 → 8
 *   - Set el_present  : 1 → 0  (strip Enhancement Layer signaling)
 *
 * Detection uses a two-tier strategy:
 * 1. Media3 MIME/codecs: if sampleMimeType=VIDEO_DOLBY_VISION with dvhe.07/dvh1.07 codecs → P7
 * 2. Jellyfin server hint ([dvP7Hint]): set when Jellyfin's pre-scan reports DOVI_WITH_EL.
 *    This handles MKV rips with bloated CodecPrivate (e.g. MakeMKV) where Media3 sets
 *    sampleMimeType=VIDEO_H265 instead of VIDEO_DOLBY_VISION, causing Media3-level detection
 *    to fail even though the DVCC is present in MKV BlockAdditionMapping.
 *
 * When DVCC is absent from initializationData, a synthetic Profile 8 DVCC is synthesized
 * from the codecs string (or defaulting to level 6 for 4K UHD when codecs is unavailable).
 *
 * For MEL sources (most UHD Blu-ray rips) this is lossless — all DV metadata is in the BL RPU.
 * For FEL sources the EL pixel enhancement is discarded; DV tone-mapping metadata is preserved.
 */
@OptIn(UnstableApi::class)
class DvCompatVideoRenderer(
	context: Context,
	codecAdapterFactory: MediaCodecAdapter.Factory,
	mediaCodecSelector: MediaCodecSelector,
	allowedJoiningTimeMs: Long,
	enableDecoderFallback: Boolean,
	private val forceCompatMode: Boolean,
	private val dvP7Hint: AtomicBoolean,
	eventHandler: Handler?,
	eventListener: VideoRendererEventListener?,
) : MediaCodecVideoRenderer(
	context,
	codecAdapterFactory,
	mediaCodecSelector,
	allowedJoiningTimeMs,
	enableDecoderFallback,
	eventHandler,
	eventListener,
	/* maxDroppedFramesToNotify= */ -1,
) {
	// ── Detection ─────────────────────────────────────────────────────────────

	/**
	 * Returns true if this is a Dolby Vision Profile 7 stream.
	 *
	 * Handles two source scenarios:
	 * 1. Media3 correctly detected DV → sampleMimeType = VIDEO_DOLBY_VISION, codecs = "dvhe.07.*"
	 * 2. Media3 detected DV but no codecs string → scan initializationData for DVCC bytes
	 * 3. Media3 missed DV detection (bloated CodecPrivate in MKV) → sampleMimeType = VIDEO_H265.
	 *    In this case fall back to [dvP7Hint] which reflects Jellyfin server's pre-scan result.
	 */
	private fun isDvProfile7(format: Format?): Boolean {
		if (format == null) return false
		return when (format.sampleMimeType) {
			MimeTypes.VIDEO_DOLBY_VISION -> {
				val codecs = format.codecs
				if (codecs != null) {
					codecs.startsWith("dvhe.07") || codecs.startsWith("dvh1.07")
				} else {
					// No codecs string (can happen with MKV) — scan initializationData for DVCC
					format.initializationData.any { bytes -> getDvccProfile(bytes) == 7 }
				}
			}
			MimeTypes.VIDEO_H265 -> {
				// Fallback: MakeMKV-style MKV where Media3 may not set DV MIME type.
				// DVCC lives in MKV BlockAdditionMapping, not initializationData — so byte
				// scanning won't find it. Use Jellyfin's authoritative server-side signal instead.
				format.initializationData.any { bytes -> getDvccProfile(bytes) == 7 }
					|| dvP7Hint.get()
			}
			else -> false
		}
	}

	/**
	 * Extracts dv_profile from a DOVIDecoderConfigurationRecord byte array.
	 *
	 * Handles two layouts:
	 * - Raw record: bytes[0]=dv_version_major(1), bytes[1]=dv_version_minor,
	 *               bytes[2:3]=packed profile/level/flags
	 * - Full dvcC/dvvC box: 4-byte size + 4-byte fourcc + raw record starting at bytes[8]
	 *
	 * Returns the profile (1–9) or -1 if not a valid DVCC record.
	 */
	private fun getDvccProfile(bytes: ByteArray): Int {
		// Layout A: raw DOVIDecoderConfigurationRecord
		// bytes[0] = dv_version_major (must be 1)
		if (bytes.size >= 4 && bytes[0].toInt() == 1) {
			val word = ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[3].toInt() and 0xFF)
			val profile = (word shr 9) and 0x7F
			if (profile in 1..9) return profile
		}
		// Layout B: full dvcC or dvvC ISO box (8-byte header before the record)
		// fourcc: "dvcC" = 0x64766343, "dvvC" = 0x64767643
		if (bytes.size >= 12) {
			val b4 = bytes[4].toInt() and 0xFF
			val b5 = bytes[5].toInt() and 0xFF
			val b6 = bytes[6].toInt() and 0xFF
			val b7 = bytes[7].toInt() and 0xFF
			val isDvcC = b4 == 0x64 && b5 == 0x76 && b6 == 0x63 && b7 == 0x43
			val isDvvC = b4 == 0x64 && b5 == 0x76 && b6 == 0x76 && b7 == 0x43
			if ((isDvcC || isDvvC) && bytes[8].toInt() == 1) {
				val word = ((bytes[10].toInt() and 0xFF) shl 8) or (bytes[11].toInt() and 0xFF)
				val profile = (word shr 9) and 0x7F
				if (profile in 1..9) return profile
			}
		}
		return -1
	}

	// ── Format patching ───────────────────────────────────────────────────────

	/**
	 * Build a new [Format] with Profile 7 → Profile 8.1 rewrite.
	 *
	 * - Updates codecs string: "dvhe.07.*" → "dvhe.08.*" (defaults to "dvhe.08.06" when absent)
	 * - Patches DVCC bytes in initializationData (profile 7→8, el_present 1→0)
	 * - If no DVCC found in initializationData, synthesizes Profile 8 DVCC from the
	 *   codecs string (or level 6 as a safe default for 4K UHD content)
	 * - Upgrades sampleMimeType from VIDEO_H265 to VIDEO_DOLBY_VISION when needed
	 */
	private fun patchToProfile8(format: Format): Format {
		// When the source format is VIDEO_H265 (Media3 missed DV detection), there's no
		// dvhe.07.xx codecs string — default to dvhe.08.06 (Profile 8.1, Level 6 / 4K UHD).
		val p8Codecs = format.codecs
			?.replace("dvhe.07", "dvhe.08")
			?.replace("dvh1.07", "dvh1.08")
			?: "dvhe.08.06"

		// Patch DVCC bytes in all initializationData entries
		var p8InitData = format.initializationData.map { bytes -> patchDvccBytes(bytes) }

		// If no Profile 8 DVCC ended up in initializationData after patching,
		// synthesize and append one so the DV hardware decoder is properly configured.
		if (p8InitData.none { bytes -> getDvccProfile(bytes) == 8 }) {
			val syntheticDvcc = buildProfile8Dvcc(format.codecs)
			Timber.d("DV compat: no DVCC in initializationData — injecting synthetic Profile 8 DVCC")
			p8InitData = p8InitData + syntheticDvcc
		}

		val builder = format.buildUpon()
			.setCodecs(p8Codecs)
			.setInitializationData(p8InitData)

		// If Media3 typed this as HEVC (missed DV detection), upgrade MIME type to DV
		// so MediaCodec uses the DV decoder path instead of the plain HEVC decoder.
		if (format.sampleMimeType == MimeTypes.VIDEO_H265) {
			builder.setSampleMimeType(MimeTypes.VIDEO_DOLBY_VISION)
		}

		return builder.build()
	}

	/**
	 * Rewrite the DOVIDecoderConfigurationRecord bytes.
	 *
	 * Byte layout (big-endian, bytes 2–3 form a 16-bit word):
	 *   Bits 15–9 : dv_profile  (7 bits)  ← 7 → 8
	 *   Bits  8–3 : dv_level    (6 bits)  ← preserved
	 *   Bit     2 : rpu_present (1 bit)   ← kept as-is
	 *   Bit     1 : el_present  (1 bit)   ← 1 → 0
	 *   Bit     0 : bl_present  (1 bit)   ← kept as-is (always 1)
	 *
	 * Handles both raw record and full dvcC/dvvC box layouts (see [getDvccProfile]).
	 */
	private fun patchDvccBytes(bytes: ByteArray): ByteArray {
		// Layout A: raw record
		if (bytes.size >= 4 && bytes[0].toInt() == 1) {
			val word = ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[3].toInt() and 0xFF)
			val profile = (word shr 9) and 0x7F
			if (profile == 7) {
				Timber.d("DV compat: patching DVCC (raw) profile 7→8, el_present 1→0")
				return patchWordAt(bytes, 2, word)
			}
		}
		// Layout B: full dvcC/dvvC box
		if (bytes.size >= 12) {
			val b4 = bytes[4].toInt() and 0xFF
			val b5 = bytes[5].toInt() and 0xFF
			val b6 = bytes[6].toInt() and 0xFF
			val b7 = bytes[7].toInt() and 0xFF
			val isDvcC = b4 == 0x64 && b5 == 0x76 && b6 == 0x63 && b7 == 0x43
			val isDvvC = b4 == 0x64 && b5 == 0x76 && b6 == 0x76 && b7 == 0x43
			if ((isDvcC || isDvvC) && bytes[8].toInt() == 1) {
				val word = ((bytes[10].toInt() and 0xFF) shl 8) or (bytes[11].toInt() and 0xFF)
				val profile = (word shr 9) and 0x7F
				if (profile == 7) {
					Timber.d("DV compat: patching DVCC (boxed) profile 7→8, el_present 1→0")
					return patchWordAt(bytes, 10, word)
				}
			}
		}
		return bytes
	}

	/** Rewrites the 16-bit packed field at [offset] in a copy of [bytes]: profile→8, el→0. */
	private fun patchWordAt(bytes: ByteArray, offset: Int, word: Int): ByteArray {
		val level = (word shr 3) and 0x3F
		val rpu = (word shr 2) and 0x01
		val newWord = (8 shl 9) or (level shl 3) or (rpu shl 2) or (0 shl 1) or 1
		val result = bytes.copyOf()
		result[offset] = ((newWord shr 8) and 0xFF).toByte()
		result[offset + 1] = (newWord and 0xFF).toByte()
		return result
	}

	/**
	 * Synthesizes a minimal Profile 8 DOVIDecoderConfigurationRecord.
	 *
	 * Extracts the level from the codecs string (e.g. "dvhe.07.06" → level 6).
	 * Falls back to level 6 (50 Mbps, suitable for 4K UHD) when the codecs string
	 * is null or not a DV-format string.
	 */
	private fun buildProfile8Dvcc(codecs: String?): ByteArray {
		val level = codecs?.split(".")?.getOrNull(2)?.toIntOrNull()
			?.takeIf { it in 1..13 } ?: 6
		// Profile 8.1: dv_profile=8, same level, rpu_present=1, el_present=0, bl_present=1
		val word = (8 shl 9) or (level shl 3) or (1 shl 2) or (0 shl 1) or 1
		Timber.d("DV compat: synthesized Profile 8 DVCC level=$level (codecs=$codecs, word=0x${word.toString(16)})")
		return byteArrayOf(
			0x01.toByte(),                          // dv_version_major = 1
			0x00.toByte(),                          // dv_version_minor = 0
			((word shr 8) and 0xFF).toByte(),       // packed profile/level/flags high byte
			(word and 0xFF).toByte(),               // packed profile/level/flags low byte
			0x10.toByte(),                          // dv_bl_signal_compatibility_id=1 (BT.2020 PQ), reserved=0
			0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
			0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
			0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
			0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
			0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
		)
	}

	// ── ExoPlayer hooks ───────────────────────────────────────────────────────

	/**
	 * Intercept the input format before the codec is initialized.
	 * Swapping to Profile 8 here ensures initializationData (DVCC bytes) reach
	 * MediaCodec as Profile 8.1.
	 */
	@Throws(ExoPlaybackException::class)
	override fun onInputFormatChanged(formatHolder: FormatHolder): DecoderReuseEvaluation? {
		val fmt = formatHolder.format
		Timber.d(
			"DV compat: onInputFormatChanged " +
				"mime=${fmt?.sampleMimeType} codecs=${fmt?.codecs} " +
				"initDataCount=${fmt?.initializationData?.size} dvP7Hint=${dvP7Hint.get()}"
		)
		fmt?.initializationData?.forEachIndexed { i, bytes ->
			val preview = bytes.take(8).joinToString(" ") { "%02X".format(it) }
			Timber.d(
				"DV compat:   initData[$i] size=${bytes.size} " +
					"bytes=[$preview] dvccProfile=${getDvccProfile(bytes)}"
			)
		}

		if (isDvProfile7(formatHolder.format)) {
			Timber.d("DV compat: Profile 7 detected — rewriting as Profile 8.1 (force=$forceCompatMode)")
			formatHolder.format = patchToProfile8(formatHolder.format!!)

			val patched = formatHolder.format
			Timber.d(
				"DV compat: patched → mime=${patched?.sampleMimeType} codecs=${patched?.codecs} " +
					"initDataCount=${patched?.initializationData?.size}"
			)
		} else {
			Timber.d("DV compat: not Profile 7 — passing through unchanged (mime=${fmt?.sampleMimeType})")
		}
		return super.onInputFormatChanged(formatHolder)
	}

	/**
	 * Override decoder selection so that on Profile 7 content we query for
	 * a Profile 8 decoder (broadly available) instead of a Profile 7 decoder (rare).
	 */
	@Throws(MediaCodecUtil.DecoderQueryException::class)
	override fun getDecoderInfos(
		mediaCodecSelector: MediaCodecSelector,
		format: Format,
		requiresSecureDecoder: Boolean,
	): List<MediaCodecInfo> {
		Timber.d(
			"DV compat: getDecoderInfos mime=${format.sampleMimeType} codecs=${format.codecs} " +
				"isDvP7=${isDvProfile7(format)} dvP7Hint=${dvP7Hint.get()}"
		)
		if (isDvProfile7(format)) {
			val p8Format = patchToProfile8(format)
			val decoders = super.getDecoderInfos(mediaCodecSelector, p8Format, requiresSecureDecoder)
			if (decoders.isNotEmpty()) {
				Timber.d("DV compat: routing Profile 7 → Profile 8 decoder: ${decoders.first().name}")
				return decoders
			}
			Timber.d("DV compat: no Profile 8 DV decoder found — ExoPlayer fallback will handle it")
		}
		return super.getDecoderInfos(mediaCodecSelector, format, requiresSecureDecoder)
	}
}
