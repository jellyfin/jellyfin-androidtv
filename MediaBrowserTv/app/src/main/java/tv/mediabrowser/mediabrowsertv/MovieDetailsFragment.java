package tv.mediabrowser.mediabrowsertv;

import android.support.v17.leanback.widget.ArrayObjectAdapter;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.SimilarItemsQuery;

/**
 * Created by Eric on 12/19/2014.
 */
public class MovieDetailsFragment extends BaseItemDetailsFragment {

    @Override
    protected void addAdditionalRows(final ArrayObjectAdapter adapter) {
        SimilarItemsQuery similar = new SimilarItemsQuery();
        similar.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
        similar.setUserId(TvApp.getApplication().getCurrentUser().getId());
        similar.setId(mBaseItem.getId());
        similar.setLimit(10);

        switch (mBaseItem.getType()) {
            case "Movie":
                mApiClient.GetSimilarMoviesAsync(similar, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        addRow(adapter, "Similar Items", response.getItems());
                    }
                });
        }

    }
}
