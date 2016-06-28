package tv.emby.embyatv.presentation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.Presenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import mediabrowser.model.entities.MediaStream;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.ui.GridButton;

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
