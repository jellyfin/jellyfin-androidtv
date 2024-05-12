@file:JvmName("ModelCompat")

package org.jellyfin.androidtv.util.sdk.compat

import org.jellyfin.androidtv.util.sdk.toNameGuidPair
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ExtraType
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PersonKind
import org.jellyfin.sdk.model.api.VideoRange
import org.jellyfin.sdk.model.api.VideoRangeType
import org.jellyfin.sdk.model.extensions.toNameGuidPair
import org.jellyfin.sdk.model.serializer.toUUID
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod as LegacySubtitleDeliveryMethod
import org.jellyfin.apiclient.model.drawing.ImageOrientation as LegacyImageOrientation
import org.jellyfin.apiclient.model.dto.BaseItemDto as LegacyBaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemPerson as LegacyBaseItemPerson
import org.jellyfin.apiclient.model.dto.ChapterInfoDto as LegacyChapterInfoDto
import org.jellyfin.apiclient.model.dto.GenreDto as LegacyGenreDto
import org.jellyfin.apiclient.model.dto.MediaSourceInfo as LegacyMediaSourceInfo
import org.jellyfin.apiclient.model.dto.MediaSourceType as LegacyMediaSourceType
import org.jellyfin.apiclient.model.dto.NameIdPair as LegacyNameIdPair
import org.jellyfin.apiclient.model.dto.StudioDto as LegacyStudioDto
import org.jellyfin.apiclient.model.dto.UserItemDataDto as LegacyUserItemDataDto
import org.jellyfin.apiclient.model.entities.ExtraType as LegacyExtraType
import org.jellyfin.apiclient.model.entities.ImageType as LegacyImageType
import org.jellyfin.apiclient.model.entities.IsoType as LegacyIsoType
import org.jellyfin.apiclient.model.entities.LocationType as LegacyLocationType
import org.jellyfin.apiclient.model.entities.MediaStream as LegacyMediaStream
import org.jellyfin.apiclient.model.entities.MediaStreamType as LegacyMediaStreamType
import org.jellyfin.apiclient.model.entities.MediaUrl as LegacyMediaUrl
import org.jellyfin.apiclient.model.entities.MetadataFields as LegacyMetadataFields
import org.jellyfin.apiclient.model.entities.SortOrder as LegacySortOrder
import org.jellyfin.apiclient.model.entities.Video3DFormat as LegacyVideo3DFormat
import org.jellyfin.apiclient.model.entities.VideoType as LegacyVideoType
import org.jellyfin.apiclient.model.library.PlayAccess as LegacyPlayAccess
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto as LegacyChannelInfoDto
import org.jellyfin.apiclient.model.livetv.ChannelType as LegacyChannelType
import org.jellyfin.apiclient.model.livetv.DayPattern as LegacyDayPattern
import org.jellyfin.apiclient.model.livetv.KeepUntil as LegacyKeepUntil
import org.jellyfin.apiclient.model.livetv.ProgramAudio as LegacyProgramAudio
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto as LegacySeriesTimerInfoDto
import org.jellyfin.apiclient.model.mediainfo.MediaProtocol as LegacyMediaProtocol
import org.jellyfin.apiclient.model.mediainfo.TransportStreamTimestamp as LegacyTransportStreamTimestamp
import org.jellyfin.apiclient.model.providers.ExternalUrl as LegacyExternalUrl
import org.jellyfin.sdk.model.api.BaseItemDto as ModernBaseItemDto
import org.jellyfin.sdk.model.api.BaseItemPerson as ModernBaseItemPerson
import org.jellyfin.sdk.model.api.ChannelType as ModernChannelType
import org.jellyfin.sdk.model.api.ChapterInfo as ModernChapterInfo
import org.jellyfin.sdk.model.api.DayOfWeek as ModernDayOfWeek
import org.jellyfin.sdk.model.api.DayPattern as ModernDayPattern
import org.jellyfin.sdk.model.api.ExternalUrl as ModernExternalUrl
import org.jellyfin.sdk.model.api.ImageOrientation as ModernImageOrientation
import org.jellyfin.sdk.model.api.ImageType as ModernImageType
import org.jellyfin.sdk.model.api.IsoType as ModernIsoType
import org.jellyfin.sdk.model.api.KeepUntil as ModernKeepUntil
import org.jellyfin.sdk.model.api.LocationType as ModernLocationType
import org.jellyfin.sdk.model.api.MediaProtocol as ModernMediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo as ModernMediaSourceInfo
import org.jellyfin.sdk.model.api.MediaSourceType as ModernMediaSourceType
import org.jellyfin.sdk.model.api.MediaStream as ModernMediaStream
import org.jellyfin.sdk.model.api.MediaStreamType as ModernMediaStreamType
import org.jellyfin.sdk.model.api.MediaUrl as ModernMediaUrl
import org.jellyfin.sdk.model.api.MetadataField as ModernMetadataField
import org.jellyfin.sdk.model.api.NameGuidPair as ModernNameGuidPair
import org.jellyfin.sdk.model.api.NameIdPair as ModernNameIdPair
import org.jellyfin.sdk.model.api.PlayAccess as ModernPlayAccess
import org.jellyfin.sdk.model.api.ProgramAudio as ModernProgramAudio
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto as ModernSeriesTimerInfoDto
import org.jellyfin.sdk.model.api.SortOrder as ModernSortOrder
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod as ModernSubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.TransportStreamTimestamp as ModernTransportStreamTimestamp
import org.jellyfin.sdk.model.api.UserItemDataDto as ModernUserItemDataDto
import org.jellyfin.sdk.model.api.Video3dFormat as ModernVideo3dFormat
import org.jellyfin.sdk.model.api.VideoType as ModernVideoType

