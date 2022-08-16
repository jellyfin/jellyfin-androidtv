package org.jellyfin.androidtv.ui.browsing;

import static org.koin.java.KoinJavaComponent.inject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.constant.ChangeTriggerType;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.constant.Extras;
import org.jellyfin.androidtv.constant.GridDirection;
import org.jellyfin.androidtv.constant.ImageType;
import org.jellyfin.androidtv.constant.PosterSize;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.FilterOptions;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.data.querying.ViewQuery;
import org.jellyfin.androidtv.data.repository.UserViewsRepository;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.databinding.HorizontalGridBrowseBinding;
import org.jellyfin.androidtv.databinding.PopupEmptyBinding;
import org.jellyfin.androidtv.preference.LibraryPreferences;
import org.jellyfin.androidtv.preference.PreferencesRepository;
import org.jellyfin.androidtv.ui.AlphaPickerView;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.preference.PreferencesActivity;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.HorizontalGridPresenter;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.ui.shared.KeyListener;
import org.jellyfin.androidtv.ui.shared.MessageListener;
import org.jellyfin.androidtv.util.CoroutineUtils;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.entities.SortOrder;
import org.jellyfin.apiclient.model.querying.ArtistsQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.constant.CollectionType;
import org.jellyfin.sdk.model.constant.ItemSortBy;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import kotlin.Lazy;
import kotlinx.serialization.json.Json;
import timber.log.Timber;

public class BrowseGridFragment extends Fragment {
    private final static int CHUNK_SIZE_MINIMUM = 25;

    private String mainTitle;
    private BaseActivity mActivity;
    private BaseRowItem mCurrentItem;
    private CompositeClickedListener mClickedListener = new CompositeClickedListener();
    private CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    private final Handler mHandler = new Handler();
    private int mCardHeight;
    private BrowseRowDef mRowDef;
    private CardPresenter mCardPresenter;

    private boolean justLoaded = true;
    private PosterSize mPosterSizeSetting = PosterSize.MED;
    private ImageType mImageType = ImageType.POSTER;
    private GridDirection mGridDirection = GridDirection.HORIZONTAL;
    private boolean determiningPosterSize = false;

    private UUID mParentId;
    private BaseItemDto mFolder;
    private LibraryPreferences libraryPreferences;

    private HorizontalGridBrowseBinding binding;
    private ItemRowAdapter mAdapter;
    private Presenter mGridPresenter;
    private Presenter.ViewHolder mGridViewHolder;
    private BaseGridView mGridView;
    private int mSelectedPosition = -1;
    private int mGridHeight = -1;
    private int mGridWidth = -1;
    private int mGridItemSpacingHorizontal = 0;
    private int mGridItemSpacingVertical = 0;
    private int mGridPaddingLeft = 0;
    private int mGridPaddingTop = 0;

    private final Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private final Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private final Lazy<PreferencesRepository> preferencesRepository = inject(PreferencesRepository.class);
    private final Lazy<UserViewsRepository> userViewsRepository = inject(UserViewsRepository.class);
    private final Lazy<UserRepository> userRepository = inject(UserRepository.class);

    private int mCardsScreenEst = 0;
    private int mCardsScreenStride = 0;
    private double mCardFocusScale = 1.15; // 115%, just a default we use the resource card_scale_focus otherwise
    private final int MIN_NUM_CARDS = 5; // minimum number of visible cards we allow, this results in more empty space
    private final double CARD_SPACING_PCT = 1.0; // 100% expressed as relative to the padding_left/top, which depends on the mCardFocusScale and AspectRatio
    private final double CARD_SPACING_HORIZONTAL_BANNER_PCT = 0.5; // 50% allow horizontal card overlapping for banners, otherwise spacing is too large
    private final int MIN_GRIDSIZE_CHANGE_DELTA = 4; // minimum pixel size changes, to trigger a recreate of the grid via onGridSizeMeasurements
    private final int VIEW_SELECT_UPDATE_DELAY = 250; // delay in ms until we update the top-row info for a selected item

    private boolean mDirty = true; // CardHeight, RowDef or GridSize changed

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init with some working defaults
        DisplayMetrics display = requireContext().getResources().getDisplayMetrics();
        mGridHeight = display.heightPixels - (int) Math.round(display.density * 130.6); // top + bottom in dp, elements scale with density so adjust accordingly
        mGridWidth = display.widthPixels;

        sortOptions = new HashMap<>();
        {
            sortOptions.put(0, new SortOption(getString(R.string.lbl_name), ItemSortBy.SortName, SortOrder.Ascending));
            sortOptions.put(1, new SortOption(getString(R.string.lbl_date_added), ItemSortBy.DateCreated + "," + ItemSortBy.SortName, SortOrder.Descending));
            sortOptions.put(2, new SortOption(getString(R.string.lbl_premier_date), ItemSortBy.PremiereDate + "," + ItemSortBy.SortName, SortOrder.Descending));
            sortOptions.put(3, new SortOption(getString(R.string.lbl_rating), ItemSortBy.OfficialRating + "," + ItemSortBy.SortName, SortOrder.Ascending));
            sortOptions.put(4, new SortOption(getString(R.string.lbl_community_rating), ItemSortBy.CommunityRating + "," + ItemSortBy.SortName, SortOrder.Descending));
            sortOptions.put(5, new SortOption(getString(R.string.lbl_critic_rating), ItemSortBy.CriticRating + "," + ItemSortBy.SortName, SortOrder.Descending));
            sortOptions.put(6, new SortOption(getString(R.string.lbl_last_played), ItemSortBy.DatePlayed + "," + ItemSortBy.SortName, SortOrder.Descending));
        }

