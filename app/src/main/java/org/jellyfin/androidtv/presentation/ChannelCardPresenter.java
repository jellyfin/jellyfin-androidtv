package org.jellyfin.androidtv.presentation;

import android.content.Context;
import androidx.leanback.widget.Presenter;
import android.view.View;
import android.view.ViewGroup;

import org.jellyfin.androidtv.TvApp;

import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;

public class ChannelCardPresenter extends Presenter {

    private static ViewGroup mViewParent;


    public ChannelCardPresenter() { super();}

    private static Context getContext() {
        return TvApp.getApplication().getCurrentActivity() != null ? TvApp.getApplication().getCurrentActivity() : mViewParent.getContext();
    }

    static class ViewHolder extends Presenter.ViewHolder {
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
        //Log.d(TAG, "onCreateViewHolder");
        mViewParent = parent;

        MyChannelCardView view = new MyChannelCardView(getContext());

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
        //Log.d(TAG, "onUnbindViewHolder");
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
        //Log.d(TAG, "onViewAttachedToWindow");
    }

}
