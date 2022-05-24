package org.jellyfin.androidtv.ui.presentation;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.leanback.widget.RowPresenter;

import org.jellyfin.androidtv.data.model.InfoItem;
import org.jellyfin.androidtv.databinding.ViewRowDetailsBinding;
import org.jellyfin.androidtv.ui.AsyncImageView;
import org.jellyfin.androidtv.ui.DetailRowView;
import org.jellyfin.androidtv.ui.TextUnderButton;
import org.jellyfin.androidtv.ui.itemdetail.MyDetailsOverviewRow;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.MarkdownRenderer;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

public class MyDetailsOverviewRowPresenter extends RowPresenter {
    private final MarkdownRenderer markdownRenderer;
    private ViewHolder viewHolder;

    public MyDetailsOverviewRowPresenter(MarkdownRenderer markdownRenderer) {
        super();

        this.markdownRenderer = markdownRenderer;

        // Don't call setActivated() on views
        setSyncActivatePolicy(SYNC_ACTIVATED_CUSTOM);
    }

    public final class ViewHolder extends RowPresenter.ViewHolder {
        private TextView mGenreRow;
        private LinearLayout mInfoRow;
        private TextView mTitle;
        private AsyncImageView mPoster;
        private TextView mSummary;
        private LinearLayout mButtonRow;
        private ProgressBar mProgress;

        private TextView mInfoTitle1;
        private TextView mInfoTitle2;
        private TextView mInfoTitle3;
        private TextView mInfoValue1;
        private TextView mInfoValue2;
        private TextView mInfoValue3;

        private RelativeLayout mLeftFrame;

        /**
         * Constructor for ViewHolder.
         *
         * @param rootView The View bound to the Row.
         * @param binding
         */
        public ViewHolder(DetailRowView view) {
            super(view);

            ViewRowDetailsBinding binding = view.getBinding();

            mTitle = binding.fdTitle;
            mInfoTitle1 = binding.infoTitle1;
            mInfoTitle2 = binding.infoTitle2;
            mInfoTitle3 = binding.infoTitle3;
            mInfoValue1 = binding.infoValue1;
            mInfoValue2 = binding.infoValue2;
            mInfoValue3 = binding.infoValue3;

            mLeftFrame = binding.leftFrame;

            mGenreRow = binding.fdGenreRow;
            mInfoRow = binding.fdMainInfoRow;
            mPoster = binding.mainImage;
            mProgress = binding.fdProgress;
            mButtonRow = binding.fdButtonRow;
            mSummary = binding.fdSummaryText;
        }

        public void collapseLeftFrame() {
            ViewGroup.LayoutParams params = mLeftFrame.getLayoutParams();
            params.width = Utils.convertDpToPixel(view.getContext(), 100);
        }
    }

    @Override
    protected ViewHolder createRowViewHolder(ViewGroup parent) {
        DetailRowView view = new DetailRowView(parent.getContext());
        viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);

        MyDetailsOverviewRow row = (MyDetailsOverviewRow) item;
        ViewHolder vh = (ViewHolder) holder;

        setTitle(row.getItem().getName());
        InfoLayoutHelper.addInfoRow(holder.view.getContext(), row.getItem(), vh.mInfoRow, false, false);
        addGenres(vh.mGenreRow, row.getItem());
        setInfo1(row.getInfoItem1());
        setInfo2(row.getInfoItem2());
        setInfo3(row.getInfoItem3());

        String posterUrl = row.getImageDrawable();
        vh.mPoster.load(posterUrl, null, null, 1.0, 0);
        int progress = row.getProgress();
        if (progress > 0 && posterUrl != null) {
            vh.mProgress.setProgress(progress);
            vh.mProgress.setVisibility(View.VISIBLE);
        }

        String summaryRaw = row.getSummary();
        if (summaryRaw != null)
            vh.mSummary.setText(markdownRenderer.toMarkdownSpanned(summaryRaw));

        switch (row.getItem().getBaseItemType()) {
            case Person:
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) vh.mSummary.getLayoutParams();
                params.topMargin = 10;
                params.height = Utils.convertDpToPixel(vh.view.getContext(), 185);
                vh.mSummary.setMaxLines(9);
                vh.mGenreRow.setVisibility(View.GONE);
                vh.mInfoRow.setVisibility(View.GONE);
                vh.collapseLeftFrame();

                break;
        }

        vh.mButtonRow.removeAllViews();
        for (TextUnderButton button : row.getActions()) {
            vh.mButtonRow.addView(button);
        }
    }

    private void addGenres(TextView textView, BaseItemDto item) {
        textView.setText(TextUtils.join(" / ", item.getGenres()));
    }

    public void setTitle(String text) {
        viewHolder.mTitle.setText(text);
        if (text.length() > 28) {
            // raise it up a bit
            ((RelativeLayout.LayoutParams) viewHolder.mTitle.getLayoutParams()).topMargin = Utils.convertDpToPixel(viewHolder.view.getContext(), 55);
        }
    }

    public void setInfo1(InfoItem info) {
        if (info == null) {
            viewHolder.mInfoTitle1.setText("");
            viewHolder.mInfoValue1.setText("");
        } else {
            viewHolder.mInfoTitle1.setText(info.getLabel());
            viewHolder.mInfoValue1.setText(info.getValue());
        }
    }

    public void setInfo2(InfoItem info) {
        if (info == null) {
            viewHolder.mInfoTitle2.setText("");
            viewHolder.mInfoValue2.setText("");
        } else {
            viewHolder.mInfoTitle2.setText(info.getLabel());
            viewHolder.mInfoValue2.setText(info.getValue());
        }
    }

    public void setInfo3(InfoItem info) {
        if (info == null) {
            viewHolder.mInfoTitle3.setText("");
            viewHolder.mInfoValue3.setText("");
        } else {
            viewHolder.mInfoTitle3.setText(info.getLabel());
            viewHolder.mInfoValue3.setText(info.getValue());
        }
    }

    public TextView getSummaryView() {
        return viewHolder.mSummary;
    }

    public void updateEndTime(String text) {
        if (viewHolder != null && viewHolder.mInfoValue3 != null)
            viewHolder.mInfoValue3.setText(text);
    }

    @Override
    protected void onSelectLevelChanged(RowPresenter.ViewHolder holder) {
        // Do nothing - this removes the shadow on the out of focus rows of image cards
    }
}
