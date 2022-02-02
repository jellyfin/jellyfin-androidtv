package org.jellyfin.androidtv.ui.browsing;

import static org.koin.java.KoinJavaComponent.inject;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.querying.ViewQuery;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.ClockBehavior;
import org.jellyfin.androidtv.ui.ClockUserView;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

public class StdBrowseFragment extends BrowseSupportFragment implements RowLoader {
    protected String MainTitle;
    protected boolean ShowBadge = true;
    protected BaseRowItem mCurrentItem;
    protected ListRow mCurrentRow;
    protected CompositeClickedListener mClickedListener = new CompositeClickedListener();
    protected CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    protected ArrayObjectAdapter mRowsAdapter;
    protected ArrayList<BrowseRowDef> mRows = new ArrayList<>();
    protected CardPresenter mCardPresenter;
    private TextClock mClock;

    protected boolean justLoaded = true;

    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backgroundService.getValue().attach(requireActivity());

        setupUIElements();

        setupQueries(this);

        setupEventListeners();
    }

    protected void setupQueries(RowLoader rowLoader) {
        rowLoader.loadRows(mRows);
    }

    @Override
    public void onResume() {
        super.onResume();

        ClockBehavior showClock = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getClockBehavior());

        if (showClock == ClockBehavior.ALWAYS || showClock == ClockBehavior.IN_MENUS)
            mClock.setVisibility(View.VISIBLE);
        else
            mClock.setVisibility(View.GONE);

        //React to deletion
        DataRefreshService dataRefreshService = KoinJavaComponent.<DataRefreshService>get(DataRefreshService.class);
        if (getActivity() != null && !getActivity().isFinishing() && mCurrentRow != null && mCurrentItem != null && mCurrentItem.getItemId() != null && mCurrentItem.getItemId().equals(dataRefreshService.getLastDeletedItemId())) {
            ((ItemRowAdapter)mCurrentRow.getAdapter()).remove(mCurrentItem);
            dataRefreshService.setLastDeletedItemId(null);
        }

        if (!justLoaded) {
            //Re-retrieve anything that needs it but delay slightly so we don't take away gui landing
            if (mRowsAdapter != null) {
                refreshCurrentItem();
            }

        } else {
            justLoaded = false;
        }
    }

    public void loadRows(List<BrowseRowDef> rows) {
        mRowsAdapter = new ArrayObjectAdapter(new PositionableListRowPresenter());
        mCardPresenter = new CardPresenter();

        for (BrowseRowDef def : rows) {
            HeaderItem header = new HeaderItem(def.getHeaderText());
            ItemRowAdapter rowAdapter;
            switch (def.getQueryType()) {
                case NextUp:
                    rowAdapter = new ItemRowAdapter(def.getNextUpQuery(), true, mCardPresenter, mRowsAdapter);
                    break;
                case LatestItems:
                    rowAdapter = new ItemRowAdapter(def.getLatestItemsQuery(), true, mCardPresenter, mRowsAdapter);
                    break;
                case Season:
                    rowAdapter = new ItemRowAdapter(def.getSeasonQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case Upcoming:
                    rowAdapter = new ItemRowAdapter(def.getUpcomingQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case Views:
                    rowAdapter = new ItemRowAdapter(new ViewQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case SimilarSeries:
                    rowAdapter = new ItemRowAdapter(def.getSimilarQuery(), QueryType.SimilarSeries, mCardPresenter, mRowsAdapter);
                    break;
                case SimilarMovies:
                    rowAdapter = new ItemRowAdapter(def.getSimilarQuery(), QueryType.SimilarMovies, mCardPresenter, mRowsAdapter);
                    break;
                case Persons:
                    rowAdapter = new ItemRowAdapter(def.getPersonsQuery(), def.getChunkSize(), mCardPresenter, mRowsAdapter);
                    break;
                case LiveTvChannel:
                    rowAdapter = new ItemRowAdapter(def.getTvChannelQuery(), 40, mCardPresenter, mRowsAdapter);
                    break;
                case LiveTvProgram:
                    rowAdapter = new ItemRowAdapter(def.getProgramQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case LiveTvRecording:
                    rowAdapter = new ItemRowAdapter(def.getRecordingQuery(), def.getChunkSize(), mCardPresenter, mRowsAdapter);
                    break;
                case LiveTvRecordingGroup:
                    rowAdapter = new ItemRowAdapter(def.getRecordingGroupQuery(), mCardPresenter, mRowsAdapter);
                    break;
                default:
                    rowAdapter = new ItemRowAdapter(def.getQuery(), def.getChunkSize(), def.getPreferParentThumb(), def.isStaticHeight(), mCardPresenter, mRowsAdapter, def.getQueryType());
                    break;
            }

            rowAdapter.setReRetrieveTriggers(def.getChangeTriggers());

            ListRow row = new ListRow(header, rowAdapter);
            mRowsAdapter.add(row);
            rowAdapter.setRow(row);
            rowAdapter.Retrieve();
        }

        addAdditionalRows(mRowsAdapter);

        setAdapter(mRowsAdapter);
    }

    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {

    }

    protected void setupUIElements() {
        if (ShowBadge)
            setBadgeDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.app_logo));

        setTitle(MainTitle); // Badge, when set, takes precedent over title
        setHeadersState(HEADERS_DISABLED);
    }

    @Override
    public void onStart() {
        super.onStart();

        // move the badge/title to the left to make way for our clock/user bug
        ImageView badge = (ImageView) getActivity().findViewById(R.id.title_badge);
        TextView title = (TextView) getActivity().findViewById(R.id.title_text);
        if (badge != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) badge.getLayoutParams();
            lp.rightMargin = Utils.convertDpToPixel(getActivity(), 120);
            badge.setLayoutParams(lp);
        }
        if (title != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) title.getLayoutParams();
            lp.rightMargin = Utils.convertDpToPixel(getActivity(), 120);
            title.setLayoutParams(lp);
        }

        ViewGroup root = (ViewGroup) getActivity().findViewById(android.R.id.content);

        // and add the clock element
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ClockUserView userClock = new ClockUserView(getActivity(), null);
        mClock = userClock.findViewById(R.id.clock);
        layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        layoutParams.rightMargin = Utils.convertDpToPixel(getActivity(), 40);
        layoutParams.topMargin = Utils.convertDpToPixel(getActivity(), 20);
        userClock.setLayoutParams(layoutParams);
        root.addView(userClock);
    }

    protected void setupEventListeners() {
        setOnItemViewClickedListener(mClickedListener);
        mClickedListener.registerListener(new ItemViewClickedListener());

        setOnItemViewSelectedListener(mSelectedListener);
        mSelectedListener.registerListener(new ItemViewSelectedListener());
    }

    private void refreshCurrentItem() {
        if (mCurrentItem != null && mCurrentItem.getBaseItemType() != BaseItemType.UserView && mCurrentItem.getBaseItemType() != BaseItemType.CollectionFolder) {
            Timber.d("Refresh item \"%s\"", mCurrentItem.getFullName(requireContext()));
            mCurrentItem.refresh(new EmptyResponse() {
                @Override
                public void onResponse() {
                    ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow)mCurrentRow).getAdapter();
                    adapter.notifyArrayItemRangeChanged(adapter.indexOf(mCurrentItem), 1);
                }
            });

        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow)row).getAdapter(), ((BaseRowItem)item).getIndex(), getActivity());
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) {
                mCurrentItem = null;
                //fill in default background
                backgroundService.getValue().clearBackgrounds();
                return;
            } else {
                mCurrentItem = (BaseRowItem)item;
            }

            mCurrentRow = (ListRow) row;
            BaseRowItem rowItem = (BaseRowItem) item;

            if (((ListRow) row).getAdapter() instanceof ItemRowAdapter) {
                //TvApp.getApplication().getLogger().Debug("Selected Item "+rowItem.getIndex() + " type: "+ (rowItem.getItemType().equals(BaseRowItem.ItemType.BaseItem) ? rowItem.getBaseItem().getType() : "other"));
                ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow) row).getAdapter();
                adapter.loadMoreItemsIfNeeded(rowItem.getIndex());
            }

            backgroundService.getValue().setBackground(rowItem.getBaseItem());
        }
    }
}
