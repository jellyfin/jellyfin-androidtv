package tv.emby.embyatv.itemhandling;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;

import java.util.List;

import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.entities.DisplayPreferences;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.search.SearchHint;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.browsing.BrowseRecordingsActivity;
import tv.emby.embyatv.browsing.CollectionActivity;
import tv.emby.embyatv.browsing.GenericFolderActivity;
import tv.emby.embyatv.browsing.GenericGridActivity;
import tv.emby.embyatv.browsing.MainActivity;
import tv.emby.embyatv.browsing.UserViewActivity;
import tv.emby.embyatv.details.FullDetailsActivity;
import tv.emby.embyatv.details.PhotoPlayerActivity;
import tv.emby.embyatv.details.SongListActivity;
import tv.emby.embyatv.livetv.LiveTvGuideActivity;
import tv.emby.embyatv.model.ChapterItemInfo;
import tv.emby.embyatv.model.ViewType;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.playback.PlaybackOverlayActivity;
import tv.emby.embyatv.startup.SelectUserActivity;
import tv.emby.embyatv.util.DelayedMessage;
import tv.emby.embyatv.util.KeyProcessor;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 12/21/2014.
 */
public class ItemLauncher {
    public static void launch(BaseRowItem rowItem, ItemRowAdapter adapter, int pos, final Activity activity) {
        launch(rowItem, adapter, pos, activity, false);
    }