fun LegacyBaseItemDto.asSdk(): ModernBaseItemDto = ModernBaseItemDto(
	name = this.name,
	originalTitle = this.originalTitle,
	serverId = this.serverId,
	id = this.id.toUUID(),
	etag = this.etag,
	sourceType = this.sourceType,
	playlistItemId = this.playlistItemId,
	dateCreated = this.dateCreated?.toLocalDateTime(),
	dateLastMediaAdded = this.dateLastMediaAdded?.toLocalDateTime(),
	extraType = this.extraType?.asSdk(),
	airsBeforeSeasonNumber = this.airsBeforeSeasonNumber,
	airsAfterSeasonNumber = this.airsAfterSeasonNumber,
	airsBeforeEpisodeNumber = this.airsBeforeEpisodeNumber,
	canDelete = this.canDelete,
	canDownload = this.canDownload,
	hasSubtitles = this.hasSubtitles,
	preferredMetadataLanguage = this.preferredMetadataLanguage,
	preferredMetadataCountryCode = this.preferredMetadataCountryCode,
	container = this.container,
	sortName = this.sortName,
	forcedSortName = this.forcedSortName,
	video3dFormat = this.video3DFormat?.asSdk(),
	premiereDate = this.premiereDate?.toLocalDateTime(),
	externalUrls = this.externalUrls?.map { it.asSdk() },
	mediaSources = this.mediaSources?.map { it.asSdk() },
	criticRating = this.criticRating,
	productionLocations = this.productionLocations?.toList(),
	path = this.path,
	enableMediaSourceDisplay = false, // this.enableMediaSourceDisplay
	officialRating = this.officialRating,
	customRating = this.customRating,
	channelId = this.channelId?.toUUID(),
	channelName = this.channelName,
	overview = this.overview,
	taglines = this.taglines,
	genres = this.genres,
	communityRating = this.communityRating,
	cumulativeRunTimeTicks = this.cumulativeRunTimeTicks,
	runTimeTicks = this.runTimeTicks,
	playAccess = this.playAccess?.asSdk(),
	aspectRatio = this.aspectRatio,
	productionYear = this.productionYear,
	isPlaceHolder = this.isPlaceHolder,
	number = this.number,
	channelNumber = this.channelNumber,
	indexNumber = this.indexNumber,
	indexNumberEnd = this.indexNumberEnd,
	parentIndexNumber = this.parentIndexNumber,
	remoteTrailers = this.remoteTrailers?.map { it.asSdk() },
	providerIds = this.providerIds,
	isHd = this.isHD,
	isFolder = this.isFolder,
	parentId = this.parentId?.toUUID(),
	type = BaseItemKind.from(this.type)!!,
	people = this.people?.map { it.asSdk() },
	studios = this.studios?.map { it.asSdk() },
	genreItems = this.genreItems?.map { it.asSdk() },
	parentLogoItemId = this.parentLogoItemId?.toUUIDOrNull(),
	parentBackdropItemId = this.parentBackdropItemId?.toUUIDOrNull(),
	parentBackdropImageTags = this.parentBackdropImageTags,
	localTrailerCount = this.localTrailerCount,
	userData = this.userData?.asSdk(),
	recursiveItemCount = this.recursiveItemCount,
	childCount = this.childCount,
	seriesName = this.seriesName,
	seriesId = this.seriesId?.toUUID(),
	seasonId = this.seasonId?.toUUID(),
	specialFeatureCount = this.specialFeatureCount,
	displayPreferencesId = this.displayPreferencesId,
	status = this.status,
	airTime = this.airTime,
	airDays = this.airDays?.mapNotNull { ModernDayOfWeek.from(it) },
	tags = this.tags,
	primaryImageAspectRatio = this.primaryImageAspectRatio,
	artists = this.artists,
	artistItems = this.artistItems?.map { it.asSdk().toNameGuidPair() },
	album = this.album,
	collectionType = this.collectionType?.let { CollectionType.fromNameOrNull(this.collectionType.lowercase()) }
		?: CollectionType.UNKNOWN,
	displayOrder = this.displayOrder,
	albumId = this.albumId?.toUUID(),
	albumPrimaryImageTag = this.albumPrimaryImageTag,
	seriesPrimaryImageTag = this.seriesPrimaryImageTag,
	albumArtist = this.albumArtist,
	albumArtists = this.albumArtists?.map { it.asSdk().toNameGuidPair() },
	seasonName = this.seasonName,
	mediaStreams = this.mediaStreams?.map { it.asSdk() },
	videoType = this.videoType?.asSdk(),
	partCount = this.partCount,
	mediaSourceCount = this.mediaSourceCount,
	imageTags = this.imageTags?.asSdk(),
	backdropImageTags = this.backdropImageTags,
	screenshotImageTags = this.screenshotImageTags,
	parentLogoImageTag = this.parentLogoImageTag,
	parentArtItemId = this.parentArtItemId?.toUUIDOrNull(),
	parentArtImageTag = this.parentArtImageTag,
	seriesThumbImageTag = this.seriesThumbImageTag,
	imageBlurHashes = this.imageBlurHashes?.asSdk(),
	seriesStudio = this.seriesStudio,
	parentThumbItemId = this.parentThumbItemId?.toUUIDOrNull(),
	parentThumbImageTag = this.parentThumbImageTag,
	parentPrimaryImageItemId = this.parentPrimaryImageItemId,
	parentPrimaryImageTag = this.parentPrimaryImageTag,
	chapters = this.chapters?.map { it.asSdk() },
	locationType = this.locationType?.asSdk(),
	isoType = this.isoType?.asSdk(),
	mediaType = MediaType.fromName(this.mediaType.lowercase().replaceFirstChar { it.uppercase() }),
	endDate = this.endDate?.toLocalDateTime(),
	lockedFields = this.lockedFields?.map { it.asSdk() },
	trailerCount = this.trailerCount,
	movieCount = this.movieCount,
	seriesCount = this.seriesCount,
	programCount = this.programCount,
	episodeCount = this.episodeCount,
	songCount = this.songCount,
	albumCount = this.albumCount,
	artistCount = this.artistCount,
	musicVideoCount = this.musicVideoCount,
	lockData = this.lockData,
	width = this.width,
	height = this.height,
	cameraMake = this.cameraMake,
	cameraModel = this.cameraModel,
	software = this.software,
	exposureTime = this.exposureTime,
	focalLength = this.focalLength,
	imageOrientation = this.imageOrientation?.asSdk(),
	aperture = this.aperture,
	shutterSpeed = this.shutterSpeed,
	latitude = this.latitude,
	longitude = this.longitude,
	altitude = this.altitude,
	isoSpeedRating = this.isoSpeedRating,
	seriesTimerId = this.seriesTimerId,
	programId = this.programId,
	channelPrimaryImageTag = this.channelPrimaryImageTag,
	startDate = this.startDate?.toLocalDateTime(),
	completionPercentage = this.completionPercentage,
	isRepeat = this.isRepeat,
	episodeTitle = this.episodeTitle,
	channelType = this.channelType?.asSdk(),
	audio = this.audio?.asSdk(),
	isMovie = this.isMovie,
	isSports = this.isSports,
	isSeries = this.isSeries,
	isLive = this.isLive,
	isNews = this.isNews,
	isKids = this.isKids,
	isPremiere = this.isPremiere,
	timerId = this.timerId,
	currentProgram = this.currentProgram?.asSdk(),
)

