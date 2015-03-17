package tv.emby.embyatv.startup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.details.DetailsActivity;

/**
 * Created by Eric on 3/2/2015.
 */
public class DirectEntryActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TvApp.getApplication().getCurrentUser().getHasPassword()
                && (!TvApp.getApplication().getIsAutoLoginConfigured()
                || (TvApp.getApplication().getIsAutoLoginConfigured() && TvApp.getApplication().getPrefs().getBoolean("pref_auto_pw_prompt", false)))) {
            //Need to prompt for pw
            Intent pwIntent = new Intent(this, DpadPwActivity.class);
            pwIntent.putExtra("User", TvApp.getApplication().getSerializer().SerializeToString(TvApp.getApplication().getCurrentUser()));
            pwIntent.putExtra("ItemId", getIntent().getStringExtra("ItemId"));
            pwIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(pwIntent);
        } else {
            //Can just go right into details
            Intent detailsIntent = new Intent(this, DetailsActivity.class);
            detailsIntent.putExtra("ItemId", getIntent().getStringExtra("ItemId"));
            startActivity(detailsIntent);
        }
    }
}
