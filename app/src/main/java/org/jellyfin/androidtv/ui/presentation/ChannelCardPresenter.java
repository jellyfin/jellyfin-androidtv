package org.jellyfin.androidtv.ui.presentation;

import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;

public class ChannelCardPresenter extends Presenter {
    class ViewHolder extends Presenter.ViewHolder {
        private MyChannelCardView mCardView;

        public ViewHolder(View view) {
            super(view);
            mCardView = (MyChannelCardView) view;
        }

        public void setItem(ChannelInfoDto item) {
            mCardView.setItem(item);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        MyChannelCardView view = new MyChannelCardView(parent.getContext());

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