private fun BaseItemKind.Companion.from(str: String) = when (str.uppercase()) {
	"AGGREGATE_FOLDER", "AGGREGATEFOLDER" -> BaseItemKind.AGGREGATE_FOLDER
	"AUDIO" -> BaseItemKind.AUDIO
	"AUDIO_BOOK", "AUDIOBOOK" -> BaseItemKind.AUDIO_BOOK
	"BASE_PLUGIN_FOLDER", "BASEPLUGINFOLDER" -> BaseItemKind.BASE_PLUGIN_FOLDER
	"BOOK" -> BaseItemKind.BOOK
	"BOX_SET", "BOXSET" -> BaseItemKind.BOX_SET
	"CHANNEL" -> BaseItemKind.CHANNEL
	"CHANNEL_FOLDER_ITEM", "CHANNELFOLDERITEM" -> BaseItemKind.CHANNEL_FOLDER_ITEM
	"COLLECTION_FOLDER", "COLLECTIONFOLDER" -> BaseItemKind.COLLECTION_FOLDER
	"EPISODE" -> BaseItemKind.EPISODE
	"FOLDER" -> BaseItemKind.FOLDER
	"GENRE" -> BaseItemKind.GENRE
	"MANUAL_PLAYLISTS_FOLDER", "MANUALPLAYLISTSFOLDER" -> BaseItemKind.MANUAL_PLAYLISTS_FOLDER
	"MOVIE" -> BaseItemKind.MOVIE
	"LIVE_TV_CHANNEL", "LIVETVCHANNEL" -> BaseItemKind.LIVE_TV_CHANNEL
	"LIVE_TV_PROGRAM", "LIVETVPROGRAM" -> BaseItemKind.LIVE_TV_PROGRAM
	"MUSIC_ALBUM", "MUSICALBUM" -> BaseItemKind.MUSIC_ALBUM
	"MUSIC_ARTIST", "MUSICARTIST" -> BaseItemKind.MUSIC_ARTIST
	"MUSIC_GENRE", "MUSICGENRE" -> BaseItemKind.MUSIC_GENRE
	"MUSIC_VIDEO", "MUSICVIDEO" -> BaseItemKind.MUSIC_VIDEO
	"PERSON" -> BaseItemKind.PERSON
	"PHOTO" -> BaseItemKind.PHOTO
	"PHOTO_ALBUM", "PHOTOALBUM" -> BaseItemKind.PHOTO_ALBUM
	"PLAYLIST" -> BaseItemKind.PLAYLIST
	"PLAYLISTS_FOLDER", "PLAYLISTSFOLDER" -> BaseItemKind.PLAYLISTS_FOLDER
	"PROGRAM" -> BaseItemKind.PROGRAM
	"RECORDING" -> BaseItemKind.RECORDING
	"SEASON" -> BaseItemKind.SEASON
	"SERIES" -> BaseItemKind.SERIES
	"STUDIO" -> BaseItemKind.STUDIO
	"TRAILER" -> BaseItemKind.TRAILER
	"TV_CHANNEL", "TVCHANNEL" -> BaseItemKind.TV_CHANNEL
	"TV_PROGRAM", "TVPROGRAM" -> BaseItemKind.TV_PROGRAM
	"USER_ROOT_FOLDER", "USERROOTFOLDER" -> BaseItemKind.USER_ROOT_FOLDER
	"USER_VIEW", "USERVIEW" -> BaseItemKind.USER_VIEW
	"VIDEO" -> BaseItemKind.VIDEO
	"YEAR" -> BaseItemKind.YEAR
	else -> null
}

