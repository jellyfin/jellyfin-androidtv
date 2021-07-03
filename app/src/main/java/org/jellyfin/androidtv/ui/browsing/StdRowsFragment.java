package org.jellyfin.androidtv.ui.browsing;

import static org.koin.java.KoinJavaComponent.inject;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.bumptech.glide.Glide;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.querying.ViewQuery;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.RatingType;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.ui.shared.IKeyListener;
import org.jellyfin.androidtv.ui.shared.IMessageListener;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

public class StdRowsFragment extends RowsSupportFragment implements IRowLoader {
    protected BaseActivity mActivity;
    protected BaseRowItem mCurrentItem;
    protected ListRow mCurrentRow;
    protected CompositeClickedListener mClickedListener = new CompositeClickedListener();
    protected CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    protected ArrayObjectAdapter mRowsAdapter;
    protected ArrayList<BrowseRowDef> mRows = new ArrayList<>();
    protected CardPresenter mCardPresenter;
    protected boolean justLoaded = true;
    protected boolean homeSection = false;

    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private Lazy<UserPreferences> userPreferences = inject(UserPreferences.class);

    public StdRowsFragment() {

    }

    public StdRowsFragment(boolean homeSection) {
        this.homeSection = homeSection;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() instanceof BaseActivity) mActivity = (BaseActivity)getActivity();

        backgroundService.getValue().attach(requireActivity());

        setupUIElements();

        setupQueries(this);

