package tv.emby.embyatv.itemhandling;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.app.ActivityOptionsCompat;

import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.search.SearchHint;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.browsing.CollectionActivity;
import tv.emby.embyatv.browsing.GenericFolderActivity;
import tv.emby.embyatv.browsing.UserViewActivity;
import tv.emby.embyatv.details.DetailsActivity;
import tv.emby.embyatv.model.ChapterItemInfo;
import tv.emby.embyatv.playback.PlaybackOverlayActivity;
import tv.emby.embyatv.startup.DpadPwActivity;
import tv.emby.embyatv.startup.SelectUserActivity;
import tv.emby.embyatv.util.DelayedMessage;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 12/21/2014.
 */
public class ItemLauncher {
    public static void launch(BaseRowItem rowItem, final TvApp application, final Activity activity, final Presenter.ViewHolder itemViewHolder) {
        switch (rowItem.getItemType()) {

            case BaseItem:
                final BaseItemDto baseItem = rowItem.getBaseItem();
                TvApp.getApplication().getLogger().Debug("Item selected: " + rowItem.getIndex() + " - " + baseItem.getName());

                //specialized type handling
                switch (baseItem.getType()) {
                    case "UserView":
                        switch (baseItem.getCollectionType()) {
                            case "movies":
                            case "tvshows":
                            case "music":
                            case "livetv":
                                // open user view browsing
                                Intent intent = new Intent(activity, UserViewActivity.class);
                                intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));

                                activity.startActivity(intent);
                                break;
                            default:
                                // open generic folder browsing
                                Intent folderIntent = new Intent(activity, GenericFolderActivity.class);
                                folderIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));
                                activity.startActivity(folderIntent);
                        }
                        return;
                    case "Series":
                        //Start activity for details display
                        Intent intent = new Intent(activity, DetailsActivity.class);
                        intent.putExtra("ItemId", baseItem.getId());

                        activity.startActivity(intent);

                        return;

                    case "BoxSet":
                        // open collection browsing
                        Intent collectionIntent = new Intent(activity, CollectionActivity.class);
                        collectionIntent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));

                        activity.startActivity(collectionIntent);
                        return;

                }

                // or generic handling
                if (baseItem.getIsFolder()) {
                    // open generic folder browsing
                    Intent intent = new Intent(activity, GenericFolderActivity.class);
                    intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));

                    activity.startActivity(intent);
                } else {
                    switch (rowItem.getSelectAction()) {

                        case ShowDetails:
                            //Start details fragment for display and playback
                            Intent intent = new Intent(activity, DetailsActivity.class);
                            intent.putExtra("ItemId", baseItem.getId());
                            if (baseItem.getHasPrimaryImage()) {
                                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                        activity,
                                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                                activity.startActivity(intent, bundle);
                            } else {
                                activity.startActivity(intent);
                            }
                            break;
                        case Play:
                            if (baseItem.getPlayAccess() == PlayAccess.Full) {
                                //Just play it directly
                                Utils.getItemsToPlay(baseItem, baseItem.getType().equals("Movie"), new Response<String[]>() {
                                    @Override
                                    public void onResponse(String[] response) {
                                        Intent intent = new Intent(activity, PlaybackOverlayActivity.class);
                                        intent.putExtra("Items", response);
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
                Intent intent = new Intent(activity, DetailsActivity.class);
                intent.putExtra("ItemId", rowItem.getPerson().getId());

                if (rowItem.getPerson().getHasPrimaryImage()) {
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            activity,
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                    activity.startActivity(intent, bundle);
                } else {
                    activity.startActivity(intent);
                }

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
                        if (response.getIsFolder() && !"Series".equals(response.getType())) {
                            // open generic folder browsing
                            Intent intent = new Intent(activity, GenericFolderActivity.class);
                            intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(response));

                            activity.startActivity(intent);

                        } else {
                            Intent intent = new Intent(activity, DetailsActivity.class);
                            intent.putExtra("ItemId", response.getId());
                            if (response.getHasPrimaryImage()) {
                                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                        activity,
                                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                                activity.startActivity(intent, bundle);
                            } else {
                                activity.startActivity(intent);
                            }
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
                ProgramInfoDto program = rowItem.getProgramInfo();
                switch (rowItem.getSelectAction()) {

                    case ShowDetails:
                        //Start details fragment for display and playback
                        Intent programIntent = new Intent(activity, DetailsActivity.class);
                        programIntent.putExtra("ItemId", program.getId());
                        programIntent.putExtra("ItemType", program.getType());
                        programIntent.putExtra("ChannelId", program.getChannelId());
                        programIntent.putExtra("ProgramInfo", TvApp.getApplication().getSerializer().SerializeToString(program));

                        if (program.getHasPrimaryImage()) {
                            Bundle programBundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    activity,
                                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                    DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                            activity.startActivity(programIntent, programBundle);
                        } else {
                            activity.startActivity(programIntent);
                        }
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
                        String[] items = new String[] {TvApp.getApplication().getSerializer().SerializeToString(response)};
                        Intent intent = new Intent(activity, PlaybackOverlayActivity.class);
                        intent.putExtra("Items", items);
                        intent.putExtra("Position", 0);
                        activity.startActivity(intent);
                    }
                });
                break;

            case LiveTvRecording:
                switch (rowItem.getSelectAction()) {

                    case ShowDetails:
                        //Start details fragment for display and playback
                        Intent recIntent = new Intent(activity, DetailsActivity.class);
                        recIntent.putExtra("ItemId", rowItem.getRecordingInfo().getId());

                        Bundle recBundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                activity,
                                ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                        activity.startActivity(recIntent, recBundle);
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



        }

    }

    public static void ServerSignIn(IConnectionManager connectionManager, ServerInfo serverInfo, final Activity activity) {
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
                    case SignedIn: // never allow default "remember user"
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
