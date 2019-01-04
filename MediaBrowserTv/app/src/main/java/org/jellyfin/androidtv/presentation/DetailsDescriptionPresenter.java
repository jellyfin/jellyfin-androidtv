package org.jellyfin.androidtv.presentation;

import mediabrowser.model.dto.BaseItemDto;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.Utils;

public class DetailsDescriptionPresenter extends MyAbstractDetailsPresenter {
    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        BaseItemDto baseItem = (BaseItemDto) item;

        if (baseItem != null) {
            viewHolder.getTitle().setText(baseItem.getName());
            InfoLayoutHelper.addInfoRow(viewHolder.getActivity(), baseItem, viewHolder.getInfoRow(), true, true);
            viewHolder.getBody().setText(baseItem.getOverview());
        }
    }
}