private fun ModernDayOfWeek.Companion.from(str: String) = when (str.uppercase()) {
	"SUNDAY" -> ModernDayOfWeek.SUNDAY
	"MONDAY" -> ModernDayOfWeek.MONDAY
	"TUESDAY" -> ModernDayOfWeek.TUESDAY
	"WEDNESDAY" -> ModernDayOfWeek.WEDNESDAY
	"THURSDAY" -> ModernDayOfWeek.THURSDAY
	"FRIDAY" -> ModernDayOfWeek.FRIDAY
	"SATURDAY" -> ModernDayOfWeek.SATURDAY
	else -> null
}

private fun Date.toLocalDateTime(): LocalDateTime = toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

private fun LegacyExtraType.asSdk(): ExtraType = when (this) {
	LegacyExtraType.Clip -> ExtraType.CLIP
	LegacyExtraType.Trailer -> ExtraType.TRAILER
	LegacyExtraType.BehindTheScenes -> ExtraType.BEHIND_THE_SCENES
	LegacyExtraType.DeletedScene -> ExtraType.DELETED_SCENE
	LegacyExtraType.Interview -> ExtraType.INTERVIEW
	LegacyExtraType.Scene -> ExtraType.SCENE
	LegacyExtraType.Sample -> ExtraType.SAMPLE
	LegacyExtraType.ThemeSong -> ExtraType.THEME_SONG
	LegacyExtraType.ThemeVideo -> ExtraType.THEME_VIDEO
}

