package org.jellyfin.androidtv.ui.livetv;

import static org.koin.java.KoinJavaComponent.inject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.repository.CustomMessageRepository;
import org.jellyfin.androidtv.databinding.LiveTvGuideBinding;
import org.jellyfin.androidtv.ui.AsyncImageView;
import org.jellyfin.androidtv.ui.FriendlyDateButton;
import org.jellyfin.androidtv.ui.GuideChannelHeader;
import org.jellyfin.androidtv.ui.GuidePagingButton;
import org.jellyfin.androidtv.ui.HorizontalScrollViewListener;
import org.jellyfin.androidtv.ui.LiveProgramDetailPopup;
import org.jellyfin.androidtv.ui.ObservableHorizontalScrollView;
import org.jellyfin.androidtv.ui.ObservableScrollView;
import org.jellyfin.androidtv.ui.ProgramGridCell;
import org.jellyfin.androidtv.ui.ScrollViewListener;
import org.jellyfin.androidtv.ui.navigation.Destinations;
import org.jellyfin.androidtv.ui.navigation.NavigationRepository;
import org.jellyfin.androidtv.util.CoroutineUtils;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.TextUtilsKt;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.EmptyLifecycleAwareResponse;
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.koin.java.KoinJavaComponent;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import kotlin.Lazy;
import timber.log.Timber;

public class LiveTvGuideFragment extends Fragment implements LiveTvGuide, View.OnKeyListener {
    public static final int GUIDE_ROW_HEIGHT_DP = 55;
    public static final int GUIDE_ROW_WIDTH_PER_MIN_DP = 7;
    public static final int PAGE_SIZE = 75;
    public static final int NORMAL_HOURS = 9;
    public static final int FILTERED_HOURS = 4;

    private TextView mDisplayDate;
    private TextView mTitle;
    private TextView mChannelStatus;
    private TextView mFilterStatus;
    private TextView mSummary;
    private AsyncImageView mImage;
    private LinearLayout mInfoRow;
    private LinearLayout mChannels;
    private LinearLayout mTimeline;
    private LinearLayout mProgramRows;
    private ObservableScrollView mChannelScroller;
    private HorizontalScrollView mTimelineScroller;
    private View mSpinner;
    private View mResetButton;

    private BaseItemDto mSelectedProgram;
    private RelativeLayout mSelectedProgramView;

    private List<ChannelInfoDto> mAllChannels;
    private String mFirstFocusChannelId;
    private boolean focusAtEnd;
    private GuideFilters mFilters = new GuideFilters();

    private Calendar mCurrentGuideStart;
    private Calendar mCurrentGuideEnd;
    private long mCurrentLocalGuideStart = System.currentTimeMillis();
    private long mCurrentLocalGuideEnd;
    private int mCurrentDisplayChannelStartNdx = 0;
    private int mCurrentDisplayChannelEndNdx = 0;

    private int guideRowHeightPx;
    private int guideRowWidthPerMinPx;

