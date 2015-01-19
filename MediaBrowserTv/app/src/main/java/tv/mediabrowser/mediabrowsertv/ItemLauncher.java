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

import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserDto;

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
                        //Retrieve series for details display
                        application.getApiClient().GetItemAsync(baseItem.getId(), application.getCurrentUser().getId(), new Response<BaseItemDto>() {
                            @Override
                            public void onResponse(BaseItemDto response) {
                                Intent intent = new Intent(activity, DetailsActivity.class);
                                intent.putExtra("BaseItemDto", TvApp.getApplication().getSerializer().SerializeToString(response));

                                activity.startActivity(intent);

                            }

                            @Override
                            public void onError(Exception exception) {
                                application.getLogger().ErrorException("Error retrieving full object", exception);
                                exception.printStackTrace();
                            }
                        });
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
                    //Retrieve full item for display and playback
                    application.getApiClient().GetItemAsync(baseItem.getId(), application.getCurrentUser().getId(), new Response<BaseItemDto>() {
                        @Override
                        public void onResponse(BaseItemDto response) {
                            Intent intent = new Intent(activity, DetailsActivity.class);
                            intent.putExtra("BaseItemDto", TvApp.getApplication().getSerializer().SerializeToString(response));

                            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    activity,
                                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                    DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                            activity.startActivity(intent, bundle);

                        }

                        @Override
                        public void onError(Exception exception) {
                            application.getLogger().ErrorException("Error retrieving full object", exception);
                            exception.printStackTrace();
                        }
                    });
                }
                break;
            case Person:
                //Retrieve full item for display
                application.getApiClient().GetItemAsync(rowItem.getPerson().getId(), application.getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        Intent intent = new Intent(activity, DetailsActivity.class);
                        intent.putExtra("BaseItemDto", TvApp.getApplication().getSerializer().SerializeToString(response));

                        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                activity,
                                ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                        activity.startActivity(intent, bundle);

                    }

                    @Override
                    public void onError(Exception exception) {
                        application.getLogger().ErrorException("Error retrieving full object", exception);
                        exception.printStackTrace();
                    }
                });
                break;
            case Chapter:
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
                                //todo Check for saved user credentials and use them
//                                if ([has saved user]){
//                                  load saved user info
//                                response.getApiClient().GetUserAsync(response.getApiClient().getCurrentUserId(), new Response<UserDto>() {
//                                    @Override
//                                    public void onResponse(UserDto response) {
//                                        application.setCurrentUser(response);
//                                        Intent intent = new Intent(activity, MainActivity.class);
//                                        activity.startActivity(intent);
//                                    }
//                                });
//                            } else

                                //Set api client for login
                                TvApp.getApplication().setLoginApiClient(response.getApiClient());
                                //Open user selection
                                Intent userIntent = new Intent(activity, SelectUserActivity.class);
                                userIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                activity.startActivity(userIntent);
                                break;
                            case ConnectSignIn:
                            case ServerSelection:
                                Utils.showToast(activity, "Unexpected response from server connect: "+response.getState());
                                break;
                        }
                    }
                });
                break;

            case User:
                final UserDto user = rowItem.getUser();
                if (user.getHasPassword()) {
                    final EditText password = new EditText(activity);
                    password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    new AlertDialog.Builder(activity)
                            .setTitle("Enter Password")
                            .setMessage("Please enter password for " + user.getName())
                            .setView(password)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String pw = password.getText().toString();
                                    Utils.loginUser(user.getName(), pw, application.getLoginApiClient(), activity);
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Do nothing.
                                }
                            }).show();

                } else {
                    Utils.loginUser(user.getName(), "", application.getLoginApiClient(), activity);
                }
        }

    }
}
