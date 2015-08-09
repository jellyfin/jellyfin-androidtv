package tv.emby.embyatv.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.util.InfoLayoutHelper;

/**
 * Created by Eric on 7/27/2015.
 */
public class ItemPanel extends RelativeLayout {

    private TextView title;
    private LinearLayout infoRow;
    private TextView summary;

    public ItemPanel(Context context) {
        super(context);
        init(context);
    }

    public ItemPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.item_detail_panel, null, false);
        this.addView(v);
        if (!isInEditMode()) {
            title = (TextView) v.findViewById(R.id.title);
            infoRow = (LinearLayout) v.findViewById(R.id.infoRow);
            summary = (TextView) v.findViewById(R.id.summary);
            Typeface roboto = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
            title.setTypeface(roboto);
            summary.setTypeface(roboto);
        }
    }

    public void setItem(BaseRowItem item) {
        if (item != null) {
            title.setText(item.getFullName() + (item.getItemType() == BaseRowItem.ItemType.BaseItem && "Episode".equals(item.getBaseItem().getType()) ? " - " + item.getName() : ""));
            if (TvApp.getApplication().getCurrentActivity() != null) InfoLayoutHelper.addInfoRow(TvApp.getApplication().getCurrentActivity(), item, infoRow, true, true);
            summary.setText(item.getSummary());
        }
    }

}
