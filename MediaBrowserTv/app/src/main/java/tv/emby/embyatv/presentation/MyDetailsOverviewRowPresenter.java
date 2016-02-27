package tv.emby.embyatv.presentation;

import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.details.MyDetailsOverviewRow;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 5/22/2015.
 */
public class MyDetailsOverviewRowPresenter extends RowPresenter {

    private ViewHolder viewHolder;

    public final class ViewHolder extends RowPresenter.ViewHolder {
        private ImageView mPoster;
        private TextView mButtonHelp;
        private TextView mLastPlayedText;
        private TextView mSummaryTitle;
        private TextView mTimeLine;
        private TextView mSummary;
        private LinearLayout mButtonRow;
        private ImageView mStudioImage;

        private Typeface roboto;

        /**
         * Constructor for ViewHolder.
         *
         * @param rootView The View bound to the Row.
         */
        public ViewHolder(View rootView) {
            super(rootView);
            roboto = TvApp.getApplication().getDefaultFont();

            mPoster = (ImageView) rootView.findViewById(R.id.fdPoster);
            mStudioImage = (ImageView) rootView.findViewById(R.id.studioImage);
            mButtonHelp = (TextView) rootView.findViewById(R.id.fdButtonHelp);
            mLastPlayedText = (TextView) rootView.findViewById(R.id.fdLastPlayedText);
            mButtonRow = (LinearLayout) rootView.findViewById(R.id.fdButtonRow);
            mSummaryTitle = (TextView) rootView.findViewById(R.id.fdSummaryTitle);
            mTimeLine = (TextView) rootView.findViewById(R.id.fdSummarySubTitle);
            mLastPlayedText.setTypeface(roboto);
            mSummary = (TextView) rootView.findViewById(R.id.fdSummaryText);
            mSummary.setTypeface(roboto);

        }

    }

    @Override
    protected ViewHolder createRowViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.details_overview_row, parent, false);
        viewHolder = new ViewHolder(v);

        return viewHolder;
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);

        MyDetailsOverviewRow row = (MyDetailsOverviewRow) item;
        ViewHolder vh = (ViewHolder) holder;

        vh.mPoster.setImageDrawable(row.getImageDrawable());
        vh.mStudioImage.setImageDrawable(row.getStudioDrawable());
        vh.mSummary.setText(row.getSummary());
        vh.mSummaryTitle.setText(row.getSummaryTitle());
        switch (row.getItem().getType()) {
            case "Person":
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) vh.mSummary.getLayoutParams();
                params.topMargin = 10;
                vh.mSummary.setHeight(Utils.convertDpToPixel(TvApp.getApplication(), 235));
                vh.mSummary.setMaxLines(12);
                vh.mSummaryTitle.setVisibility(View.GONE);
                vh.mTimeLine.setVisibility(View.GONE);

                break;
            case "MusicArtist":
                RelativeLayout.LayoutParams artistParams = (RelativeLayout.LayoutParams) vh.mSummary.getLayoutParams();
                artistParams.topMargin = 20;
                vh.mSummary.setHeight(Utils.convertDpToPixel(TvApp.getApplication(), 235));
                vh.mSummary.setMaxLines(12);
                vh.mSummaryTitle.setVisibility(View.GONE);
                vh.mTimeLine.setVisibility(View.GONE);

                break;
        }

        vh.mButtonRow.removeAllViews();
        for (ImageButton button : row.getActions()) {
            button.setHelpView(vh.mButtonHelp);
            vh.mButtonRow.addView(button);
        }

        vh.mTimeLine.setText(row.getSummarySubTitle());

    }

    public LinearLayout getButtonRow() { return viewHolder.mButtonRow; }
    public TextView getButtonHelpView() { return viewHolder.mButtonHelp; }
    public ImageView getPosterView() { return viewHolder.mPoster; }

    public void updateEndTime(String text) {
        if (viewHolder != null && viewHolder.mTimeLine != null) viewHolder.mTimeLine.setText(text);
    }
}
