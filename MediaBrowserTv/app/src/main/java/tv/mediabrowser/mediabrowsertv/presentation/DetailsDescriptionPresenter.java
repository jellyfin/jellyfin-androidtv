package tv.mediabrowser.mediabrowsertv.presentation;

import mediabrowser.model.dto.BaseItemDto;
import tv.mediabrowser.mediabrowsertv.util.Utils;

public class DetailsDescriptionPresenter extends MyAbstractDetailsPresenter {
    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        BaseItemDto baseItem = (BaseItemDto) item;

        if (baseItem != null) {
            viewHolder.getTitle().setText(baseItem.getName());
            viewHolder.getSubtitle().setText(Utils.getInfoRow(baseItem));
            viewHolder.getBody().setText(baseItem.getOverview());
            viewHolder.getBody().setMaxLines(4);
        }
    }
}
