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
import org.jellyfin.androidtv.constant.CustomMessage;
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
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.util.CoroutineUtils;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.jellyfin.androidtv.util.ImageHelper;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.PlaybackHelper;
import org.jellyfin.androidtv.util.TextUtilsKt;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.EmptyResponse;
import org.jellyfin.androidtv.util.apiclient.Response;
import org.jellyfin.sdk.model.api.BaseItemDto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    BaseItemDto mSelectedProgram;
    RelativeLayout mSelectedProgramView;

    private List<BaseItemDto> mAllChannels;
    private UUID mFirstFocusChannelId;
    private boolean focusAtEnd;
    private GuideFilters mFilters = new GuideFilters();

    private LocalDateTime mCurrentGuideStart = LocalDateTime.now();
    private LocalDateTime mCurrentGuideEnd;
    private int mCurrentDisplayChannelStartNdx = 0;
    private int mCurrentDisplayChannelEndNdx = 0;

    private int guideRowHeightPx;
    private int guideRowWidthPerMinPx;

    private Handler mHandler = new Handler();

    private final Lazy<CustomMessageRepository> customMessageRepository = inject(CustomMessageRepository.class);
    private final Lazy<PlaybackHelper> playbackHelper = inject(PlaybackHelper.class);
    private final Lazy<ImageHelper> imageHelper = inject(ImageHelper.class);
    private final Lazy<PlaybackLauncher> playbackLauncher = inject(PlaybackLauncher.class);

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
                pageGuideTo(LocalDateTime.now());
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
        mCurrentGuideStart = LocalDateTime.now();
        fillTimeLine(mCurrentGuideStart, getGuideHours());
        TvManager.loadAllChannels(this, ndx -> {
            if (ndx >= PAGE_SIZE) {
                // last channel is not in first page so grab a set where it will be in the middle
                ndx = ndx - (PAGE_SIZE / 2);
            } else {
                ndx = 0; // just start at beginning
            }

            mAllChannels = TvManager.getAllChannels();
            if (!mAllChannels.isEmpty()) {
                displayChannels(ndx, PAGE_SIZE);
            } else {
                mSpinner.setVisibility(View.GONE);
            }
            return null;
        });
    }

    public void refreshFavorite(UUID channelId){
        for (int i = 0; i < mChannels.getChildCount(); i++) {
            View child = mChannels.getChildAt(i);
            if (!(child instanceof GuideChannelHeader)) continue;
            GuideChannelHeader gch = (GuideChannelHeader) child;
            if (gch.getChannel().getId().equals(channelId.toString()))
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
        if (TvManager.shouldForceReload() || mCurrentGuideStart.plusMinutes(30).isBefore(LocalDateTime.now()) || mChannels.getChildCount() == 0) {
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

        if (mCurrentGuideStart.isAfter(LocalDateTime.now())) {
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
                    LiveTvGuideFragmentHelperKt.toggleFavorite(this);
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
                    if (mSelectedProgramView instanceof ProgramGridCell) {
                        if (mSelectedProgram.getStartDate().isBefore(LocalDateTime.now()))
                            playbackHelper.getValue().retrieveAndPlay(mSelectedProgram.getChannelId(), false, requireContext());
                        else
                            showProgramOptions();
                        return true;
                    } else if (mSelectedProgramView instanceof GuideChannelHeader) {
                        // Tuning directly to a channel
                        GuideChannelHeader channelHeader = (GuideChannelHeader) mSelectedProgramView;
                        playbackHelper.getValue().getItemsToPlay(requireContext(), channelHeader.getChannel(), false, false, new Response<List<BaseItemDto>>(getLifecycle()) {
                            @Override
                            public void onResponse(List<BaseItemDto> response) {
                                if (!isActive()) return;
                                playbackLauncher.getValue().launch(requireContext(), response);
                            }
                        });
                    }
                }
                return false;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if ((mDetailPopup == null || !mDetailPopup.isShowing())
                        && mSelectedProgram != null
                        && mSelectedProgram.getChannelId() != null) {
                    // tune to the current channel
                    playbackHelper.getValue().retrieveAndPlay(mSelectedProgram.getChannelId(), false, requireContext());
                    return true;
                }

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (requireActivity().getCurrentFocus() instanceof ProgramGridCell
                        && mSelectedProgramView != null
                        && ((ProgramGridCell)mSelectedProgramView).isLast()) {
                    requestGuidePage(mCurrentGuideEnd);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (requireActivity().getCurrentFocus() instanceof ProgramGridCell
                        && mSelectedProgramView != null
                        && ((ProgramGridCell)mSelectedProgramView).isFirst()
                        && mSelectedProgram.getStartDate().isAfter(LocalDateTime.now())) {
                    focusAtEnd = true;
                    requestGuidePage(mCurrentGuideStart.minusHours(getGuideHours()));
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

    AlertDialog dateDialog;

    private void showDatePicker() {
        FrameLayout scrollPane = (FrameLayout) getLayoutInflater().inflate(R.layout.horizontal_scroll_pane, null);
        LinearLayout scrollItems = scrollPane.findViewById(R.id.scrollItems);
        for (long increment = 0; increment < 15; increment++) {
            scrollItems.addView(new FriendlyDateButton(requireContext(),  LocalDateTime.now().plusDays(increment), datePickedListener));
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

    private void requestGuidePage(final LocalDateTime startTime) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.lbl_load_guide_data)
                .setMessage(startTime.isAfter(mCurrentGuideStart) ? getString(R.string.msg_live_tv_next, getGuideHours()) : getString(R.string.msg_live_tv_prev, getGuideHours()))
                .setPositiveButton(R.string.lbl_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pageGuideTo(startTime);
                    }
                })
                .setNegativeButton(R.string.lbl_no, null)
                .show();
    }

    private void pageGuideTo(LocalDateTime startTime) {
        if (startTime.isBefore(LocalDateTime.now())) startTime = LocalDateTime.now();
        Timber.i("page to %s", startTime);
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
            mDetailPopup = new LiveProgramDetailPopup(requireActivity(), this, this, mSummary.getWidth()+20, new EmptyResponse(getLifecycle()) {
                @Override
                public void onResponse() {
                    if (!isActive()) return;
                    playbackHelper.getValue().retrieveAndPlay(mSelectedProgram.getChannelId(), false, requireContext());
                }
            });
        }

        mDetailPopup.setContent(mSelectedProgram, ((ProgramGridCell)mSelectedProgramView));
        mDetailPopup.show(mImage, mTitle.getLeft(), mTitle.getTop() - 10);
    }

    public void showFilterOptions() {
        startActivity(ActivityDestinations.INSTANCE.liveTvGuideFilterPreferences(getContext()));
        TvManager.forceReload();
    }

    public void showOptions() {
        startActivity(ActivityDestinations.INSTANCE.liveTvGuideOptionPreferences(getContext()));
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
        TvManager.getProgramsAsync(this, mCurrentDisplayChannelStartNdx, mCurrentDisplayChannelEndNdx, mCurrentGuideStart, mCurrentGuideEnd, new EmptyResponse(getLifecycle()) {
            @Override
            public void onResponse() {
                if (!isActive()) return;
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
                final BaseItemDto channel = TvManager.getChannel(i);
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
                    TvManager.setFocusParams(row, prevRow, true);
                    TvManager.setFocusParams(prevRow, row, false);
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

            mResetButton.setVisibility(mCurrentGuideStart.isAfter(LocalDateTime.now()) ? View.VISIBLE : View.GONE); // show reset button if paged ahead

            mSpinner.setVisibility(View.GONE);
            if (firstFocusView != null) {
                firstFocusView.requestFocus();
            }
        }
    }

    private int currentCellId = 0;

    private GuideChannelHeader getChannelHeader(Context context, org.jellyfin.sdk.model.api.BaseItemDto channel){
        return new GuideChannelHeader(context, this, channel);
    }

    private LinearLayout getProgramRow(List<BaseItemDto> programs, UUID channelId) {

        LinearLayout programRow = new LinearLayout(requireContext());

        if (programs.size() == 0) {
            if (mFilters.any()) return null; // don't show rows with no program data

            int minutes = ((Long) ((mCurrentGuideEnd.toInstant(ZoneOffset.UTC).toEpochMilli() - mCurrentGuideStart.toInstant(ZoneOffset.UTC).toEpochMilli()) / 60000)).intValue();
            int slot = 0;
            do {
                BaseItemDto empty = LiveTvGuideFragmentHelperKt.createNoProgramDataBaseItem(
                        getContext(),
                        channelId,
                        mCurrentGuideStart.plusMinutes(30l * slot),
                        mCurrentGuideEnd.plusMinutes(30l * (slot + 1))
                );

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

        LocalDateTime prevEnd = getCurrentLocalStartDate();
        for (BaseItemDto item : programs) {
            LocalDateTime start = item.getStartDate() != null ? item.getStartDate() : getCurrentLocalStartDate();
            if (start.isBefore(getCurrentLocalStartDate())) {
                start = getCurrentLocalStartDate();
            }

            if (start.isBefore(prevEnd))
                continue;

            if (start.isAfter(prevEnd)) {
                // fill empty time slot
                BaseItemDto empty = LiveTvGuideFragmentHelperKt.createNoProgramDataBaseItem(
                        getContext(),
                        channelId,
                        prevEnd,
                        start
                );

                ProgramGridCell cell = new ProgramGridCell(requireContext(), this, empty, false);
                cell.setId(currentCellId++);
                cell.setLayoutParams(new ViewGroup.LayoutParams(((Long) ((start.toInstant(ZoneOffset.UTC).toEpochMilli() - prevEnd.toInstant(ZoneOffset.UTC).toEpochMilli()) / 60000)).intValue() * guideRowWidthPerMinPx, guideRowHeightPx));
                if (prevEnd == mCurrentGuideStart) {
                    cell.setFirst();
                }
                programRow.addView(cell);
            }
            LocalDateTime end = item.getEndDate() != null ? item.getEndDate() : getCurrentLocalEndDate();
            if (end.isAfter(getCurrentLocalEndDate())) end = getCurrentLocalEndDate();
            prevEnd = end;
            Long duration = (end.toInstant(ZoneOffset.UTC).toEpochMilli() - start.toInstant(ZoneOffset.UTC).toEpochMilli()) / 60000;
            if (duration > 0) {
                ProgramGridCell program = new ProgramGridCell(requireContext(), this, item, false);
                program.setId(currentCellId++);
                program.setLayoutParams(new ViewGroup.LayoutParams(duration.intValue() * guideRowWidthPerMinPx, guideRowHeightPx));
                if (start == mCurrentGuideStart) {
                    program.setFirst();
                }
                if (end == mCurrentGuideEnd) {
                    program.setLast();
                }

                programRow.addView(program);
            }
        }

        //If not at end of time period - fill in the rest
        if (prevEnd.isBefore(mCurrentGuideEnd)) {
            // fill empty time slot
            BaseItemDto empty = LiveTvGuideFragmentHelperKt.createNoProgramDataBaseItem(
                    getContext(),
                    channelId,
                    prevEnd,
                    mCurrentGuideEnd
            );

            ProgramGridCell cell = new ProgramGridCell(requireContext(), this, empty, false);
            cell.setId(currentCellId++);
            cell.setLayoutParams(new ViewGroup.LayoutParams(((Long)((mCurrentGuideEnd.toInstant(ZoneOffset.UTC).toEpochMilli() - prevEnd.toInstant(ZoneOffset.UTC).toEpochMilli()) / 60000)).intValue() * guideRowWidthPerMinPx, guideRowHeightPx));
            programRow.addView(cell);
        }

        return programRow;
    }

    private void fillTimeLine(LocalDateTime start, int hours) {
        mCurrentGuideStart = start;
        mCurrentGuideStart = mCurrentGuideStart
                .withMinute(mCurrentGuideStart.getMinute())
                .withSecond(0)
                .withNano(0);

        mDisplayDate.setText(TimeUtils.getFriendlyDate(requireContext(), mCurrentGuideStart));
        mCurrentGuideEnd = mCurrentGuideStart
                .plusHours(hours);
        int oneHour = 60 * guideRowWidthPerMinPx;
        int halfHour = 30 * guideRowWidthPerMinPx;

        int interval = mCurrentGuideStart.getMinute() >= 30 ? 60 - mCurrentGuideStart.getMinute() : 30 - mCurrentGuideStart.getMinute();
        mTimeline.removeAllViews();

        LocalDateTime current = mCurrentGuideStart;
        while (current.isBefore(mCurrentGuideEnd)) {
            TextView time = new TextView(requireContext());
            time.setText(DateTimeExtensionsKt.getTimeFormatter(getContext()).format(current));
            time.setWidth(interval != 60 ? ( interval < 15 ? 15 * guideRowWidthPerMinPx : interval * guideRowWidthPerMinPx) : oneHour);
            mTimeline.addView(time);
            current = current.plusMinutes(interval);
            //after first one, we always go on hours
            interval = interval < 30 ? 30 : 60;
        }
    }

    public LocalDateTime getCurrentLocalStartDate() { return mCurrentGuideStart; }
    public LocalDateTime getCurrentLocalEndDate() { return mCurrentGuideEnd; }

    private Runnable detailUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;
            LiveTvGuideFragmentHelperKt.refreshSelectedProgram(LiveTvGuideFragment.this);
        }
    };

    void detailUpdateInternal() {
        if (mSelectedProgram == null) return;

        mTitle.setText(mSelectedProgram.getName());
        mSummary.setText(mSelectedProgram.getOverview());

        //info row
        InfoLayoutHelper.addInfoRow(requireContext(), mSelectedProgram, mInfoRow, false);

        mDisplayDate.setText(TimeUtils.getFriendlyDate(requireContext(), mSelectedProgram.getStartDate()));
        String url = imageHelper.getValue().getPrimaryImageUrl(mSelectedProgram, null, ImageHelper.MAX_PRIMARY_IMAGE_HEIGHT);
        mImage.load(url, null, ContextCompat.getDrawable(requireContext(), R.drawable.blank10x10), 0, 0);

        if (mDetailPopup != null && mDetailPopup.isShowing() && mSelectedProgramView != null) {
            mDetailPopup.setContent(mSelectedProgram, ((ProgramGridCell) mSelectedProgramView));
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
                    for (int ii = 0; ii < programRow.getChildCount(); ii++) {
                        ProgramGridCell prog = (ProgramGridCell)programRow.getChildAt(ii);
                        if (prog.getProgram() != null && prog.getProgram().getStartDate().isBefore(LocalDateTime.now()) && prog.getProgram().getEndDate().isAfter(LocalDateTime.now())) {
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
