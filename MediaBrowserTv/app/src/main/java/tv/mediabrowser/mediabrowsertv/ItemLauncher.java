package tv.mediabrowser.mediabrowsertv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.InputType;
import android.widget.EditText;

import java.util.Arrays;
import java.util.Collections;

import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.search.SearchHint;

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
                    //Start details fragment for display and playback
                    Intent intent = new Intent(activity, DetailsActivity.class);
                    intent.putExtra("ItemId", baseItem.getId());

                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            activity,
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                    activity.startActivity(intent, bundle);
                }
                break;
            case Person:
                //Start details fragment
                Intent intent = new Intent(activity, DetailsActivity.class);
                intent.putExtra("ItemId", rowItem.getPerson().getId());

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        activity,
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                activity.startActivity(intent, bundle);

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
                //Connect to the selected server
                final DelayedMessage message = new DelayedMessage(activity);
                application.getConnectionManager().Connect(rowItem.getServerInfo(), new Response<ConnectionResult>() {
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
                break;

            case User:
                final UserDto user = rowItem.getUser();
                if (user.getHasPassword()) {
                    Intent pwIntent = new Intent(activity, DpadPwActivity.class);
                    pwIntent.putExtra("User", application.getSerializer().SerializeToString(user));
                    pwIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    activity.startActivity(pwIntent);

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
                        if (response.getIsFolder()) {
                            // open generic folder browsing
                            Intent intent = new Intent(activity, GenericFolderActivity.class);
                            intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(response));

                            activity.startActivity(intent);

                        } else {
                            Intent intent = new Intent(activity, DetailsActivity.class);
                            intent.putExtra("BaseItemDto", TvApp.getApplication().getSerializer().SerializeToString(response));

                            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    activity,
                                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                    DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                            activity.startActivity(intent, bundle);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        application.getLogger().ErrorException("Error retrieving full object", exception);
                        exception.printStackTrace();
                    }
                });

        }

    }
}
