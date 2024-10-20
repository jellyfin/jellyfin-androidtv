package org.jellyfin.playback.media3.exoplayer.mapping

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
fun getFfmpegAudioMimeType(codec: String) = codec.lowercase().let { codec ->
	ffmpegAudioMimeTypes[codec]
		?: MimeTypes.getAudioMediaMimeType(codec)
		?: codec
}

val ffmpegAudioMimeTypes = mapOf(
	"aac" to MimeTypes.AUDIO_AAC,
	"ac3" to MimeTypes.AUDIO_AC3,
	"alac" to MimeTypes.AUDIO_ALAC,
	"amrnb" to MimeTypes.AUDIO_AMR_NB,
	"amrwb" to MimeTypes.AUDIO_AMR_WB,
	"dca" to MimeTypes.AUDIO_DTS,
	"eac3" to MimeTypes.AUDIO_E_AC3,
	"flac" to MimeTypes.AUDIO_FLAC,
	"mp1" to MimeTypes.AUDIO_MPEG_L1,
	"mp2" to MimeTypes.AUDIO_MPEG_L2,
	"mp3" to MimeTypes.AUDIO_MPEG,
	"opus" to MimeTypes.AUDIO_OPUS,
	"pcm_alaw" to MimeTypes.AUDIO_ALAW,
	"pcm_mulaw" to MimeTypes.AUDIO_MLAW,
	"truehd" to MimeTypes.AUDIO_TRUEHD,
	"vorbis" to MimeTypes.AUDIO_VORBIS,
// TODO: Find mime types for all these codecs...
//	"4gv" to MimeTypes.AUDIO_4GV,
//	"8svx_exp" to MimeTypes.AUDIO_8SVX_EXP,
//	"8svx_fib" to MimeTypes.AUDIO_8SVX_FIB,
//	"aac_latm" to MimeTypes.AUDIO_AAC_LATM,
//	"acelp" to MimeTypes.AUDIO_ACELP,
//	"adpcm_4xm" to MimeTypes.AUDIO_ADPCM_4_XM,
//	"adpcm_adx" to MimeTypes.AUDIO_ADPCM_ADX,
//	"adpcm_afc" to MimeTypes.AUDIO_ADPCM_AFC,
//	"adpcm_agm" to MimeTypes.AUDIO_ADPCM_AGM,
//	"adpcm_aica" to MimeTypes.AUDIO_ADPCM_AICA,
//	"adpcm_argo" to MimeTypes.AUDIO_ADPCM_ARGO,
//	"adpcm_ct" to MimeTypes.AUDIO_ADPCM_CT,
//	"adpcm_dtk" to MimeTypes.AUDIO_ADPCM_DTK,
//	"adpcm_ea" to MimeTypes.AUDIO_ADPCM_EA,
//	"adpcm_ea_maxis_xa" to MimeTypes.AUDIO_ADPCM_EA_MAXIS_XA,
//	"adpcm_ea_r1" to MimeTypes.AUDIO_ADPCM_EA_R_1,
//	"adpcm_ea_r2" to MimeTypes.AUDIO_ADPCM_EA_R_2,
//	"adpcm_ea_r3" to MimeTypes.AUDIO_ADPCM_EA_R_3,
//	"adpcm_ea_xas" to MimeTypes.AUDIO_ADPCM_EA_XAS,
//	"adpcm_g722" to MimeTypes.AUDIO_ADPCM_G_722,
//	"adpcm_g726" to MimeTypes.AUDIO_ADPCM_G_726,
//	"adpcm_g726le" to MimeTypes.AUDIO_ADPCM_G_726_LE,
//	"adpcm_ima_acorn" to MimeTypes.AUDIO_ADPCM_IMA_ACORN,
//	"adpcm_ima_alp" to MimeTypes.AUDIO_ADPCM_IMA_ALP,
//	"adpcm_ima_amv" to MimeTypes.AUDIO_ADPCM_IMA_AMV,
//	"adpcm_ima_apc" to MimeTypes.AUDIO_ADPCM_IMA_APC,
//	"adpcm_ima_apm" to MimeTypes.AUDIO_ADPCM_IMA_APM,
//	"adpcm_ima_cunning" to MimeTypes.AUDIO_ADPCM_IMA_CUNNING,
//	"adpcm_ima_dat4" to MimeTypes.AUDIO_ADPCM_IMA_DAT_4,
//	"adpcm_ima_dk3" to MimeTypes.AUDIO_ADPCM_IMA_DK_3,
//	"adpcm_ima_dk4" to MimeTypes.AUDIO_ADPCM_IMA_DK_4,
//	"adpcm_ima_ea_eacs" to MimeTypes.AUDIO_ADPCM_IMA_EA_EACS,
//	"adpcm_ima_ea_sead" to MimeTypes.AUDIO_ADPCM_IMA_EA_SEAD,
//	"adpcm_ima_iss" to MimeTypes.AUDIO_ADPCM_IMA_ISS,
//	"adpcm_ima_moflex" to MimeTypes.AUDIO_ADPCM_IMA_MOFLEX,
//	"adpcm_ima_mtf" to MimeTypes.AUDIO_ADPCM_IMA_MTF,
//	"adpcm_ima_oki" to MimeTypes.AUDIO_ADPCM_IMA_OKI,
//	"adpcm_ima_qt" to MimeTypes.AUDIO_ADPCM_IMA_QT,
//	"adpcm_ima_rad" to MimeTypes.AUDIO_ADPCM_IMA_RAD,
//	"adpcm_ima_smjpeg" to MimeTypes.AUDIO_ADPCM_IMA_SMJPEG,
//	"adpcm_ima_ssi" to MimeTypes.AUDIO_ADPCM_IMA_SSI,
//	"adpcm_ima_wav" to MimeTypes.AUDIO_ADPCM_IMA_WAV,
//	"adpcm_ima_ws" to MimeTypes.AUDIO_ADPCM_IMA_WS,
//	"adpcm_ms" to MimeTypes.AUDIO_ADPCM_MS,
//	"adpcm_mtaf" to MimeTypes.AUDIO_ADPCM_MTAF,
//	"adpcm_psx" to MimeTypes.AUDIO_ADPCM_PSX,
//	"adpcm_sbpro_2" to MimeTypes.AUDIO_ADPCM_SBPRO_2,
//	"adpcm_sbpro_3" to MimeTypes.AUDIO_ADPCM_SBPRO_3,
//	"adpcm_sbpro_4" to MimeTypes.AUDIO_ADPCM_SBPRO_4,
//	"adpcm_swf" to MimeTypes.AUDIO_ADPCM_SWF,
//	"adpcm_thp" to MimeTypes.AUDIO_ADPCM_THP,
//	"adpcm_thp_le" to MimeTypes.AUDIO_ADPCM_THP_LE,
//	"adpcm_vima" to MimeTypes.AUDIO_ADPCM_VIMA,
//	"adpcm_xa" to MimeTypes.AUDIO_ADPCM_XA,
//	"adpcm_yamaha" to MimeTypes.AUDIO_ADPCM_YAMAHA,
//	"adpcm_zork" to MimeTypes.AUDIO_ADPCM_ZORK,
//	"ape" to MimeTypes.AUDIO_APE,
//	"aptx" to MimeTypes.AUDIO_APTX,
//	"aptx_hd" to MimeTypes.AUDIO_APTX_HD,
//	"atrac1" to MimeTypes.AUDIO_ATRAC_1,
//	"atrac3" to MimeTypes.AUDIO_ATRAC_3,
//	"atrac3al" to MimeTypes.AUDIO_ATRAC_3_AL,
//	"atrac3p" to MimeTypes.AUDIO_ATRAC_3_P,
//	"atrac3pal" to MimeTypes.AUDIO_ATRAC_3_PAL,
//	"atrac9" to MimeTypes.AUDIO_ATRAC_9,
//	"avc" to MimeTypes.AUDIO_AVC,
//	"binkaudio_dct" to MimeTypes.AUDIO_BINKAUDIO_DCT,
//	"binkaudio_rdft" to MimeTypes.AUDIO_BINKAUDIO_RDFT,
//	"bmv_audio" to MimeTypes.AUDIO_BMV_AUDIO,
//	"celt" to MimeTypes.AUDIO_CELT,
//	"codec2" to MimeTypes.AUDIO_CODEC_2,
//	"comfortnoise" to MimeTypes.AUDIO_COMFORTNOISE,
//	"cook" to MimeTypes.AUDIO_COOK,
//	"derf_dpcm" to MimeTypes.AUDIO_DERF_DPCM,
//	"dfpwm" to MimeTypes.AUDIO_DFPWM,
//	"dolby_e" to MimeTypes.AUDIO_DOLBY_E,
//	"dsd_lsbf" to MimeTypes.AUDIO_DSD_LSBF,
//	"dsd_lsbf_planar" to MimeTypes.AUDIO_DSD_LSBF_PLANAR,
//	"dsd_msbf" to MimeTypes.AUDIO_DSD_MSBF,
//	"dsd_msbf_planar" to MimeTypes.AUDIO_DSD_MSBF_PLANAR,
//	"dsicinaudio" to MimeTypes.AUDIO_DSICINAUDIO,
//	"dss_sp" to MimeTypes.AUDIO_DSS_SP,
//	"dst" to MimeTypes.AUDIO_DST,
//	"dvaudio" to MimeTypes.AUDIO_DVAUDIO,
//	"evrc" to MimeTypes.AUDIO_EVRC,
//	"fastaudio" to MimeTypes.AUDIO_FASTAUDIO,
//	"g723_1" to MimeTypes.AUDIO_G_723_1,
//	"g729" to MimeTypes.AUDIO_G_729,
//	"gremlin_dpcm" to MimeTypes.AUDIO_GREMLIN_DPCM,
//	"gsm" to MimeTypes.AUDIO_GSM,
//	"gsm_ms" to MimeTypes.AUDIO_GSM_MS,
//	"hca" to MimeTypes.AUDIO_HCA,
//	"hcom" to MimeTypes.AUDIO_HCOM,
//	"iac" to MimeTypes.AUDIO_IAC,
//	"ilbc" to MimeTypes.AUDIO_ILBC,
//	"imc" to MimeTypes.AUDIO_IMC,
//	"interplay_dpcm" to MimeTypes.AUDIO_INTERPLAY_DPCM,
//	"interplayacm" to MimeTypes.AUDIO_INTERPLAYACM,
//	"mace3" to MimeTypes.AUDIO_MACE_3,
//	"mace6" to MimeTypes.AUDIO_MACE_6,
//	"metasound" to MimeTypes.AUDIO_METASOUND,
//	"mlp" to MimeTypes.AUDIO_MLP,
//	"mp3adu" to MimeTypes.AUDIO_MP_3_ADU,
//	"mp3on4" to MimeTypes.AUDIO_MP_3_ON_4,
//	"mp4als" to MimeTypes.AUDIO_MP_4_ALS,
//	"mpegh_3d_audio" to MimeTypes.AUDIO_MPEGH_3_D_AUDIO,
//	"msnsiren" to MimeTypes.AUDIO_MSNSIREN,
//	"musepack7" to MimeTypes.AUDIO_MUSEPACK_7,
//	"musepack8" to MimeTypes.AUDIO_MUSEPACK_8,
//	"nellymoser" to MimeTypes.AUDIO_NELLYMOSER,
//	"paf_audio" to MimeTypes.AUDIO_PAF_AUDIO,
//	"pcm_bluray" to MimeTypes.AUDIO_PCM_BLURAY,
//	"pcm_dvd" to MimeTypes.AUDIO_PCM_DVD,
//	"pcm_f16le" to MimeTypes.AUDIO_PCM_F_16_LE,
//	"pcm_f24le" to MimeTypes.AUDIO_PCM_F_24_LE,
//	"pcm_f32be" to MimeTypes.AUDIO_PCM_F_32_BE,
//	"pcm_f32le" to MimeTypes.AUDIO_PCM_F_32_LE,
//	"pcm_f64be" to MimeTypes.AUDIO_PCM_F_64_BE,
//	"pcm_f64le" to MimeTypes.AUDIO_PCM_F_64_LE,
//	"pcm_lxf" to MimeTypes.AUDIO_PCM_LXF,
//	"pcm_s8" to MimeTypes.AUDIO_PCM_S_8,
//	"pcm_s8_planar" to MimeTypes.AUDIO_PCM_S_8_PLANAR,
//	"pcm_s16be" to MimeTypes.AUDIO_PCM_S_16_BE,
//	"pcm_s16be_planar" to MimeTypes.AUDIO_PCM_S_16_BE_PLANAR,
//	"pcm_s16le" to MimeTypes.AUDIO_PCM_S_16_LE,
//	"pcm_s16le_planar" to MimeTypes.AUDIO_PCM_S_16_LE_PLANAR,
//	"pcm_s24be" to MimeTypes.AUDIO_PCM_S_24_BE,
//	"pcm_s24daud" to MimeTypes.AUDIO_PCM_S_24_DAUD,
//	"pcm_s24le" to MimeTypes.AUDIO_PCM_S_24_LE,
//	"pcm_s24le_planar" to MimeTypes.AUDIO_PCM_S_24_LE_PLANAR,
//	"pcm_s32be" to MimeTypes.AUDIO_PCM_S_32_BE,
//	"pcm_s32le" to MimeTypes.AUDIO_PCM_S_32_LE,
//	"pcm_s32le_planar" to MimeTypes.AUDIO_PCM_S_32_LE_PLANAR,
//	"pcm_s64be" to MimeTypes.AUDIO_PCM_S_64_BE,
//	"pcm_s64le" to MimeTypes.AUDIO_PCM_S_64_LE,
//	"pcm_sga" to MimeTypes.AUDIO_PCM_SGA,
//	"pcm_u8" to MimeTypes.AUDIO_PCM_U_8,
//	"pcm_u16be" to MimeTypes.AUDIO_PCM_U_16_BE,
//	"pcm_u16le" to MimeTypes.AUDIO_PCM_U_16_LE,
//	"pcm_u24be" to MimeTypes.AUDIO_PCM_U_24_BE,
//	"pcm_u24le" to MimeTypes.AUDIO_PCM_U_24_LE,
//	"pcm_u32be" to MimeTypes.AUDIO_PCM_U_32_BE,
//	"pcm_u32le" to MimeTypes.AUDIO_PCM_U_32_LE,
//	"pcm_vidc" to MimeTypes.AUDIO_PCM_VIDC,
//	"qcelp" to MimeTypes.AUDIO_QCELP,
//	"qdm2" to MimeTypes.AUDIO_QDM_2,
//	"qdmc" to MimeTypes.AUDIO_QDMC,
//	"ra_144" to MimeTypes.AUDIO_RA_144,
//	"ra_288" to MimeTypes.AUDIO_RA_288,
//	"ralf" to MimeTypes.AUDIO_RALF,
//	"roq_dpcm" to MimeTypes.AUDIO_ROQ_DPCM,
//	"s302m" to MimeTypes.AUDIO_S_302_M,
//	"sbc" to MimeTypes.AUDIO_SBC,
//	"sdx2_dpcm" to MimeTypes.AUDIO_SDX_2_DPCM,
//	"shorten" to MimeTypes.AUDIO_SHORTEN,
//	"sipr" to MimeTypes.AUDIO_SIPR,
//	"siren" to MimeTypes.AUDIO_SIREN,
//	"smackaudio" to MimeTypes.AUDIO_SMACKAUDIO,
//	"smv" to MimeTypes.AUDIO_SMV,
//	"sol_dpcm" to MimeTypes.AUDIO_SOL_DPCM,
//	"sonic" to MimeTypes.AUDIO_SONIC,
//	"sonicls" to MimeTypes.AUDIO_SONICLS,
//	"speex" to MimeTypes.AUDIO_SPEEX,
//	"tak" to MimeTypes.AUDIO_TAK,
//	"truespeech" to MimeTypes.AUDIO_TRUESPEECH,
//	"tta" to MimeTypes.AUDIO_TTA,
//	"twinvq" to MimeTypes.AUDIO_TWINVQ,
//	"vmdaudio" to MimeTypes.AUDIO_VMDAUDIO,
//	"wavesynth" to MimeTypes.AUDIO_WAVESYNTH,
//	"wavpack" to MimeTypes.AUDIO_WAVPACK,
//	"westwood_snd1" to MimeTypes.AUDIO_WESTWOOD_SND_1,
//	"wmalossless" to MimeTypes.AUDIO_WMALOSSLESS,
//	"wmapro" to MimeTypes.AUDIO_WMAPRO,
//	"wmav1" to MimeTypes.AUDIO_WMAV_1,
//	"wmav2" to MimeTypes.AUDIO_WMAV_2,
//	"wmavoice" to MimeTypes.AUDIO_WMAVOICE,
//	"xan_dpcm" to MimeTypes.AUDIO_XAN_DPCM,
//	"xma1" to MimeTypes.AUDIO_XMA_1,
//	"xma2" to MimeTypes.AUDIO_XMA_2,
)
