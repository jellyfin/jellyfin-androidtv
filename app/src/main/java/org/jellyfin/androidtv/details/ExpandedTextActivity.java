package tv.emby.embyatv.details;

import android.os.Bundle;
import android.app.Activity;
import android.widget.TextView;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;

public class ExpandedTextActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expanded_text);

        TextView text = (TextView)findViewById(R.id.textView);
        text.setTypeface(TvApp.getApplication().getDefaultFont());
        text.setText(getIntent().getStringExtra("text"));
    }

}