fun LegacyVideo3DFormat.asSdk(): ModernVideo3dFormat = when (this) {
	LegacyVideo3DFormat.HalfSideBySide -> ModernVideo3dFormat.HALF_SIDE_BY_SIDE
	LegacyVideo3DFormat.FullSideBySide -> ModernVideo3dFormat.FULL_SIDE_BY_SIDE
	LegacyVideo3DFormat.FullTopAndBottom -> ModernVideo3dFormat.FULL_TOP_AND_BOTTOM
	LegacyVideo3DFormat.HalfTopAndBottom -> ModernVideo3dFormat.HALF_TOP_AND_BOTTOM
	LegacyVideo3DFormat.MVC -> ModernVideo3dFormat.MVC
}

fun LegacyExternalUrl.asSdk(): ModernExternalUrl = ModernExternalUrl(
	name = this.name,
	url = this.url,
)

fun LegacyMediaSourceInfo.asSdk(): ModernMediaSourceInfo = ModernMediaSourceInfo(
	protocol = this.protocol.asSdk(),
	id = this.id,
	path = this.path,
	encoderPath = null, // this.encoderPath
	encoderProtocol = null, // this.encoderProtocol
	type = this.type.asSdk(),
	container = this.container,
	size = this.size,
	name = this.name,
	isRemote = this.isRemote,
	eTag = this.eTag,
	runTimeTicks = this.runTimeTicks,
	readAtNativeFramerate = this.readAtNativeFramerate,
	ignoreDts = false, // this.ignoreDts
	ignoreIndex = false, // this.ignoreIndex
	genPtsInput = false, // this.genPtsInput
	supportsTranscoding = this.supportsTranscoding,
	supportsDirectStream = this.supportsDirectStream,
	supportsDirectPlay = this.supportsDirectPlay,
	isInfiniteStream = this.isInfiniteStream,
	requiresOpening = this.requiresOpening,
	openToken = this.openToken,
	requiresClosing = this.requiresClosing,
	liveStreamId = this.liveStreamId,
	bufferMs = this.bufferMs,
	requiresLooping = false, // this.requiresLooping
	supportsProbing = false, // this.supportsProbing
	videoType = this.videoType?.asSdk(),
	isoType = this.isoType?.asSdk(),
	video3dFormat = this.video3DFormat?.asSdk(),
	mediaStreams = this.mediaStreams?.map { it.asSdk() },
	mediaAttachments = null, // this.mediaAttachments
	formats = this.formats,
	bitrate = this.bitrate,
	timestamp = this.timestamp?.asSdk(),
	requiredHttpHeaders = this.requiredHttpHeaders,
	transcodingUrl = this.transcodingUrl,
	transcodingSubProtocol = MediaStreamProtocol.fromName(this.transcodingSubProtocol.lowercase()),
	transcodingContainer = this.transcodingContainer,
	analyzeDurationMs = null, // this.analyzeDurationMs
	defaultAudioStreamIndex = this.defaultAudioStreamIndex,
	defaultSubtitleStreamIndex = this.defaultSubtitleStreamIndex,
)

fun LegacyMediaProtocol.asSdk(): ModernMediaProtocol = when (this) {
	LegacyMediaProtocol.File -> ModernMediaProtocol.FILE
	LegacyMediaProtocol.Http -> ModernMediaProtocol.HTTP
	LegacyMediaProtocol.Rtmp -> ModernMediaProtocol.RTMP
	LegacyMediaProtocol.Rtsp -> ModernMediaProtocol.RTSP
	LegacyMediaProtocol.Udp -> ModernMediaProtocol.UDP
	LegacyMediaProtocol.Rtp -> ModernMediaProtocol.RTP
}

fun LegacyMediaSourceType.asSdk(): ModernMediaSourceType = when (this) {
	LegacyMediaSourceType.Default -> ModernMediaSourceType.DEFAULT
	LegacyMediaSourceType.Grouping -> ModernMediaSourceType.GROUPING
	LegacyMediaSourceType.Placeholder -> ModernMediaSourceType.PLACEHOLDER
}

fun LegacyTransportStreamTimestamp.asSdk(): ModernTransportStreamTimestamp = when (this) {
	LegacyTransportStreamTimestamp.None -> ModernTransportStreamTimestamp.NONE
	LegacyTransportStreamTimestamp.Zero -> ModernTransportStreamTimestamp.ZERO
	LegacyTransportStreamTimestamp.Valid -> ModernTransportStreamTimestamp.VALID
}

fun LegacyPlayAccess.asSdk(): ModernPlayAccess = when (this) {
	LegacyPlayAccess.Full -> ModernPlayAccess.FULL
	LegacyPlayAccess.None -> ModernPlayAccess.NONE
}

fun LegacyMediaUrl.asSdk(): ModernMediaUrl = ModernMediaUrl(
	url = this.url,
	name = this.name,
)

