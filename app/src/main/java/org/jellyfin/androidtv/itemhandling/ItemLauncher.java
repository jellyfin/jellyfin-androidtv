package org.jellyfin.androidtv.itemhandling;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.BaseActivity;
import org.jellyfin.androidtv.browsing.BrowseRecordingsActivity;
import org.jellyfin.androidtv.browsing.BrowseScheduleActivity;
import org.jellyfin.androidtv.browsing.CollectionActivity;
import org.jellyfin.androidtv.browsing.GenericFolderActivity;
import org.jellyfin.androidtv.browsing.GenericGridActivity;
import org.jellyfin.androidtv.browsing.UserViewActivity;
import org.jellyfin.androidtv.details.DetailsActivity;
import org.jellyfin.androidtv.details.ItemListActivity;
import org.jellyfin.androidtv.details.PhotoPlayerActivity;
import org.jellyfin.androidtv.livetv.LiveTvGuideActivity;
import org.jellyfin.androidtv.model.ChapterItemInfo;
import org.jellyfin.androidtv.model.ViewType;
import org.jellyfin.androidtv.playback.MediaManager;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserDto;
import org.jellyfin.apiclient.model.entities.DisplayPreferences;
import org.jellyfin.apiclient.model.library.PlayAccess;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.search.SearchHint;

import java.util.ArrayList;
import java.util.List;

public class ItemLauncher {
    public static void launch(BaseRowItem rowItem, ItemRowAdapter adapter, int pos, final Activity activity) {
        launch(rowItem, adapter, pos, activity, false);
    }

