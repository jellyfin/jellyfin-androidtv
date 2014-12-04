package tv.mediabrowser.mediabrowsertv;

import android.os.Bundle;

/**
 * Created by Eric on 12/4/2014.
 */
public class HomeFragment extends StdBrowseFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        MainTitle = this.getString(R.string.home_title);
        super.onActivityCreated(savedInstanceState);

    }
}