    private Handler mHandler = new Handler();

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private final Lazy<CustomMessageRepository> customMessageRepository = inject(CustomMessageRepository.class);
    private final Lazy<NavigationRepository> navigationRepository = inject(NavigationRepository.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        guideRowHeightPx = Utils.convertDpToPixel(requireContext(), GUIDE_ROW_HEIGHT_DP);
        guideRowWidthPerMinPx = Utils.convertDpToPixel(requireContext(), GUIDE_ROW_WIDTH_PER_MIN_DP);

        LiveTvGuideBinding binding = LiveTvGuideBinding.inflate(getLayoutInflater(), container, false);

        mDisplayDate = binding.displayDate;
        mTitle = binding.title;
        mSummary = binding.summary;
        mChannelStatus = binding.channelsStatus;
        mFilterStatus = binding.filterStatus;
        mChannelStatus.setTextColor(Color.GRAY);
        mFilterStatus.setTextColor(Color.GRAY);
        mInfoRow = binding.infoRow;
        mImage = binding.programImage;
        mChannels = binding.channels;
        mTimeline = binding.timeline;
        mProgramRows = binding.programRows;
        mSpinner = binding.spinner;
        mSpinner.setVisibility(View.VISIBLE);

        View mFilterButton = binding.filterButton;
        mFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterOptions();
            }
        });
        mFilterButton.setContentDescription(getString(R.string.lbl_filters));

        View mOptionsButton = binding.optionsButton;
        mOptionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptions();
            }
        });
        mOptionsButton.setContentDescription(getString(R.string.lbl_other_options));

        View mDateButton = binding.dateButton;
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });
        mDateButton.setContentDescription(getString(R.string.lbl_select_date));

        mResetButton = binding.resetButton;
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageGuideTo(System.currentTimeMillis());
            }
        });

        mProgramRows.setFocusable(false);
        mChannelScroller = binding.channelScroller;
        ObservableScrollView programVScroller = binding.programVScroller;
        programVScroller.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                mChannelScroller.scrollTo(x, y);
            }
        });
        mChannelScroller.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                programVScroller.scrollTo(x, y);
            }
        });

        mTimelineScroller = binding.timelineHScroller;
        mTimelineScroller.setFocusable(false);
        mTimelineScroller.setFocusableInTouchMode(false);
        mTimeline.setFocusable(false);
        mTimeline.setFocusableInTouchMode(false);
        mChannelScroller.setFocusable(false);
        mChannelScroller.setFocusableInTouchMode(false);
        ObservableHorizontalScrollView programHScroller = binding.programHScroller;
        programHScroller.setScrollViewListener(new HorizontalScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableHorizontalScrollView scrollView, int x, int y, int oldx, int oldy) {
                mTimelineScroller.scrollTo(x, y);
            }
        });

        programHScroller.setFocusable(false);
        programHScroller.setFocusableInTouchMode(false);

        mChannels.setFocusable(false);
        mChannelScroller.setFocusable(false);

        // Register to receive message from popup
        CoroutineUtils.readCustomMessagesOnLifecycle(getLifecycle(), customMessageRepository.getValue(), message -> {
            if (message.equals(CustomMessage.ActionComplete.INSTANCE)) dismissProgramOptions();
            return null;
        });

        return binding.getRoot();
    }

    private int getGuideHours() {
        return mFilters.any() ? FILTERED_HOURS : NORMAL_HOURS;
    }

    private void load() {
        fillTimeLine(mCurrentLocalGuideStart, getGuideHours());
        TvManager.loadAllChannels(new LifecycleAwareResponse<Integer>(getLifecycle()) {
            @Override
            public void onResponse(Integer ndx) {
                if (!getActive()) return;

                if (ndx  >= PAGE_SIZE) {
                    // last channel is not in first page so grab a set where it will be in the middle
                    ndx = ndx - (PAGE_SIZE / 2);
                } else {
                    ndx = 0; // just start at beginning
                }

                mAllChannels = TvManager.getAllChannels();
                if (mAllChannels.size() > 0) {
                    displayChannels(ndx, PAGE_SIZE);
                } else {
                    mSpinner.setVisibility(View.GONE);
                }
            }
        });
    }

    public void refreshFavorite(String channelId){
        for (int i = 0; i < mChannels.getChildCount(); i++) {
            GuideChannelHeader gch = (GuideChannelHeader)mChannels.getChildAt(i);
            if (gch.getChannel().getId().equals(channelId))
                gch.refreshFavorite();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mFilters.load();
        doLoad();
    }

    protected void doLoad() {
        if (TvManager.shouldForceReload() || System.currentTimeMillis() >= mCurrentLocalGuideStart + 1800000  || mChannels.getChildCount() == 0) {
            load();

            mFirstFocusChannelId = TvManager.getLastLiveTvChannel();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mDisplayProgramsTask != null) {
            mDisplayProgramsTask.cancel(true);
        }
        if (mDetailPopup != null) {
            mDetailPopup.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mCurrentLocalGuideStart > System.currentTimeMillis()) {
            TvManager.forceReload(); //we paged ahead - force a re-load if we come back in
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) return onKeyUp(keyCode, event);
        else if (event.getAction() == KeyEvent.ACTION_DOWN && event.isLongPress()) return onKeyLongPress(keyCode);
        else if (event.getAction() == KeyEvent.ACTION_DOWN) return onKeyDown(keyCode, event);
        return false;
    }

    private boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                event.startTracking();
                return true;
        }
        return false;
    }

    private boolean onKeyLongPress(int keyCode) {
        switch (keyCode){
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mSelectedProgramView instanceof ProgramGridCell)
                    showProgramOptions();
                else if(mSelectedProgramView instanceof GuideChannelHeader)
                    toggleFavorite();
                return true;
        }
        return false;
    }

    private boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                // bring up filter selection
                showFilterOptions();
                break;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if ((event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0) {
                    Date curUTC = TimeUtils.convertToUtcDate(new Date());
                    if (mSelectedProgramView instanceof ProgramGridCell) {
                        if (mSelectedProgram.getStartDate().before(curUTC))
                            PlaybackHelper.retrieveAndPlay(mSelectedProgram.getChannelId(), false, requireContext());
                        else
                            showProgramOptions();
                        return true;
                    }
                }
                return false;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if ((mDetailPopup == null || !mDetailPopup.isShowing())
                        && mSelectedProgram != null
                        && mSelectedProgram.getChannelId() != null) {
                    // tune to the current channel
                    PlaybackHelper.retrieveAndPlay(mSelectedProgram.getChannelId(), false, requireContext());
                    return true;
                }

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (requireActivity().getCurrentFocus() instanceof ProgramGridCell
                        && mSelectedProgramView != null
                        && ((ProgramGridCell)mSelectedProgramView).isLast()) {
                    requestGuidePage(mCurrentLocalGuideEnd);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (requireActivity().getCurrentFocus() instanceof ProgramGridCell
                        && mSelectedProgramView != null
                        && ((ProgramGridCell)mSelectedProgramView).isFirst()
                        && TimeUtils.convertToLocalDate(mSelectedProgram.getStartDate()).getTime() > System.currentTimeMillis()) {
                    focusAtEnd = true;
                    requestGuidePage(mCurrentLocalGuideStart - (getGuideHours()*60*60000));
                }
                break;
        }

        return false;
    }

    View.OnClickListener datePickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            pageGuideTo(((FriendlyDateButton)v).getDate());
            dateDialog.dismiss();
        }
    };

    private void toggleFavorite() {
        GuideChannelHeader header = (GuideChannelHeader)mSelectedProgramView;
        UserItemDataDto data = header.getChannel().getUserData();
        if (data != null) {
            apiClient.getValue().UpdateFavoriteStatusAsync(header.getChannel().getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), !data.getIsFavorite(), new LifecycleAwareResponse<UserItemDataDto>(getLifecycle()) {
                @Override
                public void onResponse(UserItemDataDto response) {
                    if (!getActive()) return;

                    header.getChannel().setUserData(response);
                    header.findViewById(R.id.favImage).setVisibility(response.getIsFavorite() ? View.VISIBLE : View.GONE);
                    DataRefreshService dataRefreshService = KoinJavaComponent.<DataRefreshService>get(DataRefreshService.class);
                    dataRefreshService.setLastFavoriteUpdate(System.currentTimeMillis());
                }
            });
        }
    }

    AlertDialog dateDialog;

    private void showDatePicker() {
        FrameLayout scrollPane = (FrameLayout) getLayoutInflater().inflate(R.layout.horizontal_scroll_pane, null);
        LinearLayout scrollItems = scrollPane.findViewById(R.id.scrollItems);
        for (long increment = 0; increment < 15; increment++) {
            scrollItems.addView(new FriendlyDateButton(requireContext(), System.currentTimeMillis() + (increment * 86400000), datePickedListener));
        }

        dateDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.lbl_select_date)
                .setView(scrollPane)
                .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();

    }

    private void requestGuidePage(final long startTime) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.lbl_load_guide_data)
                .setMessage(startTime > mCurrentLocalGuideStart ? getString(R.string.msg_live_tv_next, getGuideHours()) : getString(R.string.msg_live_tv_prev, getGuideHours()))
                .setPositiveButton(R.string.lbl_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pageGuideTo(startTime);
                    }
                })
                .setNegativeButton(R.string.lbl_no, null)
                .show();
    }

    private void pageGuideTo(long startTime) {
        if (startTime < System.currentTimeMillis()) startTime = System.currentTimeMillis(); // don't allow the past
        Timber.i("page to %s", (new Date(startTime)).toString());
        TvManager.forceReload(); // don't allow cache
        if (mSelectedProgram != null) {
            mFirstFocusChannelId = mSelectedProgram.getChannelId();
        }
        fillTimeLine(startTime, getGuideHours());
        loadProgramData();
    }

    private LiveProgramDetailPopup mDetailPopup;

    public void dismissProgramOptions() {
        if (mDetailPopup != null) {
            mDetailPopup.dismiss();
        }
    }

    public void showProgramOptions() {
        if (mSelectedProgram == null) return;
        if (mDetailPopup == null) {
            mDetailPopup = new LiveProgramDetailPopup(requireActivity(), getLifecycle(), this, mSummary.getWidth()+20, new EmptyLifecycleAwareResponse(getLifecycle()) {
                @Override
                public void onResponse() {
                    if (!getActive()) return;

                    PlaybackHelper.retrieveAndPlay(mSelectedProgram.getChannelId(), false, requireContext());
                }
            });
        }

        mDetailPopup.setContent(mSelectedProgram, ((ProgramGridCell)mSelectedProgramView));
        mDetailPopup.show(mImage, mTitle.getLeft(), mTitle.getTop() - 10);
    }

    public void showFilterOptions() {
        navigationRepository.getValue().navigate(Destinations.INSTANCE.getLiveTvGuideFilterPreferences());
        TvManager.forceReload();
    }

    public void showOptions() {
        navigationRepository.getValue().navigate(Destinations.INSTANCE.getLiveTvGuideOptionPreferences());
        TvManager.forceReload();
    }

    public void displayChannels(int start, int max) {
        int end = start + max;
        if (end > mAllChannels.size()) {
            end = mAllChannels.size();
        }

        if (mFilters.any()) {
            // if we are filtered, then we need to get programs for all channels
            mCurrentDisplayChannelStartNdx = 0;
            mCurrentDisplayChannelEndNdx = mAllChannels.size()-1;
        } else {
            mCurrentDisplayChannelStartNdx = start;
            mCurrentDisplayChannelEndNdx = end - 1;
        }
        Timber.d("*** Display channels pre-execute");
        mSpinner.setVisibility(View.VISIBLE);

        loadProgramData();
    }

    private void loadProgramData() {
        mProgramRows.removeAllViews();
        mChannels.removeAllViews();
        mChannelStatus.setText("");
        mFilterStatus.setText("");
        TvManager.getProgramsAsync(mCurrentDisplayChannelStartNdx, mCurrentDisplayChannelEndNdx, mCurrentGuideStart, mCurrentGuideEnd, new EmptyLifecycleAwareResponse(getLifecycle()) {
            @Override
            public void onResponse() {
                if (!getActive()) return;

                Timber.d("*** Programs response");
                if (mDisplayProgramsTask != null) mDisplayProgramsTask.cancel(true);
                mDisplayProgramsTask = new DisplayProgramsTask();
                mDisplayProgramsTask.execute(mCurrentDisplayChannelStartNdx, mCurrentDisplayChannelEndNdx);
            }
        });
    }

    DisplayProgramsTask mDisplayProgramsTask;
    class DisplayProgramsTask extends AsyncTask<Integer, Integer, Void> {

        View firstFocusView;
        int displayedChannels = 0;

        @Override
        protected void onPreExecute() {
            Timber.d("*** Display programs pre-execute");
            mChannels.removeAllViews();
            mProgramRows.removeAllViews();

            if (mCurrentDisplayChannelStartNdx > 0) {
                // Show a paging row for channels above
                int pageUpStart = mCurrentDisplayChannelStartNdx - PAGE_SIZE;
                if (pageUpStart < 0) {
                    pageUpStart = 0;
                }

                TextView placeHolder = new TextView(requireContext());
                placeHolder.setHeight(guideRowHeightPx);
                mChannels.addView(placeHolder);
                displayedChannels = 0;

                String label = TextUtilsKt.getLoadChannelsLabel(requireContext(), mAllChannels.get(pageUpStart).getNumber(), mAllChannels.get(mCurrentDisplayChannelStartNdx - 1).getNumber());
                mProgramRows.addView(new GuidePagingButton(requireContext(), LiveTvGuideFragment.this, pageUpStart, label));
            }
        }

        @Override
        protected Void doInBackground(Integer... params) {
            int start = params[0];
            int end = params[1];
            boolean first = true;

            Timber.d("*** About to iterate programs");
            LinearLayout prevRow = null;
            for (int i = start; i <= end; i++) {
                if (isCancelled()) return null;
                final ChannelInfoDto channel = TvManager.getChannel(i);
                List<BaseItemDto> programs = TvManager.getProgramsForChannel(channel.getId(), mFilters);
                final LinearLayout row = getProgramRow(programs, channel.getId());
                if (row == null) continue; // no row to show

                if (first) {
                    first = false;
                    firstFocusView = row;
                }

                // set focus parameters if we are not on first row
                // this makes focus movements more predictable for the grid view
                if (prevRow != null) {
                    TvManager.setFocusParms(row, prevRow, true);
                    TvManager.setFocusParms(prevRow, row, false);
                }
                prevRow = row;

                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

                        GuideChannelHeader header = getChannelHeader(requireContext(), channel);
                        mChannels.addView(header);
                        header.loadImage();
                        mProgramRows.addView(row);
                        // put focus on the last tuned channel
                        if (channel.getId().equals(mFirstFocusChannelId)) {
                            firstFocusView = focusAtEnd ? row.getChildAt(row.getChildCount()-1) : row;
                            focusAtEnd = false;
                            mFirstFocusChannelId = null;
                        }

                    }
                });

                displayedChannels++;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Timber.d("*** Display programs post execute");
            if (mCurrentDisplayChannelEndNdx < mAllChannels.size()-1 && !mFilters.any()) {
                // Show a paging row for channels below
                int pageDnEnd = mCurrentDisplayChannelEndNdx + PAGE_SIZE;
                if (pageDnEnd >= mAllChannels.size()) pageDnEnd = mAllChannels.size()-1;

                TextView placeHolder = new TextView(requireContext());
                placeHolder.setHeight(guideRowHeightPx);
                mChannels.addView(placeHolder);

                String label = TextUtilsKt.getLoadChannelsLabel(requireContext(), mAllChannels.get(mCurrentDisplayChannelEndNdx + 1).getNumber(), mAllChannels.get(pageDnEnd).getNumber());
                mProgramRows.addView(new GuidePagingButton(requireContext(), LiveTvGuideFragment.this, mCurrentDisplayChannelEndNdx + 1, label));
            }

            mChannelStatus.setText(displayedChannels+" of "+mAllChannels.size()+" channels");
            mFilterStatus.setText(mFilters.toString() + " for "+getGuideHours()+" hours");
            mFilterStatus.setTextColor(mFilters.any() ? Color.WHITE : Color.GRAY);

            mResetButton.setVisibility(mCurrentLocalGuideStart > System.currentTimeMillis() ? View.VISIBLE : View.GONE); // show reset button if paged ahead

            mSpinner.setVisibility(View.GONE);
            if (firstFocusView != null) {
                firstFocusView.requestFocus();
            }
        }
    }

    private int currentCellId = 0;

    private GuideChannelHeader getChannelHeader(Context context, ChannelInfoDto channel){
        return new GuideChannelHeader(context, this, channel);
    }

    private LinearLayout getProgramRow(List<BaseItemDto> programs, String channelId) {

        LinearLayout programRow = new LinearLayout(requireContext());

        if (programs.size() == 0) {
            if (mFilters.any()) return null; // don't show rows with no program data

            int minutes = ((Long)((mCurrentLocalGuideEnd - mCurrentLocalGuideStart) / 60000)).intValue();
            int slot = 0;
            do {
                BaseItemDto empty = new BaseItemDto();
                empty.setId(UUID.randomUUID().toString());
                empty.setBaseItemType(BaseItemType.Folder);
                empty.setName(getString(R.string.no_program_data));
                empty.setChannelId(channelId);
                empty.setStartDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart + ((30*slot) * 60000))));
                empty.setEndDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart + ((30*(slot+1)) * 60000))));
                ProgramGridCell cell = new ProgramGridCell(requireContext(), this, empty, false);
                cell.setId(currentCellId++);
                cell.setLayoutParams(new ViewGroup.LayoutParams(30 * guideRowWidthPerMinPx, guideRowHeightPx));
                programRow.addView(cell);
                if (slot == 0)
                    cell.setFirst();
                if (slot == (minutes / 30) - 1)
                    cell.setLast();
                slot++;
            } while((30*slot) < minutes);
            return programRow;
        }

        long prevEnd = getCurrentLocalStartDate();
        for (BaseItemDto item : programs) {
            long start = item.getStartDate() != null ? TimeUtils.convertToLocalDate(item.getStartDate()).getTime() : getCurrentLocalStartDate();
            if (start < getCurrentLocalStartDate()) {
                start = getCurrentLocalStartDate();
            }

            if (start < prevEnd)
                continue;

            if (start > prevEnd) {
                // fill empty time slot
                BaseItemDto empty = new BaseItemDto();
                empty.setId(UUID.randomUUID().toString());
                empty.setBaseItemType(BaseItemType.Folder);
                empty.setName(getString(R.string.no_program_data));
                empty.setChannelId(channelId);
                empty.setStartDate(TimeUtils.convertToUtcDate(new Date(prevEnd)));
                Long duration = (start - prevEnd);
                empty.setEndDate(TimeUtils.convertToUtcDate(new Date(prevEnd+duration)));
                ProgramGridCell cell = new ProgramGridCell(requireContext(), this, empty, false);
                cell.setId(currentCellId++);
                cell.setLayoutParams(new ViewGroup.LayoutParams(((Long)(duration / 60000)).intValue() * guideRowWidthPerMinPx, guideRowHeightPx));
                if (prevEnd == mCurrentLocalGuideStart) {
                    cell.setFirst();
                }
                programRow.addView(cell);
            }
            long end = item.getEndDate() != null ? TimeUtils.convertToLocalDate(item.getEndDate()).getTime() : getCurrentLocalEndDate();
            if (end > getCurrentLocalEndDate()) end = getCurrentLocalEndDate();
            prevEnd = end;
            Long duration = (end - start) / 60000;
            if (duration > 0) {
                ProgramGridCell program = new ProgramGridCell(requireContext(), this, item, false);
                program.setId(currentCellId++);
                program.setLayoutParams(new ViewGroup.LayoutParams(duration.intValue() * guideRowWidthPerMinPx, guideRowHeightPx));
                if (start == mCurrentLocalGuideStart) {
                    program.setFirst();
                }
                if (end == mCurrentLocalGuideEnd) {
                    program.setLast();
                }

                programRow.addView(program);
            }
        }

        //If not at end of time period - fill in the rest
        if (prevEnd < mCurrentLocalGuideEnd) {
            // fill empty time slot
            BaseItemDto empty = new BaseItemDto();
            empty.setId(UUID.randomUUID().toString());
            empty.setBaseItemType(BaseItemType.Folder);
            empty.setName(getString(R.string.no_program_data));
            empty.setChannelId(channelId);
            empty.setStartDate(TimeUtils.convertToUtcDate(new Date(prevEnd)));
            Long duration = (mCurrentLocalGuideEnd - prevEnd);
            empty.setEndDate(TimeUtils.convertToUtcDate(new Date(prevEnd+duration)));
            ProgramGridCell cell = new ProgramGridCell(requireContext(), this, empty, false);
            cell.setId(currentCellId++);
            cell.setLayoutParams(new ViewGroup.LayoutParams(((Long)(duration / 60000)).intValue() * guideRowWidthPerMinPx, guideRowHeightPx));
            programRow.addView(cell);
        }

        return programRow;
    }

    private void fillTimeLine(long start, int hours) {
        mCurrentGuideStart = Calendar.getInstance();
        mCurrentGuideStart.setTime(new Date(start));
        mCurrentGuideStart.set(Calendar.MINUTE, mCurrentGuideStart.get(Calendar.MINUTE) >= 30 ? 30 : 0);
        mCurrentGuideStart.set(Calendar.SECOND, 0);
        mCurrentGuideStart.set(Calendar.MILLISECOND, 0);
        mCurrentLocalGuideStart = mCurrentGuideStart.getTimeInMillis();

        mDisplayDate.setText(TimeUtils.getFriendlyDate(requireContext(), mCurrentGuideStart.getTime()));
        Calendar current = (Calendar) mCurrentGuideStart.clone();
        mCurrentGuideEnd = (Calendar) mCurrentGuideStart.clone();
        int oneHour = 60 * guideRowWidthPerMinPx;
        int halfHour = 30 * guideRowWidthPerMinPx;
        int interval = current.get(Calendar.MINUTE) >= 30 ? 30 : 60;
        mCurrentGuideEnd.add(Calendar.HOUR, hours);
        mCurrentLocalGuideEnd = mCurrentGuideEnd.getTimeInMillis();
        mTimeline.removeAllViews();
        while (current.before(mCurrentGuideEnd)) {
            TextView time = new TextView(requireContext());
            time.setText(android.text.format.DateFormat.getTimeFormat(requireContext()).format(current.getTime()));
            time.setWidth(interval == 30 ? halfHour : oneHour);
            mTimeline.addView(time);
            current.add(Calendar.MINUTE, interval);
            //after first one, we always go on hours
            interval = 60;
        }
    }

    public long getCurrentLocalStartDate() { return mCurrentLocalGuideStart; }
    public long getCurrentLocalEndDate() { return mCurrentLocalGuideEnd; }

    private Runnable detailUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

            if (mSelectedProgram.getOverview() == null && mSelectedProgram.getId() != null) {
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemAsync(mSelectedProgram.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<BaseItemDto>(getLifecycle()) {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if (!getActive()) return;

                        mSelectedProgram = response;
                        detailUpdateInternal();
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (!getActive()) return;

                        Timber.e(exception, "Unable to get program details");
                        detailUpdateInternal();
                    }
                });
            } else {
                detailUpdateInternal();
            }
        }
    };

    private void detailUpdateInternal() {
        if (mSelectedProgram == null) return;

        mTitle.setText(mSelectedProgram.getName());
        mSummary.setText(mSelectedProgram.getOverview());

        //info row
        InfoLayoutHelper.addInfoRow(requireContext(), ModelCompat.asSdk(mSelectedProgram), mInfoRow, false, false);

        if (mSelectedProgram.getId() != null) {
            mDisplayDate.setText(TimeUtils.getFriendlyDate(requireContext(), TimeUtils.convertToLocalDate(mSelectedProgram.getStartDate())));
            String url = ImageUtils.getPrimaryImageUrl(ModelCompat.asSdk(mSelectedProgram));
            mImage.load(url, null, ContextCompat.getDrawable(requireContext(), R.drawable.blank10x10), 0, 0);
        } else {
            mImage.setImageResource(R.drawable.blank10x10);
        }
    }

    public void setSelectedProgram(RelativeLayout programView) {
        mSelectedProgramView = programView;
        if (mSelectedProgramView instanceof ProgramGridCell) {
            mSelectedProgram = ((ProgramGridCell)mSelectedProgramView).getProgram();
            mHandler.removeCallbacks(detailUpdateTask);
            mHandler.postDelayed(detailUpdateTask, 500);
        } else if (mSelectedProgramView instanceof GuideChannelHeader) {
            for (int i = 0; i < mChannels.getChildCount(); i++) {
                if (mSelectedProgramView == mChannels.getChildAt(i)) {
                    LinearLayout programRow = (LinearLayout)mProgramRows.getChildAt(i);
                    if (programRow == null)
                        return;
                    Date utcTime = TimeUtils.convertToUtcDate(new Date());
                    for (int ii = 0; ii < programRow.getChildCount(); ii++) {
                        ProgramGridCell prog = (ProgramGridCell)programRow.getChildAt(ii);
                        if (prog.getProgram() != null && prog.getProgram().getStartDate().before(utcTime) && prog.getProgram().getEndDate().after(utcTime)) {
                            mSelectedProgram = prog.getProgram();
                            if (mSelectedProgram != null) {
                                mHandler.removeCallbacks(detailUpdateTask);
                                mHandler.postDelayed(detailUpdateTask, 500);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }
}
