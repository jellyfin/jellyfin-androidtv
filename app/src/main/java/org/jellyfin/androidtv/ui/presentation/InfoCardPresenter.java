package org.jellyfin.androidtv.ui.presentation;

import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import org.jellyfin.androidtv.ui.card.MediaInfoCardView;
import org.jellyfin.apiclient.model.entities.MediaStream;

public class InfoCardPresenter extends Presenter {
    class ViewHolder extends Presenter.ViewHolder {
        private MediaInfoCardView mInfoCardView;

        public ViewHolder(View view) {
            super(view);
            mInfoCardView = (MediaInfoCardView) view;

        }

        public void setItem(MediaStream ms) {
            mInfoCardView.setMediaStream(ms);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        MediaInfoCardView infoView = new MediaInfoCardView(parent.getContext());

        infoView.setFocusable(true);
        infoView.setFocusableInTouchMode(true);
        return new ViewHolder(infoView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        if (!(item instanceof MediaStream)) return;
        MediaStream mediaItem = (MediaStream) item;

        ViewHolder vh = (ViewHolder) viewHolder;

        vh.setItem(mediaItem);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        //Log.d(TAG, "onUnbindViewHolder");
    }
}
