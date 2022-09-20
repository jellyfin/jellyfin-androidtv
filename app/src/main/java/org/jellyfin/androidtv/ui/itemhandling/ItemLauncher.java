package org.jellyfin.androidtv.ui.itemhandling;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.core.util.Consumer;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.constant.Extras;
import org.jellyfin.androidtv.constant.LiveTvOption;
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
import org.jellyfin.androidtv.util.sdk.compat.JavaCompat;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.PlayAccess;
import org.jellyfin.sdk.model.api.SearchHint;
import org.jellyfin.sdk.model.constant.CollectionType;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import kotlinx.serialization.json.Json;
import timber.log.Timber;

public class ItemLauncher {
    public static void launch(BaseRowItem rowItem, ItemRowAdapter adapter, int pos, final Activity activity) {
        launch(rowItem, adapter, pos, activity, false);
    }

    public static void createUserViewIntent(final org.jellyfin.sdk.model.api.BaseItemDto baseItem, final Context context, final Consumer<Intent> callback) {
        Timber.d("**** Collection type: %s", baseItem.getCollectionType());
        Intent intent;
        switch (baseItem.getCollectionType()) {
            case CollectionType.Movies:
            case CollectionType.TvShows:
                LibraryPreferences displayPreferences = KoinJavaComponent.<PreferencesRepository>get(PreferencesRepository.class).getLibraryPreferences(baseItem.getDisplayPreferencesId());
                boolean enableSmartScreen = displayPreferences.get(LibraryPreferences.Companion.getEnableSmartScreen());
                if (!enableSmartScreen) {
                    // open grid browsing
                    intent = new Intent(context, GenericGridActivity.class);
                    intent.putExtra(Extras.Folder, Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), baseItem));
                } else {
                    // open user view browsing
                    intent = new Intent(context, UserViewActivity.class);
                    intent.putExtra(Extras.Folder, Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), baseItem));
                }
                break;
            case CollectionType.Music:
            case CollectionType.LiveTv:
                // open user view browsing
                intent = new Intent(context, UserViewActivity.class);
                intent.putExtra(Extras.Folder, Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), baseItem));
                break;
            default:
                // open generic folder browsing
                intent = new Intent(context, GenericGridActivity.class);
                intent.putExtra(Extras.Folder, Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), baseItem));
        }

        callback.accept(intent);
    }

    public static void launchUserView(final org.jellyfin.sdk.model.api.BaseItemDto baseItem, final Activity activity, final boolean finishParent) {
        createUserViewIntent(baseItem, activity, intent -> {
            activity.startActivity(intent);
            if (finishParent) activity.finishAfterTransition();
        });
    }

    public static void launch(final BaseRowItem rowItem, ItemRowAdapter adapter, int pos, final Activity activity, final boolean noHistory) {
        KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentMediaAdapter(adapter);

        switch (rowItem.getBaseRowType()) {

            case BaseItem:
                org.jellyfin.sdk.model.api.BaseItemDto baseItem = rowItem.getBaseItem();
                try {
                    Timber.d("Item selected: %d - %s (%s)", rowItem.getIndex(), baseItem.getName(), baseItem.getType().toString());
                } catch (Exception e) {
                    //swallow it
                }

                //specialized type handling
                switch (baseItem.getType()) {
                    case USER_VIEW:
                    case COLLECTION_FOLDER:
                        launchUserView(baseItem, activity, false);
                        return;
                    case SERIES:
                    case MUSIC_ARTIST:
                        //Start activity for details display
                        Intent intent = new Intent(activity, FullDetailsActivity.class);
                        intent.putExtra("ItemId", baseItem.getId().toString());
                        if (noHistory) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        }

                        activity.startActivity(intent);

                        return;

                    case MUSIC_ALBUM:
                    case PLAYLIST:
                        //Start activity for song list display
                        Intent songListIntent = new Intent(activity, ItemListActivity.class);
                        songListIntent.putExtra("ItemId", baseItem.getId().toString());
                        if (noHistory) {
                            songListIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        }

                        activity.startActivity(songListIntent);

                        return;

                    case AUDIO:
                        Timber.d("got pos %s", pos);
                        if (rowItem.getBaseItem() == null)
                            return;

                        PlaybackLauncher playbackLauncher = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class);
                        if (playbackLauncher.interceptPlayRequest(activity, rowItem.getBaseItem())) return;

                        MediaManager mediaManager = KoinJavaComponent.<MediaManager>get(MediaManager.class);

                        // if the song currently playing is selected (and is the exact item - this only happens in the nowPlayingRow), open AudioNowPlayingActivity
                        if (mediaManager.hasAudioQueueItems() && rowItem instanceof AudioQueueItem && rowItem.getBaseItem().getId().equals(mediaManager.getCurrentAudioItem().getId())) {
                            // otherwise, open AudioNowPlayingActivity
                            Intent nowPlaying = new Intent(activity, AudioNowPlayingActivity.class);
                            activity.startActivity(nowPlaying);
                        } else if (mediaManager.hasAudioQueueItems() && rowItem instanceof AudioQueueItem && pos < mediaManager.getCurrentAudioQueueSize()) {
                            Timber.d("playing audio queue item");
                            mediaManager.playFrom(pos);
                        } else {
                            Timber.d("playing audio item");
                            List<org.jellyfin.sdk.model.api.BaseItemDto> audioItemsAsList = new ArrayList<>();

                            for (Object item : adapter) {
                                if (item instanceof BaseRowItem && ((BaseRowItem) item).getBaseItem() != null)
                                    audioItemsAsList.add(((BaseRowItem) item).getBaseItem());
                            }
                            mediaManager.playNow(activity, audioItemsAsList, pos, false);
                        }

                        return;
                    case SEASON:
                        //Start activity for enhanced browse
                        Intent seasonIntent = new Intent(activity, GenericFolderActivity.class);
                        seasonIntent.putExtra(Extras.Folder, Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), baseItem));
                        if (noHistory) {
                            seasonIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        }

                        activity.startActivity(seasonIntent);

                        return;

                    case BOX_SET:
                        // open collection browsing
                        Intent collectionIntent = new Intent(activity, CollectionActivity.class);
                        collectionIntent.putExtra(Extras.Folder, Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), baseItem));
                        if (noHistory) {
                            collectionIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        }

                        activity.startActivity(collectionIntent);
                        return;

                    case PHOTO:
                        // open photo player
                        KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentMediaPosition(pos);
                        Intent photoIntent = new Intent(activity, PhotoPlayerActivity.class);

                        activity.startActivity(photoIntent);
                        return;

                }

                // or generic handling
                if (baseItem.isFolder()) {
                    // Some items don't have a display preferences id, but it's required for StdGridFragment
                    // Use the id of the item as a workaround, it's a unique key for the specific item
                    // Which is exactly what we want
                    if (baseItem.getDisplayPreferencesId() == null) {
                        baseItem = JavaCompat.copyWithDisplayPreferencesId(baseItem, baseItem.getId().toString());
                    }

                    Intent intent = new Intent(activity, GenericGridActivity.class);
                    intent.putExtra(Extras.Folder, Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), baseItem));
                    if (noHistory) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    }

                    activity.startActivity(intent);
                } else {
                    switch (rowItem.getSelectAction()) {

                        case ShowDetails:
                            //Start details fragment for display and playback
                            Intent intent = new Intent(activity, FullDetailsActivity.class);
                            intent.putExtra("ItemId", baseItem.getId().toString());
                            if (noHistory) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            }
                            activity.startActivity(intent);
                            break;
                        case Play:
                            if (baseItem.getPlayAccess() == org.jellyfin.sdk.model.api.PlayAccess.FULL) {
                                //Just play it directly
                                final BaseItemKind itemType = baseItem.getType();
                                PlaybackHelper.getItemsToPlay(baseItem, baseItem.getType() == BaseItemKind.MOVIE, false, new Response<List<org.jellyfin.sdk.model.api.BaseItemDto>>() {
                                    @Override
                                    public void onResponse(List<org.jellyfin.sdk.model.api.BaseItemDto> response) {
                                        Class newActivity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(itemType);
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
                intent.putExtra("ItemId", rowItem.getBasePerson().getId().toString());
                if (noHistory) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                }

                activity.startActivity(intent);

                break;
            case Chapter:
                final ChapterItemInfo chapter = rowItem.getChapterInfo();
                //Start playback of the item at the chapter point
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(chapter.getItemId().toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        List<BaseItemDto> items = new ArrayList<>();
                        items.add(response);
                        KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentVideoQueue(JavaCompat.mapBaseItemCollection(items));
                        Class newActivity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(ModelCompat.asSdk(response).getType());
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
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(hint.getItemId().toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if (response.getIsFolderItem() && ModelCompat.asSdk(response).getType() != BaseItemKind.SERIES) {
                            // open generic folder browsing
                            Intent intent = new Intent(activity, GenericGridActivity.class);
                            intent.putExtra(Extras.Folder, Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), ModelCompat.asSdk(response)));

                            activity.startActivity(intent);

                        } else if (ModelCompat.asSdk(response).getType() == BaseItemKind.AUDIO) {
                            PlaybackHelper.retrieveAndPlay(response.getId(), false, activity);
                            //produce item menu
//                            KeyProcessor.HandleKey(KeyEvent.KEYCODE_MENU, rowItem, (BaseActivity) activity);
                            return;

                        } else {
                            Intent intent = new Intent(activity, FullDetailsActivity.class);
                            intent.putExtra("ItemId", response.getId().toString());
                            if (ModelCompat.asSdk(response).getType() == BaseItemKind.PROGRAM) {
                                // TODO: Seems like this is never used...
                                intent.putExtra("ItemType", response.getBaseItemType().name());

                                intent.putExtra("ChannelId", response.getChannelId());
                                intent.putExtra("ProgramInfo", Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), ModelCompat.asSdk(response)));
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
                org.jellyfin.sdk.model.api.BaseItemDto program = rowItem.getBaseItem();
                switch (rowItem.getSelectAction()) {

                    case ShowDetails:
                        //Start details fragment for display and playback
                        Intent programIntent = new Intent(activity, FullDetailsActivity.class);
                        programIntent.putExtra("ItemId", program.getId().toString());

                        // TODO Seems unused
                        programIntent.putExtra("ItemType", program.getType().name());

                        programIntent.putExtra("ChannelId", program.getChannelId());
                        programIntent.putExtra("ProgramInfo", Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), program));

                        activity.startActivity(programIntent);
                        break;
                    case Play:
                        if (program.getPlayAccess() == org.jellyfin.sdk.model.api.PlayAccess.FULL) {
                            //Just play it directly - need to retrieve program channel via items api to convert to BaseItem
                            KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(program.getChannelId().toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    List<BaseItemDto> items = new ArrayList<>();
                                    items.add(response);
                                    Class newActivity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(ModelCompat.asSdk(response).getType());
                                    Intent intent = new Intent(activity, newActivity);
                                    KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentVideoQueue(JavaCompat.mapBaseItemCollection(items));
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
                final org.jellyfin.sdk.model.api.BaseItemDto channel = rowItem.getBaseItem();
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(channel.getId().toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        PlaybackHelper.getItemsToPlay(ModelCompat.asSdk(response), false, false, new Response<List<org.jellyfin.sdk.model.api.BaseItemDto>>() {
                            @Override
                            public void onResponse(List<org.jellyfin.sdk.model.api.BaseItemDto> response) {
                                // TODO Check whether this usage of BaseItemType.valueOf is okay.
                                Class newActivity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(channel.getType());
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
                        recIntent.putExtra("ItemId", rowItem.getBaseItem().getId().toString());

                        activity.startActivity(recIntent);
                        break;
                    case Play:
                        if (rowItem.getBaseItem().getPlayAccess() == PlayAccess.FULL) {
                            //Just play it directly but need to retrieve as base item
                            KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(rowItem.getBaseItem().getId().toString(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    Class newActivity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(rowItem.getBaseItemType());
                                    Intent intent = new Intent(activity, newActivity);
                                    List<BaseItemDto> items = new ArrayList<>();
                                    items.add(response);
                                    KoinJavaComponent.<MediaManager>get(MediaManager.class).setCurrentVideoQueue(JavaCompat.mapBaseItemCollection(items));
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
                timerIntent.putExtra("SeriesTimer", Json.Default.encodeToString(org.jellyfin.sdk.model.api.SeriesTimerInfoDto.Companion.serializer(), ModelCompat.asSdk(rowItem.getSeriesTimerInfo())));

                activity.startActivity(timerIntent);
                break;


            case GridButton:
                switch (rowItem.getGridButton().getId()) {
                    case LiveTvOption.LIVE_TV_GUIDE_OPTION_ID:
                        Intent guide = new Intent(activity, LiveTvGuideActivity.class);
                        activity.startActivity(guide);
                        break;

                    case LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID:
                        Intent recordings = new Intent(activity, BrowseRecordingsActivity.class);
                        BaseItemDto folder = new BaseItemDto();
                        folder.setId(UUID.randomUUID().toString());
                        folder.setBaseItemType(BaseItemType.Folder);
                        folder.setName(activity.getString(R.string.lbl_recorded_tv));
                        recordings.putExtra(Extras.Folder, Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), ModelCompat.asSdk(folder)));
                        activity.startActivity(recordings);
                        break;

                    case LiveTvOption.LIVE_TV_SERIES_OPTION_ID:
                        Intent seriesIntent = new Intent(activity, UserViewActivity.class);
                        BaseItemDto seriesTimers = new BaseItemDto();
                        seriesTimers.setId(UUID.randomUUID().toString());
                        seriesTimers.setBaseItemType(BaseItemType.Folder);
                        seriesTimers.setCollectionType("SeriesTimers");
                        seriesTimers.setName(activity.getString(R.string.lbl_series_recordings));
                        seriesIntent.putExtra(Extras.Folder, Json.Default.encodeToString(org.jellyfin.sdk.model.api.BaseItemDto.Companion.serializer(), ModelCompat.asSdk(seriesTimers)));

                        activity.startActivity(seriesIntent);
                        break;

                    case LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID:
                        Intent schedIntent = new Intent(activity, BrowseScheduleActivity.class);
                        activity.startActivity(schedIntent);
                        break;
                }
                break;
        }
    }
}