fun LegacyBaseItemPerson.asSdk(): ModernBaseItemPerson = ModernBaseItemPerson(
	name = this.name,
	id = this.id.toUUID(),
	role = this.role,
	type = PersonKind.fromName(this.type),
	primaryImageTag = this.primaryImageTag,
	imageBlurHashes = null, // this.imageBlurHashes
)

fun LegacyStudioDto.asSdk(): ModernNameGuidPair = (this.id.toUUID() to this.name).toNameGuidPair()
fun LegacyGenreDto.asSdk(): ModernNameGuidPair = (this.id.toUUID() to this.name).toNameGuidPair()

fun LegacyUserItemDataDto.asSdk(): ModernUserItemDataDto = ModernUserItemDataDto(
	rating = this.rating,
	playedPercentage = this.playedPercentage,
	unplayedItemCount = this.unplayedItemCount,
	playbackPositionTicks = this.playbackPositionTicks,
	playCount = this.playCount,
	isFavorite = this.isFavorite,
	likes = this.likes,
	lastPlayedDate = this.lastPlayedDate?.toLocalDateTime(),
	played = this.played,
	key = this.key,
	itemId = this.itemId,
)

fun LegacyNameIdPair.asSdk(): ModernNameIdPair = ModernNameIdPair(
	name = this.name,
	id = this.id
)

fun LegacyMediaStream.asSdk(): ModernMediaStream = ModernMediaStream(
	codec = this.codec,
	codecTag = this.codecTag,
	language = this.language,
	colorRange = null, // this.colorRange
	colorSpace = null, // this.colorSpace
	colorTransfer = null, // this.colorTransfer
	colorPrimaries = null, // this.colorPrimaries
	comment = this.comment,
	timeBase = this.timeBase,
	codecTimeBase = this.codecTimeBase,
	title = this.title,
	videoRange = VideoRange.UNKNOWN, // this.videoRange
	localizedUndefined = null, // this.localizedUndefined
	localizedDefault = null, // this.localizedDefault
	localizedForced = null, // this.localizedForced
	displayTitle = this.displayTitle,
	nalLengthSize = this.nalLengthSize,
	isInterlaced = this.isInterlaced,
	isAvc = this.isAVC,
	channelLayout = this.channelLayout,
	bitRate = this.bitRate,
	bitDepth = this.bitDepth,
	refFrames = this.refFrames,
	packetLength = this.packetLength,
	channels = this.channels,
	sampleRate = this.sampleRate,
	isDefault = this.isDefault,
	isForced = this.isForced,
	height = this.height,
	width = this.width,
	averageFrameRate = this.averageFrameRate,
	realFrameRate = this.realFrameRate,
	profile = this.profile,
	type = this.type.asSdk(),
	aspectRatio = this.aspectRatio,
	index = this.index,
	score = this.score,
	isExternal = this.isExternal,
	deliveryMethod = this.deliveryMethod?.asSdk(),
	deliveryUrl = this.deliveryUrl,
	isExternalUrl = this.isExternalUrl,
	isTextSubtitleStream = this.isTextSubtitleStream,
	supportsExternalStream = this.supportsExternalStream,
	path = this.path,
	pixelFormat = this.pixelFormat,
	level = this.level,
	isAnamorphic = this.isAnamorphic,
	isHearingImpaired = false,
	videoRangeType = VideoRangeType.UNKNOWN,
)

fun LegacyMediaStreamType?.asSdk(): ModernMediaStreamType = when (this) {
	LegacyMediaStreamType.Audio -> ModernMediaStreamType.AUDIO
	LegacyMediaStreamType.Video -> ModernMediaStreamType.VIDEO
	LegacyMediaStreamType.Subtitle -> ModernMediaStreamType.SUBTITLE
	LegacyMediaStreamType.EmbeddedImage -> ModernMediaStreamType.EMBEDDED_IMAGE
	// Note: The apiclient doesn't have the DATA member and defaults to "null"
	null -> ModernMediaStreamType.DATA
}

fun LegacySubtitleDeliveryMethod.asSdk(): ModernSubtitleDeliveryMethod = when (this) {
	LegacySubtitleDeliveryMethod.Encode -> ModernSubtitleDeliveryMethod.ENCODE
	LegacySubtitleDeliveryMethod.Embed -> ModernSubtitleDeliveryMethod.EMBED
	LegacySubtitleDeliveryMethod.External -> ModernSubtitleDeliveryMethod.EXTERNAL
	LegacySubtitleDeliveryMethod.Hls -> ModernSubtitleDeliveryMethod.HLS
}