        setupEventListeners();
    }

    protected void setupQueries(IRowLoader rowLoader) {
        rowLoader.loadRows(mRows);
    }

    @Override
    public void onResume() {
        super.onResume();

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
                refreshRows();
            }

        } else {
            justLoaded = false;
        }
    }

    protected void refreshRows() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mActivity == null || mActivity.isFinishing()) return;
                for (int i = 0; i < mRowsAdapter.size(); i++) {
                    if (mRowsAdapter.get(i) instanceof ListRow) {
                        if (((ListRow) mRowsAdapter.get(i)).getAdapter() instanceof ItemRowAdapter && !mActivity.isFinishing()) {
                            ((ItemRowAdapter) ((ListRow) mRowsAdapter.get(i)).getAdapter()).ReRetrieveIfNeeded();
                        }
                    }
                }
            }
        },1500);
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

    }

    protected void setupEventListeners() {
        setOnItemViewClickedListener(mClickedListener);
        mClickedListener.registerListener(new ItemViewClickedListener());

        setOnItemViewSelectedListener(mSelectedListener);
        mSelectedListener.registerListener(new ItemViewSelectedListener());

        if (mActivity != null) {
            mActivity.registerKeyListener(new IKeyListener() {
                @Override
                public boolean onKeyUp(int key, KeyEvent event) {
                    return KeyProcessor.HandleKey(key, mCurrentItem, mActivity);
                }
            });

            mActivity.registerMessageListener(new IMessageListener() {
                @Override
                public void onMessageReceived(CustomMessage message) {
                    switch (message) {

                        case RefreshCurrentItem:
                            refreshCurrentItem();
                            break;
                    }
                }
            });
        }
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
            BaseItemDto baseItem = rowItem.getBaseItem();

            if (((ListRow) row).getAdapter() instanceof ItemRowAdapter) {
                //TvApp.getApplication().getLogger().Debug("Selected Item "+rowItem.getIndex() + " type: "+ (rowItem.getItemType().equals(BaseRowItem.ItemType.BaseItem) ? rowItem.getBaseItem().getType() : "other"));
                ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow) row).getAdapter();
                adapter.loadMoreItemsIfNeeded(rowItem.getIndex());
            }

            backgroundService.getValue().setBackground(baseItem);

            if (homeSection && userPreferences.getValue().get(UserPreferences.Companion.getHomeHeaderEnabled())) {
                LinearLayout itemInfoView = mActivity.findViewById(R.id.item_info);
                itemInfoView.setVisibility(View.VISIBLE);

                TextView rowHeader = mActivity.findViewById(R.id.home_row_header);
                rowHeader.setText(row.getHeaderItem().getName());

                ImageView itemLogoView = mActivity.findViewById(R.id.item_logo);
                TextView itemTitleView = mActivity.findViewById(R.id.item_title);
                TextView numbersView = mActivity.findViewById(R.id.numbers_row);
                TextView itemSubtitleView = mActivity.findViewById(R.id.item_subtitle);

                String itemTitle = rowItem.getBaseItemType() == BaseItemType.Episode ? baseItem.getSeriesName()
                    : rowItem.getBaseItemType() == BaseItemType.CollectionFolder || rowItem.getBaseItemType() == BaseItemType.UserView ? ""
                    : rowItem.getCardName(requireContext());

                String subtitle = rowItem.getBaseItemType() == BaseItemType.Episode ? baseItem.getName()
                    : baseItem.getTaglines() != null && baseItem.getTaglines().size() > 0 ? baseItem.getTaglines().get(0)
                    : baseItem.getShortOverview() != null ? baseItem.getShortOverview()
                    : baseItem.getOverview() != null ? baseItem.getOverview() : "";

                SpannableStringBuilder numbersString = new SpannableStringBuilder();
                if (rowItem.getBaseItemType() == BaseItemType.Episode) {
                    if (baseItem.getParentIndexNumber() != null) {
                        if (baseItem.getParentIndexNumber() == 0)
                            numbersString.append(requireContext().getString(R.string.lbl_special));
                        else
                            numbersString.append(requireContext().getString(R.string.lbl_season_number_full, baseItem.getParentIndexNumber()));
                    }
                    if (baseItem.getIndexNumber() != null && (baseItem.getParentIndexNumber() == null || baseItem.getParentIndexNumber() != 0)) {
                        if (numbersString.length() > 0) numbersString.append(" • ");
                        if (baseItem.getIndexNumberEnd() != null)
                            numbersString.append(requireContext().getString(R.string.lbl_episode_range_full, baseItem.getIndexNumber(), baseItem.getIndexNumberEnd()));
                        else
                            numbersString.append(requireContext().getString(R.string.lbl_episode_number_full, baseItem.getIndexNumber()));
                    }
                } else {
                    if (baseItem.getProductionYear() != null)
                        numbersString.append(baseItem.getProductionYear().toString());
                    if (baseItem.getOfficialRating() != null) {
                        if (numbersString.length() > 0) numbersString.append(" • ");
                        numbersString.append(baseItem.getOfficialRating());
                    }
                    if (rowItem.getBaseItemType() == BaseItemType.MusicAlbum && baseItem.getChildCount() != null && baseItem.getChildCount() > 0) {
                        if (numbersString.length() > 0) numbersString.append(" • ");
                        numbersString.append(rowItem.getSubText(requireContext()));
                    }
                }
                if (rowItem.getBaseItemType() != BaseItemType.UserView && rowItem.getBaseItemType() != BaseItemType.CollectionFolder) {
                    RatingType ratingType = userPreferences.getValue().get(UserPreferences.Companion.getDefaultRatingType());
                    if (ratingType == RatingType.RATING_TOMATOES && baseItem.getCriticRating() != null) {
                        Drawable badge = baseItem.getCriticRating() > 59 ? ContextCompat.getDrawable(requireContext(), R.drawable.ic_rt_fresh)
                            : ContextCompat.getDrawable(requireContext(), R.drawable.ic_rt_rotten);
                        if (badge != null) {
                            badge.setBounds(0, 0, numbersView.getLineHeight() + 3, numbersView.getLineHeight() + 3);
                            ImageSpan imageSpan = new ImageSpan(badge);
                            if (numbersString.length() > 0) numbersString.append("   ");
                            numbersString.setSpan(imageSpan, numbersString.length() - 1, numbersString.length(), 0);
                            numbersString.append(" ").append(Integer.toString(Math.round(baseItem.getCriticRating()))).append("%");
                        }
                    } else if (ratingType == RatingType.RATING_STARS && baseItem.getCommunityRating() != null) {
                        Drawable badge = ContextCompat.getDrawable(requireContext(), R.drawable.ic_star);
                        if (badge != null) {
                            badge.setBounds(0, 0, numbersView.getLineHeight() + 3, numbersView.getLineHeight() + 3);
                            ImageSpan imageSpan = new ImageSpan(badge);
                            if (numbersString.length() > 0) numbersString.append("   ");
                            numbersString.setSpan(imageSpan, numbersString.length() - 1, numbersString.length(), 0);
                            numbersString.append(" ").append(baseItem.getCommunityRating().toString());
                        }
                    }
                }

                // Load the title and subtitles
                itemTitleView.setText(itemTitle);
                numbersView.setText(numbersString);
                itemSubtitleView.setText(subtitle);
                if (rowItem.getBaseItemType() == BaseItemType.Episode || (baseItem.getTaglines() != null && baseItem.getTaglines().size() > 0))
                    itemSubtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                else
                    itemSubtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

                // Load the logo
                String imageUrl = ImageUtils.getLogoImageUrl(baseItem, apiClient.getValue(), 0, false);
                if (imageUrl != null) {
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .into(itemLogoView);
                    itemLogoView.setContentDescription(baseItem.getName());
                    itemLogoView.setVisibility(View.VISIBLE);
                    itemTitleView.setVisibility(View.GONE);
                } else {
                    itemLogoView.setVisibility(View.GONE);
                    itemTitleView.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
