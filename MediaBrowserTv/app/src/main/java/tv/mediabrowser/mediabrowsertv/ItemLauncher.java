package tv.mediabrowser.mediabrowsertv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.app.ActivityOptionsCompat;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;

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
                        // open user view browsing
                        Intent intent = new Intent(activity, UserViewActivity.class);
                        intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));

                        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                activity,
                                ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                        activity.startActivity(intent, bundle);
                        return;
                    case "Series":
                        //Retrieve series for details display
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
                        return;

                }

                // or generic handling
                if (baseItem.getIsFolder()) {
                    // open generic folder browsing
                    Intent intent = new Intent(activity, GenericFolderActivity.class);
                    intent.putExtra("Folder", TvApp.getApplication().getSerializer().SerializeToString(baseItem));

                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            activity,
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                    activity.startActivity(intent, bundle);
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
                //Open user selection
                
                break;
        }

    }
}