fun LegacyVideoType.asSdk(): ModernVideoType = when (this) {
	LegacyVideoType.VideoFile -> ModernVideoType.VIDEO_FILE
	LegacyVideoType.Iso -> ModernVideoType.ISO
	LegacyVideoType.Dvd -> ModernVideoType.DVD
	LegacyVideoType.BluRay -> ModernVideoType.BLU_RAY
	LegacyVideoType.HdDvd -> throw NotImplementedError("HdDvd not available in SDK")
}

private fun <T> Map<LegacyImageType, T>.asSdk(): Map<ModernImageType, T> = mapKeys {
	it.key.asSdk()
}

fun LegacyImageType.asSdk(): ModernImageType = when (this) {
	LegacyImageType.Primary -> ModernImageType.PRIMARY
	LegacyImageType.Art -> ModernImageType.ART
	LegacyImageType.Backdrop -> ModernImageType.BACKDROP
	LegacyImageType.Banner -> ModernImageType.BANNER
	LegacyImageType.Logo -> ModernImageType.LOGO
	LegacyImageType.Thumb -> ModernImageType.THUMB
	LegacyImageType.Disc -> ModernImageType.DISC
	LegacyImageType.Box -> ModernImageType.BOX
	LegacyImageType.Screenshot -> ModernImageType.SCREENSHOT
	LegacyImageType.Menu -> ModernImageType.MENU
	LegacyImageType.Chapter -> ModernImageType.CHAPTER
	LegacyImageType.BoxRear -> ModernImageType.BOX_REAR
}

fun LegacyChapterInfoDto.asSdk(): ModernChapterInfo = ModernChapterInfo(
	startPositionTicks = this.startPositionTicks,
	name = this.name,
	imagePath = null,
	imageDateModified = LocalDateTime.MIN, // this.imageDateModified
	imageTag = this.imageTag,
)

fun LegacyLocationType.asSdk(): ModernLocationType = when (this) {
	LegacyLocationType.FileSystem -> ModernLocationType.FILE_SYSTEM
	LegacyLocationType.Remote -> ModernLocationType.REMOTE
	LegacyLocationType.Virtual -> ModernLocationType.VIRTUAL
	LegacyLocationType.Offline -> ModernLocationType.OFFLINE
}

fun LegacyIsoType.asSdk(): ModernIsoType = when (this) {
	LegacyIsoType.Dvd -> ModernIsoType.DVD
	LegacyIsoType.BluRay -> ModernIsoType.BLU_RAY
}

fun LegacyMetadataFields.asSdk(): ModernMetadataField = when (this) {
	LegacyMetadataFields.Cast -> ModernMetadataField.CAST
	LegacyMetadataFields.Genres -> ModernMetadataField.GENRES
	LegacyMetadataFields.Keywords -> throw NotImplementedError("Keywords not available in SDK")
	LegacyMetadataFields.ProductionLocations -> ModernMetadataField.PRODUCTION_LOCATIONS
	LegacyMetadataFields.Studios -> ModernMetadataField.STUDIOS
	LegacyMetadataFields.Tags -> ModernMetadataField.TAGS
	LegacyMetadataFields.Name -> ModernMetadataField.NAME
	LegacyMetadataFields.Overview -> ModernMetadataField.OVERVIEW
	LegacyMetadataFields.Runtime -> ModernMetadataField.RUNTIME
	LegacyMetadataFields.OfficialRating -> ModernMetadataField.OFFICIAL_RATING
	LegacyMetadataFields.Images -> throw NotImplementedError("Images not available in SDK")
	LegacyMetadataFields.Backdrops -> throw NotImplementedError("Backdrops not available in SDK")
	LegacyMetadataFields.Screenshots -> throw NotImplementedError("Screenshots not available in SDK")
}

fun LegacyImageOrientation.asSdk(): ModernImageOrientation = when (this) {
	LegacyImageOrientation.TopLeft -> ModernImageOrientation.TOP_LEFT
	LegacyImageOrientation.TopRight -> ModernImageOrientation.TOP_RIGHT
	LegacyImageOrientation.BottomRight -> ModernImageOrientation.BOTTOM_RIGHT
	LegacyImageOrientation.BottomLeft -> ModernImageOrientation.BOTTOM_LEFT
	LegacyImageOrientation.LeftTop -> ModernImageOrientation.LEFT_TOP
	LegacyImageOrientation.RightTop -> ModernImageOrientation.RIGHT_TOP
	LegacyImageOrientation.RightBottom -> ModernImageOrientation.RIGHT_BOTTOM
	LegacyImageOrientation.LeftBottom -> ModernImageOrientation.LEFT_BOTTOM
}

