package org.jellyfin.androidtv.ui.presentation;

import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.Presenter;

import org.jellyfin.apiclient.model.entities.MediaStream;

public class InfoCardPresenter extends Presenter {
    class ViewHolder extends Presenter.ViewHolder {
        private MyInfoCardView mInfoCardView;

        public ViewHolder(View view) {
            super(view);
            mInfoCardView = (MyInfoCardView) view;

        }

        public void setItem(MediaStream ms) {
            mInfoCardView.setItem(ms);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        MyInfoCardView infoView = new MyInfoCardView(parent.getContext());

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
