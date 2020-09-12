package org.jellyfin.androidtv.ui.presentation;

import android.app.Activity;
import android.graphics.Paint;
import androidx.leanback.widget.Presenter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jellyfin.androidtv.util.Utils;

public abstract class MyAbstractDetailsPresenter extends Presenter {

    public static class ViewHolder extends Presenter.ViewHolder {
        private final TextView mTitle;
        private final TextView mSubtitle;
        private final LinearLayout mInfoRow;
        private final TextView mBody;
        private final int mTitleMargin;
        private final int mUnderTitleBaselineMargin;
        private final int mUnderSubtitleBaselineMargin;
        private final int mTitleLineSpacing;
        private final int mBodyLineSpacing;
        private final int mBodyMaxLines;
        private final int mBodyMinLines;
        private final Activity mActivity;
        private final Paint.FontMetricsInt mTitleFontMetricsInt;
        private final Paint.FontMetricsInt mSubtitleFontMetricsInt;
        private final Paint.FontMetricsInt mBodyFontMetricsInt;

        public ViewHolder(View view) {
            super(view);
            mTitle = (TextView) view.findViewById(androidx.leanback.R.id.lb_details_description_title);
            mSubtitle = (TextView) view.findViewById(androidx.leanback.R.id.lb_details_description_subtitle);
            mActivity = (Activity) view.getContext();
            mInfoRow = new LinearLayout(mActivity);
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(Utils.convertDpToPixel(mActivity, 500), Utils.convertDpToPixel(mActivity,20));
            layout.setMargins(0,15,0,0);
            mInfoRow.setLayoutParams(layout);
            //Replace subtitle with our info row
            ViewGroup parent = (ViewGroup) view;
            int index = parent.indexOfChild(mSubtitle);
            parent.removeView(mSubtitle);
            parent.addView(mInfoRow, index);
            mBody = (TextView) view.findViewById(androidx.leanback.R.id.lb_details_description_body);

            Paint.FontMetricsInt titleFontMetricsInt = getFontMetricsInt(mTitle);
            final int titleAscent = view.getResources().getDimensionPixelSize(
                    androidx.leanback.R.dimen.lb_details_description_title_baseline);
            // Ascent is negative
            mTitleMargin = titleAscent + titleFontMetricsInt.ascent;

            mUnderTitleBaselineMargin = view.getResources().getDimensionPixelSize(
                    androidx.leanback.R.dimen.lb_details_description_under_title_baseline_margin);
            mUnderSubtitleBaselineMargin = view.getResources().getDimensionPixelSize(
                    androidx.leanback.R.dimen.lb_details_description_under_subtitle_baseline_margin);

            mTitleLineSpacing = view.getResources().getDimensionPixelSize(
                    androidx.leanback.R.dimen.lb_details_description_title_line_spacing);
            mBodyLineSpacing = view.getResources().getDimensionPixelSize(
                    androidx.leanback.R.dimen.lb_details_description_body_line_spacing);

            mBodyMaxLines = 6;
            mBodyMinLines = 4;

            mTitleFontMetricsInt = getFontMetricsInt(mTitle);
            mSubtitleFontMetricsInt = getFontMetricsInt(mSubtitle);
            mBodyFontMetricsInt = getFontMetricsInt(mBody);

            mTitle.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    mBody.setMaxLines(mTitle.getLineCount() > 1 ? mBodyMinLines : mBodyMaxLines);
                }
            });
        }

        public TextView getTitle() {
            return mTitle;
        }

        public LinearLayout getInfoRow() { return mInfoRow; }

        public Activity getActivity() { return mActivity; }

        public TextView getSubtitle() {
            return mSubtitle;
        }

        public TextView getBody() {
            return mBody;
        }

        private Paint.FontMetricsInt getFontMetricsInt(TextView textView) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(textView.getTextSize());
            paint.setTypeface(textView.getTypeface());
            return paint.getFontMetricsInt();
        }
    }

    @Override
    public final ViewHolder onCreateViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(androidx.leanback.R.layout.lb_details_description, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public final void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ViewHolder vh = (ViewHolder) viewHolder;
        onBindDescription(vh, item);

        boolean hasTitle = true;
        if (TextUtils.isEmpty(vh.mTitle.getText())) {
            vh.mTitle.setVisibility(View.GONE);
            hasTitle = false;
        } else {
            vh.mTitle.setVisibility(View.VISIBLE);
            vh.mTitle.setLineSpacing(vh.mTitleLineSpacing - vh.mTitle.getLineHeight() +
                    vh.mTitle.getLineSpacingExtra(), vh.mTitle.getLineSpacingMultiplier());
        }
        setTopMargin(vh.mTitle, vh.mTitleMargin);

        boolean hasSubtitle = true;
        if (TextUtils.isEmpty(vh.mSubtitle.getText())) {
            vh.mSubtitle.setVisibility(View.GONE);
            hasSubtitle = false;
        } else {
            vh.mSubtitle.setVisibility(View.VISIBLE);
            if (hasTitle) {
                setTopMargin(vh.mSubtitle, vh.mUnderTitleBaselineMargin +
                        vh.mSubtitleFontMetricsInt.ascent - vh.mTitleFontMetricsInt.descent);
            } else {
                setTopMargin(vh.mSubtitle, 0);
            }
        }

        if (TextUtils.isEmpty(vh.mBody.getText())) {
            vh.mBody.setVisibility(View.GONE);
        } else {
            vh.mBody.setVisibility(View.VISIBLE);
            vh.mBody.setLineSpacing(vh.mBodyLineSpacing - vh.mBody.getLineHeight() +
                    vh.mBody.getLineSpacingExtra(), vh.mBody.getLineSpacingMultiplier());

            if (hasSubtitle) {
                setTopMargin(vh.mBody, vh.mUnderSubtitleBaselineMargin +
                        vh.mBodyFontMetricsInt.ascent - vh.mSubtitleFontMetricsInt.descent);
            } else if (hasTitle) {
                setTopMargin(vh.mBody, vh.mUnderTitleBaselineMargin +
                        vh.mBodyFontMetricsInt.ascent - vh.mTitleFontMetricsInt.descent);
            } else {
                setTopMargin(vh.mBody, 0);
            }
        }
    }

    /**
     * Binds the data from the item referenced in the DetailsOverviewRow to the
     * ViewHolder.
     *
     * @param vh The ViewHolder for this details description view.
     * @param item The item from the DetailsOverviewRow being presented.
     */
    protected abstract void onBindDescription(ViewHolder vh, Object item);

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {}

    private void setTopMargin(TextView textView, int topMargin) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
        lp.topMargin = topMargin;
        textView.setLayoutParams(lp);
    }
}