    public static void launchUserView(final BaseItemDto baseItem, final Activity context, final boolean finishParent) {
        //We need to get display prefs...
        TvApp.getApplication().getDisplayPrefsAsync(baseItem.getDisplayPreferencesId(), new Response<DisplayPreferences>() {
            @Override
            public void onResponse(DisplayPreferences response) {
                if (baseItem.getCollectionType() == null) {
                    baseItem.setCollectionType("unknown");
                }
                TvApp.getApplication().getLogger().Debug("**** Collection type: %s", baseItem.getCollectionType());
                switch (baseItem.getCollectionType()) {
                    case "movies":
                    case "tvshows":
                    case "music":
                        TvApp.getApplication().getLogger().Debug("**** View Type Pref: %s", response.getCustomPrefs().get("DefaultView"));
                        if (ViewType.GRID.equals(response.getCustomPrefs().get("DefaultView"))) {
                            // open grid browsing
                            Intent folderIntent = new Intent(context, GenericGridActivity.class);
                            folderIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));
                            context.startActivity(folderIntent);
                            if (finishParent) context.finish();

                        } else {
                            // open user view browsing
                            Intent intent = new Intent(context, UserViewActivity.class);
                            intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));

                            context.startActivity(intent);
                            if (finishParent) context.finish();
                        }
                        break;
                    case "livetv":
                        // open user view browsing
                        Intent intent = new Intent(context, UserViewActivity.class);
                        intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));

                        context.startActivity(intent);
                        if (finishParent) context.finish();
                        break;
                    default:
                        // open generic folder browsing
                        Intent folderIntent = new Intent(context, GenericGridActivity.class);
                        folderIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));
                        context.startActivity(folderIntent);
                        if (finishParent) context.finish();
                }
            }
        });
    }

    public static void launch(final BaseRowItem rowItem, ItemRowAdapter adapter, int pos, final Activity activity, final boolean noHistory) {
        final TvApp application = TvApp.getApplication();
        MediaManager.setCurrentMediaAdapter(adapter);

        switch (rowItem.getItemType()) {

            case BaseItem:
                final BaseItemDto baseItem = rowItem.getBaseItem();
                try {
                    TvApp.getApplication().getLogger().Debug("Item selected: %d - %s (%s)", rowItem.getIndex(), baseItem.getName(), baseItem.getBaseItemType().toString());
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
                        // Open Series or MusicArtist in details activity
                        Intent intent = new Intent(activity, DetailsActivity.class);
                        intent.putExtra(DetailsActivity.EXTRA_ITEM_ID, baseItem.getId());
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
                        //produce item menu
                        KeyProcessor.HandleKey(KeyEvent.KEYCODE_MENU, rowItem, (BaseActivity) activity);
                        return;

                    case Season:
                    case RecordingGroup:
                        //Start activity for enhanced browse
                        Intent seasonIntent = new Intent(activity, GenericFolderActivity.class);
                        seasonIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));
                        if (noHistory) {
                            seasonIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        }

                        activity.startActivity(seasonIntent);

                        return;

                    case BoxSet:
                        // open collection browsing
                        Intent collectionIntent = new Intent(activity, CollectionActivity.class);
                        collectionIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));
                        if (noHistory) {
                            collectionIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        }

                        activity.startActivity(collectionIntent);
                        return;

                    case Photo:
                        // open photo player
                        MediaManager.setCurrentMediaPosition(pos);
                        Intent photoIntent = new Intent(activity, PhotoPlayerActivity.class);

                        activity.startActivity(photoIntent);
                        return;

                }

                // or generic handling
                if (baseItem.getIsFolderItem()) {
                    // open generic folder browsing - but need display prefs
                    TvApp.getApplication().getDisplayPrefsAsync(baseItem.getDisplayPreferencesId(), new Response<DisplayPreferences>() {
                        @Override
                        public void onResponse(DisplayPreferences response) {
                            Intent intent = new Intent(activity, GenericGridActivity.class);
                            intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));
                            if (noHistory) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            }

                            activity.startActivity(intent);

                        }
                    });
                } else {
                    switch (rowItem.getSelectAction()) {

                        case ShowDetails:
                            //Start details fragment for display and playback
                            Intent intent = new Intent(activity, DetailsActivity.class);
                            intent.putExtra(DetailsActivity.EXTRA_ITEM_ID, baseItem.getId());
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
                                        Intent intent = new Intent(activity, application.getPlaybackActivityClass(baseItem.getBaseItemType()));
                                        MediaManager.setCurrentVideoQueue(response);
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
                // Open Person in Details Activity
                Intent intent = new Intent(activity, DetailsActivity.class);
                intent.putExtra(DetailsActivity.EXTRA_ITEM_ID, rowItem.getPerson().getId());
                if (noHistory) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                }

                activity.startActivity(intent);

                break;
            case Chapter:
                final ChapterItemInfo chapter = rowItem.getChapterInfo();
                //Start playback of the item at the chapter point
                application.getApiClient().GetItemAsync(chapter.getItemId(), application.getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        List<BaseItemDto> items = new ArrayList<>();
                        items.add(response);
                        MediaManager.setCurrentVideoQueue(items);
                        Intent intent = new Intent(activity, application.getPlaybackActivityClass(response.getBaseItemType()));
                        Long start = chapter.getStartPositionTicks() / 10000;
                        intent.putExtra("Position", start.intValue());
                        activity.startActivity(intent);
                    }
                });

                break;
            case Server:
                //Log in to selected server
                AuthenticationHelper.signInToServer(application.getConnectionManager(), rowItem.getServerInfo().getAddress(), activity);
                break;

            case User:
                final UserDto user = rowItem.getUser();
                if (user.getHasPassword()) {
                    Utils.processPasswordEntry(activity, user);

                } else {
                    AuthenticationHelper.loginUser(user.getName(), "", application.getLoginApiClient(), activity);
                }
                break;

            case SearchHint:
                final SearchHint hint = rowItem.getSearchHint();
                //Retrieve full item for display and playback
                application.getApiClient().GetItemAsync(hint.getItemId(), application.getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if (response.getIsFolderItem() && response.getBaseItemType() != BaseItemType.Series) {
                            // open generic folder browsing
                            Intent intent = new Intent(activity, GenericGridActivity.class);
                            intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(response));

                            activity.startActivity(intent);

                        } else if (response.getBaseItemType() == BaseItemType.Audio) {
                            PlaybackHelper.retrieveAndPlay(response.getId(), false, activity);
                            //produce item menu
//                            KeyProcessor.HandleKey(KeyEvent.KEYCODE_MENU, rowItem, (BaseActivity) activity);
                            return;

                        } else {
                            // Open All items in details activity
                            Intent intent = new Intent(activity, DetailsActivity.class);
                            intent.putExtra(DetailsActivity.EXTRA_ITEM_ID, response.getId());
                            activity.startActivity(intent);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        application.getLogger().ErrorException("Error retrieving full object", exception);
                        exception.printStackTrace();
                    }
                });
                break;
            case LiveTvProgram:
                BaseItemDto program = rowItem.getProgramInfo();
                switch (rowItem.getSelectAction()) {

                    case ShowDetails:
                        // Open LiveTvProgram in Details Activity
                        Intent programIntent = new Intent(activity, DetailsActivity.class);
                        programIntent.putExtra(DetailsActivity.EXTRA_ITEM_ID, program.getId());
                        activity.startActivity(programIntent);
                        break;
                    case Play:
                        if (program.getPlayAccess() == PlayAccess.Full) {
                            //Just play it directly - need to retrieve program channel via items api to convert to BaseItem
                            TvApp.getApplication().getApiClient().GetItemAsync(program.getChannelId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    List<BaseItemDto> items = new ArrayList<>();
                                    items.add(response);
                                    Intent intent = new Intent(activity, TvApp.getApplication().getPlaybackActivityClass(response.getBaseItemType()));
                                    MediaManager.setCurrentVideoQueue(items);
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
                TvApp.getApplication().getApiClient().GetItemAsync(channel.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        PlaybackHelper.getItemsToPlay(response, false, false, new Response<List<BaseItemDto>>() {
                            @Override
                            public void onResponse(List<BaseItemDto> response) {
                                // TODO Check whether this usage of BaseItemType.valueOf is okay.
                                Intent intent = new Intent(activity, application.getPlaybackActivityClass(BaseItemType.valueOf(channel.getType())));
                                MediaManager.setCurrentVideoQueue(response);
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
                        Intent recIntent = new Intent(activity, DetailsActivity.class);
                        recIntent.putExtra(DetailsActivity.EXTRA_ITEM_ID, rowItem.getRecordingInfo().getId());

                        activity.startActivity(recIntent);
                        break;
                    case Play:
                        if (rowItem.getRecordingInfo().getPlayAccess() == PlayAccess.Full) {
                            //Just play it directly but need to retrieve as base item
                            TvApp.getApplication().getApiClient().GetItemAsync(rowItem.getRecordingInfo().getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    Intent intent = new Intent(activity, application.getPlaybackActivityClass(rowItem.getBaseItemType()));
                                    List<BaseItemDto> items = new ArrayList<>();
                                    items.add(response);
                                    MediaManager.setCurrentVideoQueue(items);
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
                // Open SeriesTimer in details activity
                Intent timerIntent = new Intent(activity, DetailsActivity.class);
                timerIntent.putExtra(DetailsActivity.EXTRA_ITEM_ID, rowItem.getItemId());

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
                        folder.setName(TvApp.getApplication().getResources().getString(R.string.lbl_recorded_tv));
                        recordings.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(folder));
                        activity.startActivity(recordings);
                        break;

                    case TvApp.VIDEO_QUEUE_OPTION_ID:
                        Intent queueIntent = new Intent(activity, ItemListActivity.class);
                        queueIntent.putExtra("ItemId", ItemListActivity.VIDEO_QUEUE);
                        //Resume first item if needed
                        List<BaseItemDto> items = MediaManager.getCurrentVideoQueue();
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
                        seriesIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(seriesTimers));

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
