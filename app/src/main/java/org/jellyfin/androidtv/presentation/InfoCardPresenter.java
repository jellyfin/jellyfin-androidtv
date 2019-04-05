package org.jellyfin.androidtv.presentation;

import android.content.Context;
import android.support.v17.leanback.widget.Presenter;
import android.view.View;
import android.view.ViewGroup;

import org.jellyfin.androidtv.TvApp;

import org.jellyfin.apiclient.model.entities.MediaStream;

public class InfoCardPresenter extends Presenter {

    private static ViewGroup mViewParent;


    public InfoCardPresenter() { super();}

    private static Context getContext() {
        return TvApp.getApplication().getCurrentActivity() != null ? TvApp.getApplication().getCurrentActivity() : mViewParent.getContext();
    }

    static class ViewHolder extends Presenter.ViewHolder {
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
        //Log.d(TAG, "onCreateViewHolder");
        mViewParent = parent;

        MyInfoCardView infoView = new MyInfoCardView(getContext());

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

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
        //Log.d(TAG, "onViewAttachedToWindow");
    }

}
