package org.jellyfin.androidtv.ui.itemhandling;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.core.util.Consumer;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.Extras;
import org.jellyfin.androidtv.data.model.ChapterItemInfo;
import org.jellyfin.androidtv.preference.LibraryPreferences;
import org.jellyfin.androidtv.preference.PreferencesRepository;
import org.jellyfin.androidtv.ui.browsing.BrowseRecordingsActivity;
import org.jellyfin.androidtv.ui.browsing.BrowseScheduleActivity;
import org.jellyfin.androidtv.ui.browsing.CollectionActivity;
import org.jellyfin.androidtv.ui.browsing.GenericFolderActivity;
import org.jellyfin.androidtv.ui.browsing.GenericGridActivity;
import org.jellyfin.androidtv.ui.browsing.UserViewActivity;
import org.jellyfin.androidtv.ui.itemdetail.FullDetailsActivity;
import org.jellyfin.androidtv.ui.itemdetail.ItemListActivity;
import org.jellyfin.androidtv.ui.itemdetail.PhotoPlayerActivity;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideActivity;
import org.jellyfin.androidtv.ui.playback.AudioNowPlayingActivity;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.library.PlayAccess;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.search.SearchHint;
import org.jellyfin.apiclient.serialization.GsonJsonSerializer;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ItemLauncher {
    public static void launch(BaseRowItem rowItem, ItemRowAdapter adapter, int pos, final Activity activity) {
        launch(rowItem, adapter, pos, activity, false);
    }

    public static void createUserViewIntent(final BaseItemDto baseItem, final Context context, final Consumer<Intent> callback) {
        if (baseItem.getCollectionType() == null) {
            baseItem.setCollectionType("unknown");
        }
        Timber.d("**** Collection type: %s", baseItem.getCollectionType());
        Intent intent;
        switch (baseItem.getCollectionType()) {
            case "movies":
            case "tvshows":
                LibraryPreferences displayPreferences = KoinJavaComponent.<PreferencesRepository>get(PreferencesRepository.class).getLibraryPreferences(baseItem.getDisplayPreferencesId());
                boolean enableSmartScreen = displayPreferences.get(LibraryPreferences.Companion.getEnableSmartScreen());
                if (!enableSmartScreen) {
                    // open grid browsing
                    intent = new Intent(context, GenericGridActivity.class);
                    intent.putExtra(Extras.Folder, KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(baseItem));
                } else {
                    // open user view browsing
                    intent = new Intent(context, UserViewActivity.class);
                    intent.putExtra(Extras.Folder, KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(baseItem));
                }
                break;
            case "music":
            case "livetv":
                // open user view browsing
                intent = new Intent(context, UserViewActivity.class);
                intent.putExtra(Extras.Folder, KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(baseItem));
                break;
            default:
                // open generic folder browsing
                intent = new Intent(context, GenericGridActivity.class);
                intent.putExtra(Extras.Folder, KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(baseItem));
        }

        callback.accept(intent);
    }

    public static void launchUserView(final BaseItemDto baseItem, final Activity activity, final boolean finishParent) {
        createUserViewIntent(baseItem, activity, intent -> {
            activity.startActivity(intent);
            if (finishParent) activity.finishAfterTransition();
        });
    }

    public static void launch(final BaseRowItem rowItem, ItemRowAdapter adapter, int pos, final Activity activity, final boolean noHistory) {
        KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentMediaAdapter(adapter);

        switch (rowItem.getItemType()) {

            case BaseItem:
                final BaseItemDto baseItem = rowItem.getBaseItem();
                try {
                    Timber.d("Item selected: %d - %s (%s)", rowItem.getIndex(), baseItem.getName(), baseItem.getBaseItemType().toString());
                } catch (Exception e) {
                    //swallow it
                }

                //specialized type handling
                switch (baseItem.getBaseItemType()) {
                    case UserView:
                    case CollectionFolder:
                        launchUserView(baseItem, activity, false);
                        return;
                    case Series:
                    case MusicArtist:
                        //Start activity for details display
                        Intent intent = new Intent(activity, FullDetailsActivity.class);
                        intent.putExtra("ItemId", baseItem.getId());
                        if (noHistory) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        }

                        activity.startActivity(intent);

                        return;

                    case MusicAlbum:
                    case Playlist:
                        //Start activity for song list display
                        Intent songListIntent = new Intent(activity, ItemListActivity.class);
                        songListIntent.putExtra("ItemId", baseItem.getId());
                        if (noHistory) {
                            songListIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        }

                        activity.startActivity(songListIntent);

                        return;

                    case Audio:
                        Timber.d("got pos %s", pos);
                        if (rowItem.getBaseItem() == null)
                            return;
                        MediaManager mediaManager = KoinJavaComponent.<MediaManager>get(MediaManager.class);

                        // if the song currently playing is selected (and is the exact item - this only happens in the nowPlayingRow), open AudioNowPlayingActivity
                        if (mediaManager.hasAudioQueueItems() && rowItem.getBaseItem() == mediaManager.getCurrentAudioItem()) {
                            // otherwise, open AudioNowPlayingActivity
                            Intent nowPlaying = new Intent(activity, AudioNowPlayingActivity.class);
                            activity.startActivity(nowPlaying);
                        } else if (mediaManager.hasAudioQueueItems() && rowItem instanceof AudioQueueItem && pos < mediaManager.getCurrentAudioQueueSize()) {
                            Timber.d("playing audio queue item");
                            mediaManager.playFrom(pos);
                        } else {
                            Timber.d("playing audio item");
                            List<BaseItemDto> audioItemsAsList = new ArrayList<>();

                            for (Object item : adapter.unmodifiableList()) {
                                if (item instanceof BaseRowItem && ((BaseRowItem) item).getBaseItem() != null)
                                    audioItemsAsList.add(((BaseRowItem) item).getBaseItem());
                            }
                            mediaManager.playNow(activity, audioItemsAsList, pos, false);
                        }

                        return;
                    case Season:
                    case RecordingGroup:
                        //Start activity for enhanced browse
                        Intent seasonIntent = new Intent(activity, GenericFolderActivity.class);
                        seasonIntent.putExtra(Extras.Folder, KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(baseItem));
                        if (noHistory) {
                            seasonIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        }

                        activity.startActivity(seasonIntent);

                        return;

                    case BoxSet:
                        // open collection browsing
                        Intent collectionIntent = new Intent(activity, CollectionActivity.class);
                        collectionIntent.putExtra(Extras.Folder, KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(baseItem));
                        if (noHistory) {
                            collectionIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        }

                        activity.startActivity(collectionIntent);
                        return;

                    case Photo:
                        // open photo player
                        KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentMediaPosition(pos);
                        Intent photoIntent = new Intent(activity, PhotoPlayerActivity.class);

                        activity.startActivity(photoIntent);
                        return;

                }

                // or generic handling
                if (baseItem.getIsFolderItem()) {
                    // Some items don't have a display preferences id, but it's required for StdGridFragment
                    // Use the id of the item as a workaround, it's a unique key for the specific item
                    // Which is exactly what we want
                    if (baseItem.getDisplayPreferencesId() == null) {
                        baseItem.setDisplayPreferencesId(baseItem.getId());
                    }

                    Intent intent = new Intent(activity, GenericGridActivity.class);
                    intent.putExtra(Extras.Folder, KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(baseItem));
                    if (noHistory) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    }

                    activity.startActivity(intent);
                } else {
                    switch (rowItem.getSelectAction()) {

                        case ShowDetails:
                            //Start details fragment for display and playback
                            Intent intent = new Intent(activity, FullDetailsActivity.class);
                            intent.putExtra("ItemId", baseItem.getId());
                            if (noHistory) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            }
                            activity.startActivity(intent);
                            break;
                        case Play:
                            if (baseItem.getPlayAccess() == PlayAccess.Full) {
                                //Just play it directly
                                PlaybackHelper.getItemsToPlay(baseItem, baseItem.getBaseItemType() == BaseItemType.Movie, false, new Response<List<BaseItemDto>>() {
                                    @Override
                                    public void onResponse(List<BaseItemDto> response) {
                                        Class newActivity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(baseItem.getBaseItemType());
                                        Intent intent = new Intent(activity, newActivity);
                                        KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentVideoQueue(response);
                                        intent.putExtra("Position", 0);
                                        activity.startActivity(intent);
                                    }
                                });
                            } else {
                                Utils.showToast(activity, "Item not playable at this time");
                            }
                            break;
                    }
                }
                break;
            case Person:
                //Start details fragment
                Intent intent = new Intent(activity, FullDetailsActivity.class);
                intent.putExtra("ItemId", rowItem.getPerson().getId());
                if (noHistory) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                }

                activity.startActivity(intent);

                break;
            case Chapter:
                final ChapterItemInfo chapter = rowItem.getChapterInfo();
                //Start playback of the item at the chapter point
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(chapter.getItemId(), TvApp.getApplication().getCurrentUser().getId().toString(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        List<BaseItemDto> items = new ArrayList<>();
                        items.add(response);
                        KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentVideoQueue(items);
                        Class newActivity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(response.getBaseItemType());
                        Intent intent = new Intent(activity, newActivity);
                        Long start = chapter.getStartPositionTicks() / 10000;
                        intent.putExtra("Position", start.intValue());
                        activity.startActivity(intent);
                    }
                });

                break;

            case SearchHint:
                final SearchHint hint = rowItem.getSearchHint();
                //Retrieve full item for display and playback
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(hint.getItemId(), TvApp.getApplication().getCurrentUser().getId().toString(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if (response.getIsFolderItem() && response.getBaseItemType() != BaseItemType.Series) {
                            // open generic folder browsing
                            Intent intent = new Intent(activity, GenericGridActivity.class);
                            intent.putExtra(Extras.Folder, KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(response));

                            activity.startActivity(intent);

                        } else if (response.getBaseItemType() == BaseItemType.Audio) {
                            PlaybackHelper.retrieveAndPlay(response.getId(), false, activity);
                            //produce item menu
//                            KeyProcessor.HandleKey(KeyEvent.KEYCODE_MENU, rowItem, (BaseActivity) activity);
                            return;

                        } else {
                            Intent intent = new Intent(activity, FullDetailsActivity.class);
                            intent.putExtra("ItemId", response.getId());
                            if (response.getBaseItemType() == BaseItemType.Program) {
                                // TODO: Seems like this is never used...
                                intent.putExtra("ItemType", response.getBaseItemType().name());

                                intent.putExtra("ChannelId", response.getChannelId());
                                intent.putExtra("ProgramInfo", KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(response));
                            }
                            activity.startActivity(intent);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        Timber.e(exception, "Error retrieving full object");
                        exception.printStackTrace();
                    }
                });
                break;
            case LiveTvProgram:
                BaseItemDto program = rowItem.getProgramInfo();
                switch (rowItem.getSelectAction()) {

                    case ShowDetails:
                        //Start details fragment for display and playback
                        Intent programIntent = new Intent(activity, FullDetailsActivity.class);
                        programIntent.putExtra("ItemId", program.getId());

                        // TODO Seems unused
                        programIntent.putExtra("ItemType", program.getBaseItemType().name());

                        programIntent.putExtra("ChannelId", program.getChannelId());
                        programIntent.putExtra("ProgramInfo", KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(program));

                        activity.startActivity(programIntent);
                        break;
                    case Play:
                        if (program.getPlayAccess() == PlayAccess.Full) {
                            //Just play it directly - need to retrieve program channel via items api to convert to BaseItem
                            KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(program.getChannelId(), TvApp.getApplication().getCurrentUser().getId().toString(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    List<BaseItemDto> items = new ArrayList<>();
                                    items.add(response);
                                    Class newActivity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(response.getBaseItemType());
                                    Intent intent = new Intent(activity, newActivity);
                                    KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentVideoQueue(items);
                                    intent.putExtra("Position", 0);
                                    activity.startActivity(intent);

                                }
                            });
                        } else {
                            Utils.showToast(activity, "Item not playable at this time");
                        }
                }
                break;

            case LiveTvChannel:
                //Just tune to it by playing
                final ChannelInfoDto channel = rowItem.getChannelInfo();
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(channel.getId(), TvApp.getApplication().getCurrentUser().getId().toString(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        PlaybackHelper.getItemsToPlay(response, false, false, new Response<List<BaseItemDto>>() {
                            @Override
                            public void onResponse(List<BaseItemDto> response) {
                                // TODO Check whether this usage of BaseItemType.valueOf is okay.
                                Class newActivity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(BaseItemType.valueOf(channel.getType()));
                                Intent intent = new Intent(activity, newActivity);
                                KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentVideoQueue(response);
                                intent.putExtra("Position", 0);
                                activity.startActivity(intent);

                            }
                        });
                    }
                });
                break;

            case LiveTvRecording:
                switch (rowItem.getSelectAction()) {

                    case ShowDetails:
                        //Start details fragment for display and playback
                        Intent recIntent = new Intent(activity, FullDetailsActivity.class);
                        recIntent.putExtra("ItemId", rowItem.getRecordingInfo().getId());

                        activity.startActivity(recIntent);
                        break;
                    case Play:
                        if (rowItem.getRecordingInfo().getPlayAccess() == PlayAccess.Full) {
                            //Just play it directly but need to retrieve as base item
                            KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(rowItem.getRecordingInfo().getId(), TvApp.getApplication().getCurrentUser().getId().toString(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    Class newActivity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(rowItem.getBaseItemType());
                                    Intent intent = new Intent(activity, newActivity);
                                    List<BaseItemDto> items = new ArrayList<>();
                                    items.add(response);
                                    KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentVideoQueue(items);
                                    intent.putExtra("Position", 0);
                                    activity.startActivity(intent);
                                }
                            });
                        } else {
                            Utils.showToast(activity, "Item not playable at this time");
                        }
                        break;
                }
                break;

            case SeriesTimer:
                //Start details fragment for display and playback
                Intent timerIntent = new Intent(activity, FullDetailsActivity.class);
                timerIntent.putExtra("ItemId", rowItem.getItemId());
                timerIntent.putExtra("ItemType", "SeriesTimer");
                timerIntent.putExtra("SeriesTimer", KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(rowItem.getSeriesTimerInfo()));

                activity.startActivity(timerIntent);
                break;


            case GridButton:
                switch (rowItem.getGridButton().getId()) {
                    case TvApp.LIVE_TV_GUIDE_OPTION_ID:
                        Intent guide = new Intent(activity, LiveTvGuideActivity.class);
                        activity.startActivity(guide);
                        break;

                    case TvApp.LIVE_TV_RECORDINGS_OPTION_ID:
                        Intent recordings = new Intent(activity, BrowseRecordingsActivity.class);
                        BaseItemDto folder = new BaseItemDto();
                        folder.setId("");
                        folder.setName(activity.getString(R.string.lbl_recorded_tv));
                        recordings.putExtra(Extras.Folder, KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(folder));
                        activity.startActivity(recordings);
                        break;

                    case TvApp.VIDEO_QUEUE_OPTION_ID:
                        Intent queueIntent = new Intent(activity, ItemListActivity.class);
                        queueIntent.putExtra("ItemId", ItemListActivity.VIDEO_QUEUE);
                        //Resume first item if needed
                        List<BaseItemDto> items = KoinJavaComponent.<MediaManager>get(MediaManager.class).getCurrentVideoQueue();
                        if (items != null) {
                            BaseItemDto first = items.size() > 0 ? items.get(0) : null;
                            if (first != null && first.getUserData() != null) {
                                Long resume = first.getUserData().getPlaybackPositionTicks() / 10000;
                                queueIntent.putExtra("Position", resume.intValue());

                            }
                        }

                        activity.startActivity(queueIntent);
                        break;

                    case TvApp.LIVE_TV_SERIES_OPTION_ID:
                        Intent seriesIntent = new Intent(activity, UserViewActivity.class);
                        BaseItemDto seriesTimers = new BaseItemDto();
                        seriesTimers.setId("SERIESTIMERS");
                        seriesTimers.setCollectionType("SeriesTimers");
                        seriesTimers.setName(activity.getString(R.string.lbl_series_recordings));
                        seriesIntent.putExtra(Extras.Folder, KoinJavaComponent.<GsonJsonSerializer>get(GsonJsonSerializer.class).SerializeToString(seriesTimers));

                        activity.startActivity(seriesIntent);
                        break;

                    case TvApp.LIVE_TV_SCHEDULE_OPTION_ID:
                        Intent schedIntent = new Intent(activity, BrowseScheduleActivity.class);
                        activity.startActivity(schedIntent);
                        break;
                }
                break;
        }
    }
}
