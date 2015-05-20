package tv.emby.embyatv.startup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.details.DetailsActivity;
import tv.emby.embyatv.details.FullDetailsActivity;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 3/2/2015.
 */
public class DirectEntryActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_startup);

        if (TvApp.getApplication().getCurrentUser().getHasPassword()
                && (!TvApp.getApplication().getIsAutoLoginConfigured()
                || (TvApp.getApplication().getIsAutoLoginConfigured() && TvApp.getApplication().getPrefs().getBoolean("pref_auto_pw_prompt", false)))) {
            //Need to prompt for pw
            Utils.processPasswordEntry(this, TvApp.getApplication().getCurrentUser(), getIntent().getStringExtra("ItemId"));
        } else {
            //Can just go right into details
            Intent detailsIntent = new Intent(this, FullDetailsActivity.class);
            detailsIntent.putExtra("ItemId", getIntent().getStringExtra("ItemId"));
            startActivity(detailsIntent);
        }
    }
}
