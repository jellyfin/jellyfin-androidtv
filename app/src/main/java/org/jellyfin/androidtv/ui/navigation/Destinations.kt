package org.jellyfin.androidtv.ui.navigation

import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.constant.Extras
import org.jellyfin.androidtv.ui.browsing.BrowseGridFragment
import org.jellyfin.androidtv.ui.browsing.BrowseRecordingsFragment
import org.jellyfin.androidtv.ui.browsing.BrowseScheduleFragment
import org.jellyfin.androidtv.ui.browsing.BrowseViewFragment
import org.jellyfin.androidtv.ui.browsing.ByGenreFragment
import org.jellyfin.androidtv.ui.browsing.ByLetterFragment
import org.jellyfin.androidtv.ui.browsing.CollectionFragment
import org.jellyfin.androidtv.ui.browsing.GenericFolderFragment
import org.jellyfin.androidtv.ui.browsing.SuggestedMoviesFragment
import org.jellyfin.androidtv.ui.favorites.FavoritesFragment
import org.jellyfin.androidtv.ui.home.HomeFragment
import org.jellyfin.androidtv.ui.itemdetail.FullDetailsFragment
import org.jellyfin.androidtv.ui.itemdetail.ItemListFragment
import org.jellyfin.androidtv.ui.itemdetail.MusicFavoritesListFragment
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideFragment
import org.jellyfin.androidtv.ui.playback.AudioNowPlayingFragment
import org.jellyfin.androidtv.ui.playback.CustomPlaybackOverlayFragment
import org.jellyfin.androidtv.ui.playback.nextup.NextUpFragment
import org.jellyfin.androidtv.ui.playback.stillwatching.StillWatchingFragment
import org.jellyfin.androidtv.ui.player.photo.PhotoPlayerFragment
import org.jellyfin.androidtv.ui.player.video.VideoPlayerFragment
import org.jellyfin.androidtv.ui.search.SearchFragment
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID

@Suppress("TooManyFunctions")
object Destinations {
	// General
	val home = fragmentDestination<HomeFragment>()
	val favorites = fragmentDestination<FavoritesFragment>()
	fun search(query: String? = null) = fragmentDestination<SearchFragment> {
		putString(SearchFragment.EXTRA_QUERY, query)
	}

	// Browsing
	// TODO only pass item id instead of complete JSON to browsing destinations
	fun libraryBrowser(item: BaseItemDto, includeType: String? = null) =
		fragmentDestination<BrowseGridFragment> {
			putString(Extras.Folder, Json.encodeToString(item))
			putString(Extras.IncludeType, includeType)
		}

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun librarySmartScreen(item: BaseItemDto) = fragmentDestination<BrowseViewFragment> {
		putString(Extras.Folder, Json.encodeToString(item))
	}

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun collectionBrowser(item: BaseItemDto) = fragmentDestination<CollectionFragment> {
		putString(Extras.Folder, Json.encodeToString(item))
	}

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun folderBrowser(item: BaseItemDto) = fragmentDestination<GenericFolderFragment> {
		putString(Extras.Folder, Json.encodeToString(item))
	}

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun libraryByGenres(item: BaseItemDto, includeType: String) =
		fragmentDestination<ByGenreFragment> {
			putString(Extras.Folder, Json.encodeToString(item))
			putString(Extras.IncludeType, includeType)
		}

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun libraryByLetter(item: BaseItemDto, includeType: String) =
		fragmentDestination<ByLetterFragment> {
			putString(Extras.Folder, Json.encodeToString(item))
			putString(Extras.IncludeType, includeType)
		}

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun librarySuggestions(item: BaseItemDto) =
		fragmentDestination<SuggestedMoviesFragment> {
			putString(Extras.Folder, Json.encodeToString(item))
		}

	// Item details
	fun itemDetails(item: UUID) = fragmentDestination<FullDetailsFragment> {
		putString("ItemId", item.toString())
	}

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun channelDetails(item: UUID, channel: UUID, programInfo: BaseItemDto) =
		fragmentDestination<FullDetailsFragment> {
			putString("ItemId", item.toString())
			putString("ChannelId", channel.toString())
			putString("ProgramInfo", Json.encodeToString(programInfo))
		}

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun seriesTimerDetails(item: UUID, seriesTimer: SeriesTimerInfoDto) =
		fragmentDestination<FullDetailsFragment> {
			putString("ItemId", item.toString())
			putString("SeriesTimer", Json.encodeToString(seriesTimer))
		}

	fun itemList(item: UUID) = fragmentDestination<ItemListFragment> {
		putString("ItemId", item.toString())
	}

	fun musicFavorites(parent: UUID) = fragmentDestination<MusicFavoritesListFragment> {
		putString("ParentId", parent.toString())
	}

	// Live TV
	val liveTvGuide = fragmentDestination<LiveTvGuideFragment>()
	val liveTvSchedule = fragmentDestination<BrowseScheduleFragment>()
	val liveTvRecordings = fragmentDestination<BrowseRecordingsFragment>()
	val liveTvSeriesRecordings = fragmentDestination<BrowseViewFragment> {
		putBoolean(Extras.IsLiveTvSeriesRecordings, true)
	}

	// Playback
	val nowPlaying = fragmentDestination<AudioNowPlayingFragment>()

	fun photoPlayer(
		item: UUID,
		autoPlay: Boolean,
		albumSortBy: ItemSortBy?,
		albumSortOrder: SortOrder?,
	) = fragmentDestination<PhotoPlayerFragment> {
		putString(PhotoPlayerFragment.ARGUMENT_ITEM_ID, item.toString())
		putString(PhotoPlayerFragment.ARGUMENT_ALBUM_SORT_BY, albumSortBy?.serialName)
		putString(PhotoPlayerFragment.ARGUMENT_ALBUM_SORT_ORDER, albumSortOrder?.serialName)
		putBoolean(PhotoPlayerFragment.ARGUMENT_AUTO_PLAY, autoPlay)
	}

	fun videoPlayer(position: Int?) = fragmentDestination<CustomPlaybackOverlayFragment> {
		putInt("Position", position ?: 0)
	}

	fun videoPlayerNew(position: Int?) = fragmentDestination<VideoPlayerFragment> {
		putInt(VideoPlayerFragment.EXTRA_POSITION, position ?: 0)
	}

	fun nextUp(item: UUID) = fragmentDestination<NextUpFragment> {
		putString(NextUpFragment.ARGUMENT_ITEM_ID, item.toString())
	}

	fun stillWatching(item: UUID) = fragmentDestination<StillWatchingFragment> {
		putString(NextUpFragment.ARGUMENT_ITEM_ID, item.toString())
	}
}
