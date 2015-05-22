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
import android.widget.TextView;

import tv.emby.embyatv.R;
import tv.emby.embyatv.details.MyDetailsOverviewRow;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 5/22/2015.
 */
public class MyDetailsOverviewRowPresenter extends RowPresenter {

    private ViewHolder viewHolder;

    public final class ViewHolder extends RowPresenter.ViewHolder {
        private int BUTTON_SIZE;

        private ImageView mPoster;
        private TextView mButtonHelp;
        private TextView mLastPlayedText;
        private TextView mSummaryTitle;
        private TextView mTimeLine;
        private TextView mSummary;
        private LinearLayout mButtonRow;

        private Typeface roboto;

        /**
         * Constructor for ViewHolder.
         *
         * @param rootView The View bound to the Row.
         */
        public ViewHolder(View rootView) {
            super(rootView);
            BUTTON_SIZE = Utils.convertDpToPixel(rootView.getContext(), 35);
            roboto = Typeface.createFromAsset(rootView.getContext().getAssets(), "fonts/Roboto-Light.ttf");

            mPoster = (ImageView) rootView.findViewById(R.id.fdPoster);
            mButtonHelp = (TextView) rootView.findViewById(R.id.fdButtonHelp);
            mLastPlayedText = (TextView) rootView.findViewById(R.id.fdLastPlayedText);
            mButtonRow = (LinearLayout) rootView.findViewById(R.id.fdButtonRow);
            mSummaryTitle = (TextView) rootView.findViewById(R.id.fdSummaryTitle);
            mTimeLine = (TextView) rootView.findViewById(R.id.fdSummarySubTitle);
            mLastPlayedText.setTypeface(roboto);
            mSummary = (TextView) rootView.findViewById(R.id.fdSummaryText);
            mSummary.setTypeface(roboto);

            mButtonRow.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) mButtonHelp.setText("");
                }
            });


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
        vh.mSummary.setText(row.getSummary());
        vh.mSummaryTitle.setText(row.getSummaryTitle());
        switch (row.getItem().getType()) {
            case "Person":
                vh.mSummary.setX(vh.mSummaryTitle.getX());
                vh.mSummary.setY(vh.mSummaryTitle.getY()+10);
                vh.mSummary.setHeight(vh.mPoster.getHeight()-20);
                vh.mSummaryTitle.setVisibility(View.GONE);
                vh.mTimeLine.setVisibility(View.GONE);

                break;

            default:


        }

        vh.mTimeLine.setText(row.getSummarySubTitle());

    }

    public void updateEndTime(String text) {
        viewHolder.mTimeLine.setText(text);
    }
}