fun LegacyChannelType.asSdk(): ModernChannelType = when (this) {
	LegacyChannelType.TV -> ModernChannelType.TV
	LegacyChannelType.Radio -> ModernChannelType.RADIO
}

fun LegacyProgramAudio.asSdk(): ModernProgramAudio = when (this) {
	LegacyProgramAudio.Mono -> ModernProgramAudio.MONO
	LegacyProgramAudio.Stereo -> ModernProgramAudio.STEREO
	LegacyProgramAudio.Dolby -> ModernProgramAudio.DOLBY
	LegacyProgramAudio.DolbyDigital -> ModernProgramAudio.DOLBY_DIGITAL
	LegacyProgramAudio.Thx -> ModernProgramAudio.THX
	LegacyProgramAudio.Atmos -> ModernProgramAudio.ATMOS
}

fun LegacySeriesTimerInfoDto.asSdk() = ModernSeriesTimerInfoDto(
	id = this.id,
	type = this.type,
	serverId = this.serverId,
	externalId = this.externalId,
	channelId = this.channelId.toUUID(),
	externalChannelId = this.externalChannelId,
	channelName = this.channelName,
	channelPrimaryImageTag = null,
	programId = this.programId,
	externalProgramId = this.externalProgramId,
	name = this.name,
	overview = this.overview,
	startDate = this.startDate.toLocalDateTime(),
	endDate = this.endDate.toLocalDateTime(),
	serviceName = this.serviceName,
	priority = this.priority,
	prePaddingSeconds = this.prePaddingSeconds,
	postPaddingSeconds = this.postPaddingSeconds,
	isPrePaddingRequired = this.isPrePaddingRequired,
	parentBackdropItemId = this.parentBackdropItemId,
	parentBackdropImageTags = this.parentBackdropImageTags,
	isPostPaddingRequired = this.isPostPaddingRequired,
	keepUntil = this.keepUntil.asSdk(),
	recordAnyTime = this.recordAnyTime,
	skipEpisodesInLibrary = this.skipEpisodesInLibrary,
	recordAnyChannel = this.recordAnyChannel,
	keepUpTo = this.keepUpTo,
	recordNewOnly = this.recordNewOnly,
	days = this.days?.mapNotNull { ModernDayOfWeek.from(it) },
	dayPattern = this.dayPattern?.asSdk(),
	imageTags = this.imageTags?.asSdk(),
	parentThumbItemId = this.parentThumbItemId,
	parentThumbImageTag = this.parentThumbImageTag,
	parentPrimaryImageItemId = this.parentPrimaryImageItemId,
	parentPrimaryImageTag = this.parentPrimaryImageTag,
)

fun LegacyKeepUntil.asSdk(): ModernKeepUntil = when (this) {
	LegacyKeepUntil.UntilDeleted -> ModernKeepUntil.UNTIL_DELETED
	LegacyKeepUntil.UntilSpaceNeeded -> ModernKeepUntil.UNTIL_SPACE_NEEDED
	LegacyKeepUntil.UntilWatched -> ModernKeepUntil.UNTIL_WATCHED
	LegacyKeepUntil.UntilDate -> ModernKeepUntil.UNTIL_DATE
}

fun LegacyDayPattern.asSdk(): ModernDayPattern = when (this) {
	LegacyDayPattern.Daily -> ModernDayPattern.DAILY
	LegacyDayPattern.Weekdays -> ModernDayPattern.WEEKDAYS
	LegacyDayPattern.Weekends -> ModernDayPattern.WEEKENDS
}

fun Array<LegacyBaseItemPerson>.asSdk() = map(LegacyBaseItemPerson::asSdk).toTypedArray()

fun LegacyChannelInfoDto.asSdk() = ModernBaseItemDto(
	name = this.name,
	serverId = this.serverId,
	id = this.id.toUUID(),
//	externalId = this.externalId,
	mediaSources = this.mediaSources?.map { it.asSdk() },
	imageTags = this.imageTags?.asSdk(),
	number = this.number,
	playAccess = this.playAccess?.asSdk(),
//	serviceName = this.serviceName,
	channelType = this.channelType?.asSdk(),
	type = BaseItemKind.from(this.type)!!,
	mediaType = MediaType.fromName(this.mediaType.lowercase()),
	userData = this.userData?.asSdk(),
	currentProgram = this.currentProgram?.asSdk(),
	primaryImageAspectRatio = this.primaryImageAspectRatio,
)

fun ModernSortOrder.asLegacy(): LegacySortOrder = when (this) {
	ModernSortOrder.ASCENDING -> LegacySortOrder.Ascending
	ModernSortOrder.DESCENDING -> LegacySortOrder.Descending
}