        if (getActivity() instanceof BaseActivity) mActivity = (BaseActivity) getActivity();
        backgroundService.getValue().attach(requireActivity());

        mFolder = Json.Default.decodeFromString(BaseItemDto.Companion.serializer(), requireActivity().getIntent().getStringExtra(Extras.Folder));
        mParentId = mFolder.getId();
        mainTitle = mFolder.getName();
        libraryPreferences = preferencesRepository.getValue().getLibraryPreferences(Objects.requireNonNull(mFolder.getDisplayPreferencesId()));
        mPosterSizeSetting = libraryPreferences.get(LibraryPreferences.Companion.getPosterSize());
        mImageType = libraryPreferences.get(LibraryPreferences.Companion.getImageType());
        mGridDirection = libraryPreferences.get(LibraryPreferences.Companion.getGridDirection());
        mCardFocusScale = getResources().getFraction(R.fraction.card_scale_focus, 1, 1);

        if (mGridDirection.equals(GridDirection.VERTICAL))
            setGridPresenter(new VerticalGridPresenter());
        else
            setGridPresenter(new HorizontalGridPresenter());

        setDefaultGridRowCols(mPosterSizeSetting, mImageType);
        setAutoCardGridValues();
        setupQueries();
        setupEventListeners();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = HorizontalGridBrowseBinding.inflate(inflater, container, false);

        // Hide the description because we don't have room for it
        binding.npBug.showDescription(false);