    public static void launch(BaseRowItem rowItem, ItemRowAdapter adapter, int pos, final Activity activity, final boolean noHistory) {
        final TvApp application = TvApp.getApplication();
        MediaManager.setCurrentMediaAdapter(adapter);

        switch (rowItem.getItemType()) {

            case BaseItem:
                final BaseItemDto baseItem = rowItem.getBaseItem();
                try {
                    TvApp.getApplication().getLogger().Debug("Item selected: " + rowItem.getIndex() + " - " + baseItem.getName() + " (" + baseItem.getType() + ")");
                } catch (Exception e) {
                    //swallow it
                }

                //specialized type handling
                switch (baseItem.getType()) {
                    case "UserView":
                    case "CollectionFolder":
                        //We need to get display prefs...
                        TvApp.getApplication().getDisplayPrefsAsync(baseItem.getDisplayPreferencesId(), new Response<DisplayPreferences>() {
                            @Override
                            public void onResponse(DisplayPreferences response) {
                                if (baseItem.getCollectionType() == null)
                                    baseItem.setCollectionType("unknown");
                                TvApp.getApplication().getLogger().Debug("**** Collection type: "+baseItem.getCollectionType());
                                switch (baseItem.getCollectionType()) {
                                    case "movies":
                                    case "tvshows":
                                    case "music":
                                        TvApp.getApplication().getLogger().Debug("**** View Type Pref: "+response.getCustomPrefs().get("DefaultView"));
                                        if (ViewType.GRID.equals(response.getCustomPrefs().get("DefaultView"))) {
                                            // open grid browsing
                                            Intent folderIntent = new Intent(activity, GenericGridActivity.class);
                                            folderIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));
                                            activity.startActivity(folderIntent);

                                        } else {
                                            // open user view browsing
                                            Intent intent = new Intent(activity, UserViewActivity.class);
                                            intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));

                                            activity.startActivity(intent);

                                        }
                                        break;
                                    case "livetv":
                                        // open user view browsing
                                        Intent intent = new Intent(activity, UserViewActivity.class);
                                        intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));

                                        activity.startActivity(intent);
                                        break;
                                    default:
                                        // open generic folder browsing
                                        Intent folderIntent = new Intent(activity, GenericGridActivity.class);
                                        folderIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));
                                        activity.startActivity(folderIntent);
                                }

                            }
                        });
                        return;
                    case "Series":
                    case "MusicArtist":
                        //Start activity for details display
                        Intent intent = new Intent(activity, FullDetailsActivity.class);
                        intent.putExtra("ItemId", baseItem.getId());
                        if (noHistory) intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                        activity.startActivity(intent);

                        return;

                    case "MusicAlbum":
                    case "Playlist":
                        //Start activity for song list display
                        Intent songListIntent = new Intent(activity, SongListActivity.class);
                        songListIntent.putExtra("ItemId", baseItem.getId());
                        if (noHistory) songListIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                        activity.startActivity(songListIntent);

                        return;

                    case "Audio":
                        //produce item menu
                        KeyProcessor.HandleKey(KeyEvent.KEYCODE_MENU, rowItem, (BaseActivity) activity);
                        return;

                    case "Season":
                    case "RecordingGroup":
                        //Start activity for enhanced browse
                        Intent seasonIntent = new Intent(activity, GenericFolderActivity.class);
                        seasonIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));
                        if (noHistory) seasonIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                        activity.startActivity(seasonIntent);

                        return;

                    case "BoxSet":
                        // open collection browsing
                        Intent collectionIntent = new Intent(activity, CollectionActivity.class);
                        collectionIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));
                        if (noHistory) collectionIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                        activity.startActivity(collectionIntent);
                        return;

                    case "Photo":
                        // open photo player
                        MediaManager.setCurrentMediaPosition(pos);
                        Intent photoIntent = new Intent(activity, PhotoPlayerActivity.class);

                        activity.startActivity(photoIntent);
                        return;

                }

                // or generic handling
                if (baseItem.getIsFolder()) {
                    // open generic folder browsing - but need display prefs
                    TvApp.getApplication().getDisplayPrefsAsync(baseItem.getDisplayPreferencesId(), new Response<DisplayPreferences>() {
                        @Override
                        public void onResponse(DisplayPreferences response) {
                            Intent intent = new Intent(activity, GenericGridActivity.class);
                            intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));
                            if (noHistory) intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                            activity.startActivity(intent);

                        }
                    });
                } else {
                    switch (rowItem.getSelectAction()) {

                        case ShowDetails:
                            //Start details fragment for display and playback
                            Intent intent = new Intent(activity, FullDetailsActivity.class);
                            intent.putExtra("ItemId", baseItem.getId());
                            if (noHistory) intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            activity.startActivity(intent);
                            break;
                        case Play:
                            if (baseItem.getPlayAccess() == PlayAccess.Full) {
                                //Just play it directly
                                Utils.getItemsToPlay(baseItem, baseItem.getType().equals("Movie"), false, new Response<List<BaseItemDto>>() {
                                    @Override
                                    public void onResponse(List<BaseItemDto> response) {
                                        Intent intent = new Intent(activity, PlaybackOverlayActivity.class);
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
                //Start details fragment
                Intent intent = new Intent(activity, FullDetailsActivity.class);
                intent.putExtra("ItemId", rowItem.getPerson().getId());
                if (noHistory) intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                activity.startActivity(intent);

                break;
            case Chapter:
                final ChapterItemInfo chapter = rowItem.getChapterInfo();
                //Start playback of the item at the chapter point
                application.getApiClient().GetItemAsync(chapter.getItemId(), application.getCurrentUser().getId(), new Response<BaseItemDto>(){
                    @Override
                    public void onResponse(BaseItemDto response) {
                        String[] items = new String[1];
                        items[0] = application.getSerializer().SerializeToString(response);
                        Intent intent = new Intent(activity, PlaybackOverlayActivity.class);
                        intent.putExtra("Items", items);
                        Long start = chapter.getStartPositionTicks() / 10000;
                        intent.putExtra("Position", start.intValue());
                        activity.startActivity(intent);
                    }
                });
                break;
            case Server:
                //Log in to selected server
                ServerSignIn(application.getConnectionManager(), rowItem.getServerInfo(), activity);
                break;

            case User:
                final UserDto user = rowItem.getUser();
                if (user.getHasPassword()) {
                    Utils.processPasswordEntry(activity, user);

                } else {
                    Utils.loginUser(user.getName(), "", application.getLoginApiClient(), activity);
                }
                break;

            case SearchHint:
                final SearchHint hint = rowItem.getSearchHint();
                //Retrieve full item for display and playback
                application.getApiClient().GetItemAsync(hint.getItemId(), application.getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if ((response.getIsFolder() && !"Series".equals(response.getType())) || "MusicArtist".equals(response.getType())) {
                            // open generic folder browsing
                            Intent intent = new Intent(activity, GenericGridActivity.class);
                            intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(response));

                            activity.startActivity(intent);

                        } else {
                            Intent intent = new Intent(activity, FullDetailsActivity.class);
                            intent.putExtra("ItemId", response.getId());
                            if (response.getType().equals("Program")) {
                                intent.putExtra("ItemType", response.getType());
                                intent.putExtra("ChannelId", response.getChannelId());
                                intent.putExtra("ProgramInfo", TvApp.getApplication().getSerializer().SerializeToString(response));
                            }
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
                        //Start details fragment for display and playback
                        Intent programIntent = new Intent(activity, FullDetailsActivity.class);
                        programIntent.putExtra("ItemId", program.getId());
                        programIntent.putExtra("ItemType", program.getType());
                        programIntent.putExtra("ChannelId", program.getChannelId());
                        programIntent.putExtra("ProgramInfo", TvApp.getApplication().getSerializer().SerializeToString(program));

                            activity.startActivity(programIntent);
                        break;
                    case Play:
                        if (program.getPlayAccess() == PlayAccess.Full) {
                            //Just play it directly - need to retrieve program channel via items api to convert to BaseItem
                            TvApp.getApplication().getApiClient().GetItemAsync(program.getChannelId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    String[] items = new String[] {TvApp.getApplication().getSerializer().SerializeToString(response)};
                                    Intent intent = new Intent(activity, PlaybackOverlayActivity.class);
                                    intent.putExtra("Items", items);
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
                ChannelInfoDto channel = rowItem.getChannelInfo();
                TvApp.getApplication().getApiClient().GetItemAsync(channel.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        Utils.getItemsToPlay(response, false, false, new Response<List<BaseItemDto>>() {
                            @Override
                            public void onResponse(List<BaseItemDto> response) {
                                Intent intent = new Intent(activity, PlaybackOverlayActivity.class);
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
                        Intent recIntent = new Intent(activity, FullDetailsActivity.class);
                        recIntent.putExtra("ItemId", rowItem.getRecordingInfo().getId());

                        activity.startActivity(recIntent);
                        break;
                    case Play:
                        if (rowItem.getRecordingInfo().getPlayAccess() == PlayAccess.Full) {
                            //Just play it directly but need to retrieve as base item
                            TvApp.getApplication().getApiClient().GetItemAsync(rowItem.getRecordingInfo().getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                                @Override
                                public void onResponse(BaseItemDto response) {
                                    Intent intent = new Intent(activity, PlaybackOverlayActivity.class);
                                    String[] items = new String[] {TvApp.getApplication().getSerializer().SerializeToString(response)};
                                    intent.putExtra("Items", items);
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


                }
                break;


        }

    }

    public static void ServerSignIn(final IConnectionManager connectionManager, final ServerInfo serverInfo, final Activity activity) {
        //Connect to the selected server
        final DelayedMessage message = new DelayedMessage(activity);
        connectionManager.Connect(serverInfo, new Response<ConnectionResult>() {
            @Override
            public void onResponse(ConnectionResult response) {
                message.Cancel();
                switch (response.getState()) {
                    case Unavailable:
                        Utils.showToast(activity, "Server unavailable");
                        break;
                    case SignedIn:
                        if (serverInfo.getUserLinkType() != null) {
                            // go straight in for connect only
                            response.getApiClient().GetUserAsync(serverInfo.getUserId(), new Response<UserDto>() {
                                @Override
                                public void onResponse(UserDto response) {
                                    TvApp.getApplication().setCurrentUser(response);
                                    TvApp.getApplication().setConnectLogin(true);
                                    Intent homeIntent = new Intent(activity, MainActivity.class);
                                    activity.startActivity(homeIntent);
                                }
                            });
                            break;
                        }
                    case ServerSignIn:
                        //Set api client for login
                        TvApp.getApplication().setLoginApiClient(response.getApiClient());
                        //Open user selection
                        Intent userIntent = new Intent(activity, SelectUserActivity.class);
                        userIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        activity.startActivity(userIntent);
                        break;
                    case ConnectSignIn:
                    case ServerSelection:
                        Utils.showToast(activity, "Unexpected response from server connect: " + response.getState());
                        break;
                }
            }

            @Override
            public void onError(Exception exception) {
                message.Cancel();
                Utils.showToast(activity, "Error Signing in to server");
                exception.printStackTrace();
                Utils.reportError(activity, "Error Signing in to server");
            }
        });

    }
}
