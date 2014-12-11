package tv.mediabrowser.mediabrowsertv;

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import mediabrowser.model.dto.BaseItemDto;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {
    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        BaseItemDto baseItem = (BaseItemDto) item;

        if (baseItem != null) {
            viewHolder.getTitle().setText(baseItem.getName());
            viewHolder.getSubtitle().setText(baseItem.getOfficialRating());
            viewHolder.getBody().setMaxLines(4);
            viewHolder.getBody().setText(baseItem.getOverview());
        }
    }
}