        // NOTE: we only get the 100% correct grid size if we render it once, so hook into it here
        binding.rowsFragment.post(() -> {
            if (binding.rowsFragment.getHeight() > 0 && binding.rowsFragment.getWidth() > 0) {
                if (mGridView == null) {
                    return;
                }
                // prevent adaption on minor size delta's
                if (Math.abs(mGridHeight - binding.rowsFragment.getHeight()) > MIN_GRIDSIZE_CHANGE_DELTA || Math.abs(mGridWidth - binding.rowsFragment.getWidth()) > MIN_GRIDSIZE_CHANGE_DELTA) {
                    mGridHeight = binding.rowsFragment.getHeight();
                    mGridWidth = binding.rowsFragment.getWidth();
                    Timber.d("Auto-Adapting grid size to height <%s> width <%s>", binding.rowsFragment.getHeight(), binding.rowsFragment.getWidth());
                    mDirty = true;
                    determiningPosterSize = true;
                    setAutoCardGridValues();
                    createGrid();
                    loadGrid();
                    determiningPosterSize = false;
                }
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        createGrid();
        loadGrid();
        addTools();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGridView = null;
    }

    private void createGrid() {
        mGridViewHolder = mGridPresenter.onCreateViewHolder(binding.rowsFragment);
        if (mGridViewHolder instanceof HorizontalGridPresenter.ViewHolder) {
            mGridView = ((HorizontalGridPresenter.ViewHolder) mGridViewHolder).getGridView();
            mGridView.setGravity(Gravity.CENTER_VERTICAL);
            mGridView.setPadding(mGridPaddingLeft, mGridPaddingTop, mGridPaddingLeft, mGridPaddingTop); // prevent initial card cutoffs
        } else if (mGridViewHolder instanceof VerticalGridPresenter.ViewHolder) {
            mGridView = ((VerticalGridPresenter.ViewHolder) mGridViewHolder).getGridView();
            mGridView.setGravity(Gravity.CENTER_HORIZONTAL);
            mGridView.setPadding(mGridPaddingLeft, mGridPaddingTop, mGridPaddingLeft, mGridPaddingTop); // prevent initial card cutoffs
        }
        mGridView.setHorizontalSpacing(mGridItemSpacingHorizontal);
        mGridView.setVerticalSpacing(mGridItemSpacingVertical);
        mGridView.setFocusable(true);
        binding.rowsFragment.removeAllViews();
        binding.rowsFragment.addView(mGridViewHolder.view);

        updateAdapter();
    }

    private void updateAdapter() {
        if (mGridView != null) {
            mGridPresenter.onBindViewHolder(mGridViewHolder, mAdapter);
            if (mSelectedPosition != -1) {
                mGridView.setSelectedPosition(mSelectedPosition);
            }
        }
    }

    /**
     * Sets the grid presenter.
     */
    public void setGridPresenter(HorizontalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        gridPresenter.setOnItemViewSelectedListener(mRowSelectedListener);
        gridPresenter.setOnItemViewClickedListener(mClickedListener);
        mGridPresenter = gridPresenter;
    }

    /**
     * Sets the grid presenter.
     */
    public void setGridPresenter(VerticalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        gridPresenter.setOnItemViewSelectedListener(mRowSelectedListener);
        gridPresenter.setOnItemViewClickedListener(mClickedListener);
        mGridPresenter = gridPresenter;
    }


    public void setItem(BaseRowItem item) {
        if (item != null) {
            binding.title.setText(item.getFullName(requireContext()));
            InfoLayoutHelper.addInfoRow(requireContext(), item, binding.infoRow, true, true);
        } else {
            binding.title.setText("");
            binding.infoRow.removeAllViews();
        }
    }

    public class SortOption {
        public String name;
        public String value;
        public SortOrder order;

        public SortOption(String name, String value, SortOrder order) {
            this.name = name;
            this.value = value;
            this.order = order;
        }
    }

    private Map<Integer, SortOption> sortOptions;

    private SortOption getSortOption(String value) {
        for (Integer key : sortOptions.keySet()) {
            SortOption option = sortOptions.get(key);
            if (Objects.requireNonNull(option).value.equals(value)) return option;
        }

        return new SortOption("Unknown", "", SortOrder.Ascending);
    }

    public void setStatusText(String folderName) {
        String text = getString(R.string.lbl_showing) + " ";
        FilterOptions filters = mAdapter.getFilters();
        if (filters == null || (!filters.isFavoriteOnly() && !filters.isUnwatchedOnly())) {
            text += getString(R.string.lbl_all_items);
        } else {
            text += (filters.isUnwatchedOnly() ? getString(R.string.lbl_unwatched) : "") + " " +
                    (filters.isFavoriteOnly() ? getString(R.string.lbl_favorites) : "");
        }

        if (mAdapter.getStartLetter() != null) {
            text += " " + getString(R.string.lbl_starting_with) + " " + mAdapter.getStartLetter();
        }

        text += " " + getString(R.string.lbl_from) + " '" + folderName + "' " + getString(R.string.lbl_sorted_by) + " " + getSortOption(mAdapter.getSortBy()).name;

        binding.statusText.setText(text);
    }

    final private OnItemViewSelectedListener mRowSelectedListener =
            new OnItemViewSelectedListener() {
                @Override
                public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                           RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int position = mGridView.getSelectedPosition();
                    Timber.d("row selected position %s", position);
                    if (position != mSelectedPosition) {
                        mSelectedPosition = position;
                    }
                    // Update the counter
                    updateCounter(position + 1);
                    if (position >= 0) {
                        mSelectedListener.onItemSelected(itemViewHolder, item, rowViewHolder, row);
                    }
                }
            };

    public void updateCounter(int position) {
        if (mAdapter != null) {
            binding.counter.setText(MessageFormat.format("{0} | {1}", position, mAdapter.getTotalItems()));
        }
    }

    private void setRowDef(final BrowseRowDef rowDef) {
        if (mRowDef == null || mRowDef.hashCode() != rowDef.hashCode()) {
            mDirty = true;
        }
        mRowDef = rowDef;
    }

    private double getCardWidthBy(final double cardHeight, ImageType imageType, BaseItemDto folder) {
        switch (imageType) {
            case POSTER:
                // special handling for square posters
                BaseItemKind fType = folder.getType();
                if (fType == BaseItemKind.AUDIO || fType == BaseItemKind.GENRE || fType == BaseItemKind.MUSIC_ALBUM || fType == BaseItemKind.MUSIC_ARTIST || fType == BaseItemKind.MUSIC_GENRE) {
                    return cardHeight;
                } else if (fType == BaseItemKind.COLLECTION_FOLDER && CollectionType.Music.equals(folder.getCollectionType())) {
                    return cardHeight;
                } else {
                    return cardHeight * ImageUtils.ASPECT_RATIO_2_3;
                }
            case THUMB:
                return cardHeight * ImageUtils.ASPECT_RATIO_16_9;
            case BANNER:
                return cardHeight * CardPresenter.ASPECT_RATIO_BANNER;
            default:
                throw new IllegalStateException("Unexpected value: " + imageType);
        }
    }

    private double getCardHeightBy(final double cardWidth, ImageType imageType, BaseItemDto folder) {
        switch (imageType) {
            case POSTER:
                // special handling for square posters
                BaseItemKind fType = folder.getType();
                if (fType == BaseItemKind.AUDIO || fType == BaseItemKind.GENRE || fType == BaseItemKind.MUSIC_ALBUM || fType == BaseItemKind.MUSIC_ARTIST || fType == BaseItemKind.MUSIC_GENRE) {
                    return cardWidth;
                } else if (fType == BaseItemKind.COLLECTION_FOLDER && CollectionType.Music.equals(folder.getCollectionType())) {
                    return cardWidth;
                } else {
                    return cardWidth / ImageUtils.ASPECT_RATIO_2_3;
                }
            case THUMB:
                return cardWidth / ImageUtils.ASPECT_RATIO_16_9;
            case BANNER:
                return cardWidth / CardPresenter.ASPECT_RATIO_BANNER;
            default:
                throw new IllegalArgumentException("Unexpected value: " + imageType);
        }
    }

    private void setDefaultGridRowCols(PosterSize posterSize, ImageType imageType) {
        // HINT: use uneven Rows/Cols if possible, so selected middle lines up with TV middle!
        if (mGridPresenter instanceof VerticalGridPresenter) {
            int numCols;
            switch (posterSize) {
                case SMALLEST:
                    numCols = imageType.equals(ImageType.BANNER) ? 6 : imageType.equals(ImageType.THUMB) ? 11 : 15;
                    break;
                case SMALL:
                    numCols = imageType.equals(ImageType.BANNER) ? 5 : imageType.equals(ImageType.THUMB) ? 9 : 13;
                    break;
                case MED:
                    numCols = imageType.equals(ImageType.BANNER) ? 4 : imageType.equals(ImageType.THUMB) ? 7 : 11;
                    break;
                case LARGE:
                    numCols = imageType.equals(ImageType.BANNER) ? 3 : imageType.equals(ImageType.THUMB) ? 5 : 7;
                    break;
                case X_LARGE:
                    numCols = imageType.equals(ImageType.BANNER) ? 2 : imageType.equals(ImageType.THUMB) ? 3 : 5;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mPosterSizeSetting);
            }
            ((VerticalGridPresenter) mGridPresenter).setNumberOfColumns(numCols);
        } else if (mGridPresenter instanceof HorizontalGridPresenter) {
            int numRows;
            switch (posterSize) {
                case SMALLEST:
                    numRows = imageType.equals(ImageType.BANNER) ? 13 : imageType.equals(ImageType.THUMB) ? 7 : 5;
                    break;
                case SMALL:
                    numRows = imageType.equals(ImageType.BANNER) ? 11 : imageType.equals(ImageType.THUMB) ? 6 : 4;
                    break;
                case MED:
                    numRows = imageType.equals(ImageType.BANNER) ? 9 : imageType.equals(ImageType.THUMB) ? 5 : 3;
                    break;
                case LARGE:
                    numRows = imageType.equals(ImageType.BANNER) ? 7 : imageType.equals(ImageType.THUMB) ? 4 : 2;
                    break;
                case X_LARGE:
                    numRows = imageType.equals(ImageType.BANNER) ? 5 : imageType.equals(ImageType.THUMB) ? 2 : 1;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mPosterSizeSetting);
            }
            ((HorizontalGridPresenter) mGridPresenter).setNumberOfRows(numRows);
        }
    }

    private void setAutoCardGridValues() {
        if (mGridPresenter == null) {
            Timber.e("Invalid presenter, cant calculate CardGridValues!");
            return;
        }
        double cardScaling = Math.max(mCardFocusScale - 1.0, 0.0);
        int cardHeightInt = 100;
        int spacingHorizontalInt = 0;
        int spacingVerticalInt = 0;
        int paddingLeftInt = 0;
        int paddingTopInt = 0;
        int numRows = 0;
        int numCols = 0;
        int numCardsScreen = 0; // number of cards visible, including cutoff's

        if (mGridPresenter instanceof HorizontalGridPresenter) {
            numRows = ((HorizontalGridPresenter) mGridPresenter).getNumberOfRows();
            if (numRows == 1) { // reduce size so minimal cards are shown
                numRows = 0;
                numCols = MIN_NUM_CARDS;
            }
        } else if (mGridPresenter instanceof VerticalGridPresenter) {
            numCols = ((VerticalGridPresenter) mGridPresenter).getNumberOfColumns();
        }

        if (numRows > 0) {
            double paddingPct = cardScaling / numRows;
            double spaceingPct = ((paddingPct / 2.0) * CARD_SPACING_PCT) * (numRows - 1);

            double wastedSpacePct = paddingPct + spaceingPct;
            double useableCardSpace = mGridHeight / (1.0 + wastedSpacePct); // decrease size
            double cardHeight = useableCardSpace / numRows;

            // fix any rounding errors and make pixel perfect
            cardHeightInt = (int) Math.round(cardHeight);
            double cardPaddingTopBottomAdj = cardHeightInt * cardScaling;
            spacingVerticalInt = Math.max((int) (Math.round((cardPaddingTopBottomAdj / 2.0) * CARD_SPACING_PCT)), 0); // round spacing
            int paddingTopBottomInt = mGridHeight - ((cardHeightInt * numRows) + (spacingVerticalInt * (numRows - 1)));
            paddingTopInt = Math.max(paddingTopBottomInt / 2, 0);

            int sumSize = (cardHeightInt * numRows) + (spacingVerticalInt * (numRows - 1)) + (paddingTopInt * 2);
            if (Math.abs(sumSize - mGridHeight) > 2) {
                Timber.w("setAutoCardGridValues calculation delta > 2, something is off GridHeight <%s> sumSize <%s>!", mGridHeight, sumSize);
            }
            int cardWidthInt = (int) getCardWidthBy(cardHeightInt, mImageType, mFolder);
            paddingLeftInt = (int) Math.round((cardWidthInt * cardScaling) / 2.0);
            spacingHorizontalInt = Math.max((int) (Math.round(paddingLeftInt * CARD_SPACING_PCT)), 0); // round spacing
            if (mImageType == ImageType.BANNER) {
                spacingHorizontalInt = Math.max((int) (Math.round(paddingLeftInt * CARD_SPACING_HORIZONTAL_BANNER_PCT)), 0); // round spacing
            }
            int cardsCol = (int) Math.round(((double) mGridWidth / (cardWidthInt + spacingHorizontalInt)) + 0.5);
            mCardsScreenEst = numRows * cardsCol;
            mCardsScreenStride = numRows;
        } else if (numCols > 0) {
            double paddingPct = cardScaling / numCols;
            double spaceingPct = ((paddingPct / 2.0) * CARD_SPACING_PCT) * (numCols - 1);
            if (mImageType == ImageType.BANNER) {
                spaceingPct = ((paddingPct / 2.0) * CARD_SPACING_HORIZONTAL_BANNER_PCT) * (numCols - 1);
            }

            double wastedSpacePct = paddingPct + spaceingPct;
            double useableCardSpace = mGridWidth / (1.0 + wastedSpacePct); // decrease size
            double cardWidth = useableCardSpace / numCols;

            // fix any rounding errors and make pixel perfect
            cardHeightInt = (int) Math.round(getCardHeightBy(cardWidth, mImageType, mFolder));
            int cardWidthInt = (int) getCardWidthBy(cardHeightInt, mImageType, mFolder);
            double cardPaddingLeftRightAdj = cardWidthInt * cardScaling;
            spacingHorizontalInt = Math.max((int) (Math.round((cardPaddingLeftRightAdj / 2.0) * CARD_SPACING_PCT)), 0); // round spacing
            if (mImageType == ImageType.BANNER) {
                spacingHorizontalInt = Math.max((int) (Math.round((cardPaddingLeftRightAdj / 2.0) * CARD_SPACING_HORIZONTAL_BANNER_PCT)), 0); // round spacing
            }
            int paddingLeftRightInt = mGridWidth - ((cardWidthInt * numCols) + (spacingHorizontalInt * (numCols - 1)));
            paddingLeftInt = Math.max(paddingLeftRightInt / 2, 0);

            int sumSize = (cardWidthInt * numCols) + (spacingHorizontalInt * (numCols - 1)) + (paddingLeftInt * 2);
            if (Math.abs(sumSize - mGridWidth) > 2) {
                Timber.w("setAutoCardGridValues calculation delta > 2, something is off GridWidth <%s> sumSize <%s>!", mGridWidth, sumSize);
            }
            paddingTopInt = (int) Math.round((cardHeightInt * cardScaling) / 2.0);
            spacingVerticalInt = Math.max((int) (Math.round(paddingTopInt * CARD_SPACING_PCT)), 0); // round spacing
            int cardsRow = (int) Math.round(((double) mGridHeight / (cardHeightInt + spacingVerticalInt)) + 0.5);
            mCardsScreenEst = numCols * cardsRow;
            mCardsScreenStride = numCols;
        }

        Timber.d("numCardsScreen <%s>", numCardsScreen);

        if (mCardHeight != cardHeightInt) {
            mDirty = true;
        }
        mCardHeight = cardHeightInt;
        mGridItemSpacingHorizontal = spacingHorizontalInt;
        mGridItemSpacingVertical = spacingVerticalInt;
        mGridPaddingLeft = paddingLeftInt;
        mGridPaddingTop = paddingTopInt;
    }

    private void setupQueries() {
        StdItemQuery query = new StdItemQuery(new ItemFields[]{
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount,
                ItemFields.MediaSources,
                ItemFields.MediaStreams,
                ItemFields.DisplayPreferencesId
        });
        query.setParentId(mParentId.toString());
        if (mFolder.getType() == BaseItemKind.USER_VIEW || mFolder.getType() == BaseItemKind.COLLECTION_FOLDER) {
            String type = mFolder.getCollectionType() != null ? mFolder.getCollectionType().toLowerCase() : "";
            switch (type) {
                case CollectionType.Movies:
                    query.setIncludeItemTypes(new String[]{"Movie"});
                    query.setRecursive(true);
                    break;
                case CollectionType.TvShows:
                    query.setIncludeItemTypes(new String[]{"Series"});
                    query.setRecursive(true);
                    break;
                case CollectionType.BoxSets:
                    query.setIncludeItemTypes(new String[]{"BoxSet"});
                    query.setParentId(null);
                    query.setRecursive(true);
                    break;
                case CollectionType.Music:
                    //Special queries needed for album artists
                    String includeType = requireActivity().getIntent().getStringExtra(Extras.IncludeType);
                    if ("AlbumArtist".equals(includeType)) {
                        ArtistsQuery albumArtists = new ArtistsQuery();
                        albumArtists.setUserId(userRepository.getValue().getCurrentUser().getValue().getId().toString());
                        albumArtists.setFields(new ItemFields[]{
                                ItemFields.PrimaryImageAspectRatio,
                                ItemFields.ItemCounts,
                                ItemFields.ChildCount
                        });
                        albumArtists.setParentId(mParentId.toString());
                        setRowDef(new BrowseRowDef("", albumArtists, CHUNK_SIZE_MINIMUM, new ChangeTriggerType[]{}));
                        return;
                    }
                    query.setIncludeItemTypes(new String[]{includeType != null ? includeType : "MusicAlbum"});
                    query.setRecursive(true);
                    break;
            }
        }

        setRowDef(new BrowseRowDef("", query, CHUNK_SIZE_MINIMUM, false, true));
    }

    @Override
    public void onResume() {
        super.onResume();

        PosterSize posterSizeSetting = libraryPreferences.get(LibraryPreferences.Companion.getPosterSize());
        ImageType imageType = libraryPreferences.get(LibraryPreferences.Companion.getImageType());
        GridDirection gridDirection = libraryPreferences.get(LibraryPreferences.Companion.getGridDirection());


        if (mImageType != imageType || mPosterSizeSetting != posterSizeSetting || mGridDirection != gridDirection || mDirty) {
            determiningPosterSize = true;

            mImageType = imageType;
            mPosterSizeSetting = posterSizeSetting;
            mGridDirection = gridDirection;

            if (mGridDirection.equals(GridDirection.VERTICAL) && (mGridPresenter == null || !(mGridPresenter instanceof VerticalGridPresenter))) {
                setGridPresenter(new VerticalGridPresenter());
            } else if (mGridDirection.equals(GridDirection.HORIZONTAL) && (mGridPresenter == null || !(mGridPresenter instanceof HorizontalGridPresenter))) {
                setGridPresenter(new HorizontalGridPresenter());
            }
            setDefaultGridRowCols(mPosterSizeSetting, mImageType);
            setAutoCardGridValues();
            createGrid();
            loadGrid();
            determiningPosterSize = false;
        }

        if (!justLoaded) {
            //Re-retrieve anything that needs it but delay slightly so we don't take away gui landing
            if (mAdapter != null) {
                mHandler.postDelayed(() -> {
                    if (mActivity != null && mActivity.isFinishing()) return;
                    if (getActivity() != null && getActivity().isFinishing()) return;
                    if (mAdapter != null && mAdapter.size() > 0) {
                        if (!mAdapter.ReRetrieveIfNeeded()) {
                            refreshCurrentItem();
                        }
                    }
                }, 500);
            }
        } else {
            justLoaded = false;
        }
    }

    private void buildAdapter() {
        mCardPresenter = new CardPresenter(false, mImageType, mCardHeight);
        mCardPresenter.setUniformAspect(true); // make grid layouts always uniform

        Timber.d("buildAdapter cardHeight <%s> getCardWidthBy <%s> chunks <%s> type <%s>", mCardHeight, (int) getCardWidthBy(mCardHeight, mImageType, mFolder), mRowDef.getChunkSize(), mRowDef.getQueryType().toString());

        // adapt chunk size if needed
        int chunkSize = mRowDef.getChunkSize();
        if (mCardsScreenEst > 0 && mCardsScreenEst >= chunkSize) {
            chunkSize = Math.min(mCardsScreenEst + mCardsScreenStride, 150); // cap at 150
            Timber.d("buildAdapter adjusting chunkSize to <%s> screenEst <%s>", chunkSize, mCardsScreenEst);
        }

        switch (mRowDef.getQueryType()) {
            case NextUp:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getNextUpQuery(), true, mCardPresenter, null);
                break;
            case Season:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getSeasonQuery(), mCardPresenter, null);
                break;
            case Upcoming:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getUpcomingQuery(), mCardPresenter, null);
                break;
            case Views:
                mAdapter = new ItemRowAdapter(requireContext(), new ViewQuery(), mCardPresenter, null);
                break;
            case SimilarSeries:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getSimilarQuery(), QueryType.SimilarSeries, mCardPresenter, null);
                break;
            case SimilarMovies:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getSimilarQuery(), QueryType.SimilarMovies, mCardPresenter, null);
                break;
            case Persons:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getPersonsQuery(), chunkSize, mCardPresenter, null);
                break;
            case LiveTvChannel:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getTvChannelQuery(), 40, mCardPresenter, null);
                break;
            case LiveTvProgram:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getProgramQuery(), mCardPresenter, null);
                break;
            case LiveTvRecording:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getRecordingQuery(), chunkSize, mCardPresenter, null);
                break;
            case AlbumArtists:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getArtistsQuery(), chunkSize, mCardPresenter, null);
                break;
            default:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getQuery(), chunkSize, mRowDef.getPreferParentThumb(), mRowDef.isStaticHeight(), mCardPresenter, null);
                break;
        }
        mDirty = false;

        FilterOptions filters = new FilterOptions();
        filters.setFavoriteOnly(libraryPreferences.get(LibraryPreferences.Companion.getFilterFavoritesOnly()));
        filters.setUnwatchedOnly(libraryPreferences.get(LibraryPreferences.Companion.getFilterUnwatchedOnly()));

        mAdapter.setRetrieveFinishedListener(new EmptyResponse() {
            @Override
            public void onResponse() {
                setStatusText(mFolder.getName());
                if (mCurrentItem == null) { // don't mess-up pos via loadMoreItemsIfNeeded
                    setItem(null);
                    updateCounter(mAdapter.getTotalItems() > 0 ? 1 : 0);
                }
                mLetterButton.setVisibility(ItemSortBy.SortName.equals(mAdapter.getSortBy()) ? View.VISIBLE : View.GONE);
                if (mAdapter.getTotalItems() == 0) {
                    binding.toolBar.requestFocus();
                    mHandler.postDelayed(() -> binding.title.setText(mFolder.getName()), 500);
                } else {
                    if (mGridView != null) mGridView.requestFocus();
                }
            }
        });
        mAdapter.setFilters(filters);

        updateAdapter();
    }

    public void loadGrid() {
        if (mCardPresenter == null || mAdapter == null || mDirty) {
            buildAdapter();
        }

        mAdapter.setSortBy(getSortOption(libraryPreferences.get(LibraryPreferences.Companion.getSortBy())));
        mAdapter.Retrieve();
    }

    private ImageButton mSortButton;
    private ImageButton mSettingsButton;
    private ImageButton mUnwatchedButton;
    private ImageButton mFavoriteButton;
    private ImageButton mLetterButton;

    private void updateDisplayPrefs() {
        libraryPreferences.set(LibraryPreferences.Companion.getFilterFavoritesOnly(), mAdapter.getFilters().isFavoriteOnly());
        libraryPreferences.set(LibraryPreferences.Companion.getFilterUnwatchedOnly(), mAdapter.getFilters().isUnwatchedOnly());
        libraryPreferences.set(LibraryPreferences.Companion.getSortBy(), mAdapter.getSortBy());
        libraryPreferences.set(LibraryPreferences.Companion.getSortOrder(), getSortOption(mAdapter.getSortBy()).order);
        CoroutineUtils.runBlocking((coroutineScope, continuation) -> libraryPreferences.commit(continuation));
    }

    private void addTools() {
        //Add tools
        int size = Utils.convertDpToPixel(requireContext(), 26);

        mSortButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mSortButton.setImageResource(R.drawable.ic_sort);
        mSortButton.setMaxHeight(size);
        mSortButton.setAdjustViewBounds(true);
        mSortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create sort menu
                PopupMenu sortMenu = new PopupMenu(getActivity(), binding.toolBar, Gravity.END);
                for (Integer key : sortOptions.keySet()) {
                    SortOption option = sortOptions.get(key);
                    if (option == null) option = sortOptions.get(0);
                    MenuItem item = sortMenu.getMenu().add(0, key, key, Objects.requireNonNull(option).name);
                    if (option.value.equals(libraryPreferences.get(LibraryPreferences.Companion.getSortBy())))
                        item.setChecked(true);
                }
                sortMenu.getMenu().setGroupCheckable(0, true, true);
                sortMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mAdapter.setSortBy(Objects.requireNonNull(sortOptions.get(item.getItemId())));
                        mAdapter.Retrieve();
                        item.setChecked(true);
                        updateDisplayPrefs();
                        return true;
                    }
                });
                sortMenu.show();
            }
        });
        mSortButton.setContentDescription(getString(R.string.lbl_sort_by));

        binding.toolBar.addView(mSortButton);

        if (mRowDef.getQueryType() == QueryType.Items) {
            mUnwatchedButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
            mUnwatchedButton.setImageResource(R.drawable.ic_unwatch);
            mUnwatchedButton.setActivated(mAdapter.getFilters().isUnwatchedOnly());
            mUnwatchedButton.setMaxHeight(size);
            mUnwatchedButton.setAdjustViewBounds(true);
            mUnwatchedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FilterOptions filters = mAdapter.getFilters();
                    if (filters == null) filters = new FilterOptions();

                    filters.setUnwatchedOnly(!filters.isUnwatchedOnly());
                    mUnwatchedButton.setActivated(filters.isUnwatchedOnly());
                    mAdapter.setFilters(filters);
                    mAdapter.Retrieve();
                    updateDisplayPrefs();
                }
            });
            mUnwatchedButton.setContentDescription(getString(R.string.lbl_unwatched));
            binding.toolBar.addView(mUnwatchedButton);
        }

        mFavoriteButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mFavoriteButton.setImageResource(R.drawable.ic_heart);
        mFavoriteButton.setActivated(mAdapter.getFilters().isFavoriteOnly());
        mFavoriteButton.setMaxHeight(size);
        mFavoriteButton.setAdjustViewBounds(true);
        mFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilterOptions filters = mAdapter.getFilters();
                if (filters == null) filters = new FilterOptions();

                filters.setFavoriteOnly(!filters.isFavoriteOnly());
                mFavoriteButton.setActivated(filters.isFavoriteOnly());
                mAdapter.setFilters(filters);
                mAdapter.Retrieve();
                updateDisplayPrefs();
            }
        });
        mFavoriteButton.setContentDescription(getString(R.string.lbl_favorite));
        binding.toolBar.addView(mFavoriteButton);

        JumplistPopup jumplistPopup = new JumplistPopup();
        mLetterButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mLetterButton.setImageResource(R.drawable.ic_jump_letter);
        mLetterButton.setMaxHeight(size);
        mLetterButton.setAdjustViewBounds(true);
        mLetterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open letter jump popup
                jumplistPopup.show();
            }
        });
        mLetterButton.setContentDescription(getString(R.string.lbl_by_letter));
        binding.toolBar.addView(mLetterButton);

        mSettingsButton = new ImageButton(requireContext(), null, 0, R.style.Button_Icon);
        mSettingsButton.setImageResource(R.drawable.ic_settings);
        mSettingsButton.setMaxHeight(size);
        mSettingsButton.setAdjustViewBounds(true);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(getActivity(), PreferencesActivity.class);
                settingsIntent.putExtra(PreferencesActivity.EXTRA_SCREEN, DisplayPreferencesScreen.class.getCanonicalName());
                Bundle screenArgs = new Bundle();
                screenArgs.putString(DisplayPreferencesScreen.ARG_PREFERENCES_ID, mFolder.getDisplayPreferencesId());
                screenArgs.putBoolean(DisplayPreferencesScreen.ARG_ALLOW_VIEW_SELECTION, userViewsRepository.getValue().allowViewSelection(mFolder.getCollectionType()));
                settingsIntent.putExtra(PreferencesActivity.EXTRA_SCREEN_ARGS, screenArgs);
                requireActivity().startActivity(settingsIntent);
            }
        });
        mSettingsButton.setContentDescription(getString(R.string.lbl_settings));
        binding.toolBar.addView(mSettingsButton);
    }

    class JumplistPopup {
        private final int WIDTH = Utils.convertDpToPixel(requireContext(), 900);
        private final int HEIGHT = Utils.convertDpToPixel(requireContext(), 55);

        private final PopupWindow popupWindow;
        private final AlphaPickerView alphaPicker;

        JumplistPopup() {
            PopupEmptyBinding layout = PopupEmptyBinding.inflate(getLayoutInflater(), binding.rowsFragment, false);
            popupWindow = new PopupWindow(layout.emptyPopup, WIDTH, HEIGHT, true);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setAnimationStyle(R.style.WindowAnimation_SlideTop);

            alphaPicker = new AlphaPickerView(requireContext(), null);
            alphaPicker.setOnAlphaSelected(letter -> {
                mAdapter.setStartLetter(letter.toString());
                loadGrid();
                dismiss();
                return null;
            });

            layout.emptyPopup.addView(alphaPicker);
        }

        public void show() {
            popupWindow.showAtLocation(binding.rowsFragment, Gravity.TOP, binding.rowsFragment.getLeft(), binding.rowsFragment.getTop());
            if (mAdapter.getStartLetter() != null && !mAdapter.getStartLetter().isEmpty()) {
                alphaPicker.focus(mAdapter.getStartLetter().charAt(0));
            }
        }

        public void dismiss() {
            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
        }
    }

    private void setupEventListeners() {
        if (mGridPresenter != null) {
            if (mGridPresenter instanceof HorizontalGridPresenter)
                ((HorizontalGridPresenter) mGridPresenter).setOnItemViewClickedListener(mClickedListener);
            else if (mGridPresenter instanceof VerticalGridPresenter)
                ((VerticalGridPresenter) mGridPresenter).setOnItemViewClickedListener(mClickedListener);
        }
        mClickedListener.registerListener(new ItemViewClickedListener());
        mSelectedListener.registerListener(new ItemViewSelectedListener());

        if (mActivity != null) {
            mActivity.registerKeyListener(new KeyListener() {
                @Override
                public boolean onKeyUp(int key, KeyEvent event) {
                    if (key == KeyEvent.KEYCODE_MEDIA_PLAY || key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                        mediaManager.getValue().setCurrentMediaAdapter(mAdapter);
                        mediaManager.getValue().setCurrentMediaPosition(mCurrentItem.getIndex());
                        mediaManager.getValue().setCurrentMediaTitle(mFolder.getName());
                    }
                    return KeyProcessor.HandleKey(key, mCurrentItem, mActivity);
                }
            });

            mActivity.registerMessageListener(new MessageListener() {
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
        if (mediaManager.getValue().getCurrentMediaPosition() >= 0) {
            mCurrentItem = mediaManager.getValue().getCurrentMediaItem();

            if (mGridPresenter instanceof HorizontalGridPresenter)
                ((HorizontalGridPresenter) mGridPresenter).setPosition(mediaManager.getValue().getCurrentMediaPosition());
            // Don't do anything for vertical grids as the presenter does not allow setting the position

            mediaManager.getValue().setCurrentMediaPosition(-1); // re-set so it doesn't mess with parent views
        }
        if (mCurrentItem != null && mCurrentItem.getBaseItemType() != BaseItemType.Photo && mCurrentItem.getBaseItemType() != BaseItemType.PhotoAlbum
                && mCurrentItem.getBaseItemType() != BaseItemType.MusicArtist && mCurrentItem.getBaseItemType() != BaseItemType.MusicAlbum) {
            Timber.d("Refresh item \"%s\"", mCurrentItem.getFullName(requireContext()));
            mCurrentItem.refresh(new EmptyResponse() {
                @Override
                public void onResponse() {

                    mAdapter.notifyArrayItemRangeChanged(mAdapter.indexOf(mCurrentItem), 1);
                    //Now - if filtered make sure we still pass
                    if (mAdapter.getFilters() != null) {
                        if ((mAdapter.getFilters().isFavoriteOnly() && !mCurrentItem.isFavorite()) || (mAdapter.getFilters().isUnwatchedOnly() && mCurrentItem.isPlayed())) {
                            //if we are about to remove last item, throw focus to toolbar so framework doesn't crash
                            if (mAdapter.size() == 1) binding.toolBar.requestFocus();
                            mAdapter.remove(mCurrentItem);
                            mAdapter.setTotalItems(mAdapter.getTotalItems() - 1);
                            updateCounter(mCurrentItem.getIndex());
                        }
                    }
                }
            });
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            ItemLauncher.launch((BaseRowItem) item, mAdapter, ((BaseRowItem) item).getIndex(), getActivity());
        }
    }

    private final Runnable mDelayedSetItem = new Runnable() {
        @Override
        public void run() {
            backgroundService.getValue().setBackground(mCurrentItem.getBaseItem());
            setItem(mCurrentItem);
        }
    };

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            mHandler.removeCallbacks(mDelayedSetItem);
            if (!(item instanceof BaseRowItem)) {
                mCurrentItem = null;
                binding.title.setText(mainTitle);
                //fill in default background
                backgroundService.getValue().clearBackgrounds();
            } else {
                mCurrentItem = (BaseRowItem) item;
                binding.title.setText(mCurrentItem.getName(requireContext()));
                binding.infoRow.removeAllViews();
                mHandler.postDelayed(mDelayedSetItem, VIEW_SELECT_UPDATE_DELAY);

                if (!determiningPosterSize)
                    mAdapter.loadMoreItemsIfNeeded(mCurrentItem.getIndex());
            }
        }
    }
}
