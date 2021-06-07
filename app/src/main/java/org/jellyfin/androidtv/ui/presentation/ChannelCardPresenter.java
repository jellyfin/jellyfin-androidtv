package org.jellyfin.androidtv.ui.presentation;

import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import org.jellyfin.androidtv.ui.card.ChannelCardView;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;

public class ChannelCardPresenter extends Presenter {
    class ViewHolder extends Presenter.ViewHolder {
        private ChannelCardView mCardView;

        public ViewHolder(View view) {
            super(view);
            mCardView = (ChannelCardView) view;
        }

        public void setItem(ChannelInfoDto item) {
            mCardView.setItem(item);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ChannelCardView view = new ChannelCardView(parent.getContext());

        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        if (!(item instanceof ChannelInfoDto)) return;
        ChannelInfoDto channel = (ChannelInfoDto) item;

        ViewHolder vh = (ViewHolder) viewHolder;

        vh.setItem(channel);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // Unused
    }
}
