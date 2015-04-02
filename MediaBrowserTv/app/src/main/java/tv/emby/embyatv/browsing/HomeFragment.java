package tv.emby.embyatv.browsing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.widget.Toast;

import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.livetv.RecommendedProgramQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.NextUpQuery;
import tv.emby.embyatv.integration.RecommendationManager;
import tv.emby.embyatv.model.ChangeTriggerType;
import tv.emby.embyatv.ui.GridButton;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;
import tv.emby.embyatv.presentation.GridButtonPresenter;
import tv.emby.embyatv.querying.StdItemQuery;
import tv.emby.embyatv.querying.ViewQuery;
import tv.emby.embyatv.settings.SettingsActivity;
import tv.emby.embyatv.startup.SelectUserActivity;
import tv.emby.embyatv.validation.UnlockActivity;

/**
 * Created by Eric on 12/4/2014.
 */
public class HomeFragment extends StdBrowseFragment {
    private static final int LOGOUT = 0;
    private static final int SETTINGS = 1;
    private static final int REPORT = 2;
    private static final int UNLOCK = 3;

    private ArrayObjectAdapter toolsRow;
    private GridButton unlockButton;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        MainTitle = this.getString(R.string.home_title);
        //Validate the app
        TvApp.getApplication().validate();

        super.onActivityCreated(savedInstanceState);

        //Validate recommendations
        RecommendationManager.getInstance().validate();

    }

    @Override
    public void onResume() {
        super.onResume();

        //if we were locked before and have just unlocked, remove the button
        if (unlockButton != null && (TvApp.getApplication().isRegistered() || TvApp.getApplication().isPaid())) toolsRow.remove(unlockButton);
    }

    @Override
    protected void setupQueries(IRowLoader rowLoader) {

        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_library), new ViewQuery()));

        StdItemQuery resumeMovies = new StdItemQuery();
        resumeMovies.setIncludeItemTypes(new String[]{"Movie"});
        resumeMovies.setRecursive(true);
        resumeMovies.setLimit(50);
        resumeMovies.setFilters(new ItemFilter[]{ItemFilter.IsResumable});
        resumeMovies.setSortBy(new String[]{ItemSortBy.DatePlayed});
        resumeMovies.setSortOrder(SortOrder.Descending);
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_continue_watching), resumeMovies, 0, new ChangeTriggerType[] {ChangeTriggerType.MoviePlayback}));

        StdItemQuery latestMovies = new StdItemQuery();
        latestMovies.setIncludeItemTypes(new String[]{"Movie"});
        latestMovies.setRecursive(true);
        latestMovies.setLimit(50);
        latestMovies.setFilters(new ItemFilter[]{ItemFilter.IsUnplayed});
        latestMovies.setSortBy(new String[]{ItemSortBy.DateCreated});
        latestMovies.setSortOrder(SortOrder.Descending);
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_latest_movies), latestMovies, 0, new ChangeTriggerType[] {ChangeTriggerType.LibraryUpdated, ChangeTriggerType.MoviePlayback}));

        NextUpQuery nextUpQuery = new NextUpQuery();
        nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        nextUpQuery.setLimit(50);
        nextUpQuery.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_next_up_tv), nextUpQuery, new ChangeTriggerType[] {ChangeTriggerType.TvPlayback}));

        //On now
        RecommendedProgramQuery onNow = new RecommendedProgramQuery();
        onNow.setIsAiring(true);
        onNow.setUserId(TvApp.getApplication().getCurrentUser().getId());
        onNow.setLimit(50);
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_on_now), onNow));

//        StdItemQuery latestMusic = new StdItemQuery();
//        latestMusic.setIncludeItemTypes(new String[]{"MusicAlbum"});
//        latestMusic.setRecursive(true);
//        latestMusic.setLimit(50);
//        latestMusic.setSortBy(new String[]{ItemSortBy.DateCreated});
//        latestMusic.setSortOrder(SortOrder.Descending);
//        mRows.add(new BrowseRowDef("Latest Albums", latestMusic, 0));

        rowLoader.loadRows(mRows);
    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        super.addAdditionalRows(rowAdapter);

        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), mApplication.getString(R.string.lbl_settings), null);

        GridButtonPresenter mGridPresenter = new GridButtonPresenter();
        toolsRow = new ArrayObjectAdapter(mGridPresenter);
        toolsRow.add(new GridButton(SETTINGS, mApplication.getString(R.string.lbl_app_settings), R.drawable.gears));
        toolsRow.add(new GridButton(LOGOUT, mApplication.getString(R.string.lbl_logout) + TvApp.getApplication().getCurrentUser().getName(), R.drawable.logout));
        //give this some time to have validated
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!TvApp.getApplication().isRegistered() && !TvApp.getApplication().isPaid()) {
                    unlockButton = new GridButton(UNLOCK, mApplication.getString(R.string.lbl_unlock), R.drawable.unlock);
                    toolsRow.add(unlockButton);
                }
            }
        }, 5000);
        //toolsRow.add(new GridButton(REPORT, mApplication.getString(R.string.lbl_send_logs), R.drawable.upload));
        rowAdapter.add(new ListRow(gridHeader, toolsRow));
    }

    @Override
    protected void setupEventListeners() {
        super.setupEventListeners();
        mClickedListener.registerListener(new ItemViewClickedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof GridButton) {
                switch (((GridButton) item).getId()) {
                    case LOGOUT:
                        TvApp app = TvApp.getApplication();
                        if (app.getIsAutoLoginConfigured()) {
                            // Present user selection
                            app.setLoginApiClient(app.getApiClient());
                            Intent userIntent = new Intent(getActivity(), SelectUserActivity.class);
                            userIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            getActivity().startActivity(userIntent);

                        }

                        getActivity().finish(); //don't actually log out because we handle it ourselves

                        break;
                    case SETTINGS:
                        Intent settings = new Intent(getActivity(), SettingsActivity.class);
                        getActivity().startActivity(settings);
                        break;
                    case UNLOCK:
                        Intent unlock = new Intent(getActivity(), UnlockActivity.class);
                        getActivity().startActivity(unlock);
                        break;
                    case REPORT:
                        Utils.reportError(getActivity(), "Send Log to Dev");
                        break;
                    default:
                        Toast.makeText(getActivity(), item.toString(), Toast.LENGTH_SHORT)
                                .show();
                        break;
                }
            }
        }
    }


}
