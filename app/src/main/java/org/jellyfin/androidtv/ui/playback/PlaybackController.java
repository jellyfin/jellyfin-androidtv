package org.jellyfin.androidtv.ui.playback;

import static org.koin.java.KoinJavaComponent.inject;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.data.compat.PlaybackException;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.data.compat.SubtitleStreamInfo;
import org.jellyfin.androidtv.data.compat.VideoOptions;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.preference.SystemPreferences;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.UserSettingPreferences;
import org.jellyfin.androidtv.preference.constant.NextUpBehavior;
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer;
import org.jellyfin.androidtv.preference.constant.RefreshRateSwitchingBehavior;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.util.DeviceUtils;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.ReportingHelper;
import org.jellyfin.androidtv.util.apiclient.StreamHelper;
import org.jellyfin.androidtv.util.profile.BaseProfile;
import org.jellyfin.androidtv.util.profile.ExoPlayerProfile;
import org.jellyfin.androidtv.util.profile.LibVlcProfile;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.MediaSourceInfo;
import org.jellyfin.apiclient.model.entities.LocationType;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.entities.MediaStreamType;
import org.jellyfin.apiclient.model.library.PlayAccess;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.mediainfo.SubtitleTrackInfo;
import org.jellyfin.apiclient.model.session.PlayMethod;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

public class PlaybackController implements PlaybackControllerNotifiable {
    // Frequency to report playback progress
    private final static long PROGRESS_REPORTING_INTERVAL = TimeUtils.secondsToMillis(3);
    // Frequency to report paused state
    private static final long PROGRESS_REPORTING_PAUSE_INTERVAL = TimeUtils.secondsToMillis(15);

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private Lazy<PlaybackManager> playbackManager = inject(PlaybackManager.class);
    private Lazy<UserPreferences> userPreferences = inject(UserPreferences.class);
    private Lazy<SystemPreferences> systemPreferences = inject(SystemPreferences.class);
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private Lazy<org.jellyfin.sdk.api.client.ApiClient> api = inject(org.jellyfin.sdk.api.client.ApiClient.class);
    private Lazy<DataRefreshService> dataRefreshService = inject(DataRefreshService.class);

    List<BaseItemDto> mItems;
    VideoManager mVideoManager;
    int mCurrentIndex = 0;
    private long mCurrentPosition = 0;
    private PlaybackState mPlaybackState = PlaybackState.IDLE;

    private StreamInfo mCurrentStreamInfo;
    private List<SubtitleStreamInfo> mSubtitleStreams;

    @Nullable
    private CustomPlaybackOverlayFragment mFragment;
    private Boolean spinnerOff = false;

    private VideoOptions mCurrentOptions;
    private int mDefaultSubIndex = -1;
    private int mDefaultAudioIndex = -1;
    private boolean burningSubs = false;
    private float mRequestedPlaybackSpeed = -1.0f;

    private PlayMethod mPlaybackMethod = PlayMethod.Transcode;

    private Runnable mReportLoop;
    private Handler mHandler;

    private long mStartPosition = 0;

    // tmp position used when seeking
    private long mSeekPosition = -1;
    private boolean wasSeeking = false;
    private boolean finishedInitialSeek = false;

    private long mCurrentProgramEndTime;
    private long mCurrentProgramStartTime = 0;
    private long mCurrentTranscodeStartTime;
    private boolean isLiveTv = false;
    private boolean directStreamLiveTv;
    private String liveTvChannelName = "";
    private boolean useVlc;

    private boolean vlcErrorEncountered;
    private boolean exoErrorEncountered;
    private int playbackRetries = 0;
    private long lastPlaybackError = 0;

    private Display.Mode[] mDisplayModes;
    private RefreshRateSwitchingBehavior refreshRateSwitchingBehavior = RefreshRateSwitchingBehavior.DISABLED;

    public PlaybackController(List<BaseItemDto> items, CustomPlaybackOverlayFragment fragment) {
        mItems = items;
        mFragment = fragment;
        mHandler = new Handler();

        if (DeviceUtils.is60()) {
            refreshRateSwitchingBehavior = userPreferences.getValue().get(UserPreferences.Companion.getRefreshRateSwitchingBehavior());
            if (refreshRateSwitchingBehavior != RefreshRateSwitchingBehavior.DISABLED)
                getDisplayModes();
        }

        // Set default value for useVlc field
        // when set to auto the default will be exoplayer
        useVlc = userPreferences.getValue().get(UserPreferences.Companion.getVideoPlayer()) == PreferredVideoPlayer.VLC;
    }

    public void clearFragment() {
        mFragment = null;
    }

    public boolean hasFragment() {
        return mFragment != null;
    }

    public CustomPlaybackOverlayFragment getFragment() {
        return mFragment;
    }

    public void init(@NonNull VideoManager mgr, @NonNull CustomPlaybackOverlayFragment fragment) {
        mVideoManager = mgr;
        mVideoManager.subscribe(this);
        mFragment = fragment;
        directStreamLiveTv = userPreferences.getValue().get(UserPreferences.Companion.getLiveTvDirectPlayEnabled());
    }

    public void setItems(List<BaseItemDto> items) {
        mItems = items;
        mCurrentIndex = 0;
    }

    public PlayMethod getPlaybackMethod() {
        return mPlaybackMethod;
    }

    public void setPlaybackMethod(@NonNull PlayMethod value) {
        mPlaybackMethod = value;
    }

    public float getPlaybackSpeed() {
        if (hasInitializedVideoManager()) {
            // Actually poll the video manager, since exoplayer can revert back
            // to 1x if it can't go faster, so we should check directly
            return mVideoManager.getPlaybackSpeed();
        } else {
            return mRequestedPlaybackSpeed;
        }
    }

    public void setPlaybackSpeed(float speed) {
        mRequestedPlaybackSpeed = speed;
        if (hasInitializedVideoManager()) {
            mVideoManager.setPlaybackSpeed(speed);
        }
    }

    public BaseItemDto getCurrentlyPlayingItem() {
        return mItems.size() > mCurrentIndex ? mItems.get(mCurrentIndex) : null;
    }

    public boolean hasInitializedVideoManager() {
        return mVideoManager != null && mVideoManager.isInitialized();
    }

    public MediaSourceInfo getCurrentMediaSource() {
        if (mCurrentStreamInfo != null && mCurrentStreamInfo.getMediaSource() != null) {
            return mCurrentStreamInfo.getMediaSource();
        } else {
            ArrayList<MediaSourceInfo> mediaSources = getCurrentlyPlayingItem().getMediaSources();

            if (mediaSources == null || mediaSources.isEmpty()) {
                return null;
            } else {
                return mediaSources.get(0);
            }
        }
    }

    public StreamInfo getCurrentStreamInfo() {
        return mCurrentStreamInfo;
    }

    public boolean canSeek() {
        return !isLiveTv;
    }

    public boolean isLiveTv() {
        return isLiveTv;
    }

    public int getSubtitleStreamIndex() {
        return (mCurrentOptions != null && mCurrentOptions.getSubtitleStreamIndex() != null) ? mCurrentOptions.getSubtitleStreamIndex() : -1;
    }

    public List<SubtitleStreamInfo> getSubtitleStreams() {
        return mSubtitleStreams;
    }

    public SubtitleStreamInfo getSubtitleStreamInfo(int index) {
        for (SubtitleStreamInfo info : mSubtitleStreams) {
            if (info.getIndex() == index) return info;
        }

        return null;
    }

    public boolean isNativeMode() {
        return mVideoManager == null || mVideoManager.isNativeMode();
    }

    public boolean isTranscoding() {
        // use or here so that true is the default since
        // this method is used to exclude features that may break unless we are sure playback is direct
        return mCurrentStreamInfo == null || mCurrentStreamInfo.getPlayMethod() == PlayMethod.Transcode;
    }

    public boolean hasNextItem() {
        return mItems != null && mCurrentIndex < mItems.size() - 1;
    }

    public BaseItemDto getNextItem() {
        return hasNextItem() ? mItems.get(mCurrentIndex + 1) : null;
    }

    public boolean isPlaying() {
        // since playbackController is so closely tied to videoManager, check if it is playing too since they can fall out of sync
        return mPlaybackState == PlaybackState.PLAYING && hasInitializedVideoManager() && mVideoManager.isPlaying();
    }

    public void setAudioDelay(long value) {
        if (hasInitializedVideoManager()) mVideoManager.setAudioDelay(value);
    }

    public long getAudioDelay() {
        return hasInitializedVideoManager() ? mVideoManager.getAudioDelay() : 0;
    }

    public void playerErrorEncountered() {
        if (isNativeMode()) exoErrorEncountered = true;
        else vlcErrorEncountered = true;

        // reset the retry count if it's been more than 30s since previous error
        if (playbackRetries > 0 && System.currentTimeMillis() - lastPlaybackError > 30000) {
            Timber.d("playback stabilized - retry count reset to 0 from %s", playbackRetries);
            playbackRetries = 0;
        }

        playbackRetries++;
        lastPlaybackError = System.currentTimeMillis();

        if (playbackRetries < 3) {
            if (mFragment != null)
                Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.player_error));
            Timber.i("Player error encountered - retrying");
            stop();
            play(mCurrentPosition);
        } else {
            mPlaybackState = PlaybackState.ERROR;
            if (mFragment != null) {
                Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.too_many_errors));
                mFragment.finish();
            }
        }
    }

    @TargetApi(23)
    private void getDisplayModes() {
        if (mFragment == null)
            return;
        Display display = mFragment.requireActivity().getWindowManager().getDefaultDisplay();
        mDisplayModes = display.getSupportedModes();
        Timber.i("** Available display refresh rates:");
        for (Display.Mode mDisplayMode : mDisplayModes) {
            Timber.d("display mode %s - %dx%d@%f", mDisplayMode.getModeId(), mDisplayMode.getPhysicalWidth(), mDisplayMode.getPhysicalHeight(), mDisplayMode.getRefreshRate());
        }
    }

    @TargetApi(23)
    private Display.Mode findBestDisplayMode(MediaStream videoStream) {
        if (mFragment == null || mDisplayModes == null || videoStream.getRealFrameRate() == null) return null;


        int curWeight = 0;
        Display.Mode bestMode = null;
        int sourceRate = Math.round(videoStream.getRealFrameRate() * 100);

        Display.Mode defaultMode = mFragment.requireActivity().getWindowManager().getDefaultDisplay().getMode();

        Timber.d("trying to find display mode for video: %dx%d@%f", videoStream.getWidth(), videoStream.getHeight(), videoStream.getRealFrameRate());
        for (Display.Mode mode : mDisplayModes) {
            Timber.d("considering display mode: %s - %dx%d@%f", mode.getModeId(), mode.getPhysicalWidth(), mode.getPhysicalHeight(), mode.getRefreshRate());

            // Skip unwanted display modes
            if (mode.getPhysicalWidth() < 1280 || mode.getPhysicalHeight() < 720)  // Skip non-HD
                continue;

            if (mode.getPhysicalWidth() < videoStream.getWidth() || mode.getPhysicalHeight() < videoStream.getHeight())  // Disallow reso downgrade
                continue;

            int rate = Math.round(mode.getRefreshRate() * 100);
            if (rate != sourceRate && rate != sourceRate * 2 && rate != Math.round(sourceRate * 2.5)) // Skip inappropriate rates
                continue;

            Timber.d("qualifying display mode: %s - %dx%d@%f", mode.getModeId(), mode.getPhysicalWidth(), mode.getPhysicalHeight(), mode.getRefreshRate());

            // if scaling on-device, keep native resolution modes at diff 0 (best score)
            // for other resolutions when scaling on device, or if scaling on tv, score based on distance from media resolution
            int resolutionDifference = 0;
            if ((refreshRateSwitchingBehavior == RefreshRateSwitchingBehavior.SCALE_ON_DEVICE &&
                    !(mode.getPhysicalWidth() == defaultMode.getPhysicalWidth() && mode.getPhysicalHeight() == defaultMode.getPhysicalHeight())) ||

                    refreshRateSwitchingBehavior == RefreshRateSwitchingBehavior.SCALE_ON_TV) {

                resolutionDifference = Math.abs(mode.getPhysicalWidth() - videoStream.getWidth());
            }
            int refreshRateDifference = rate - sourceRate;

            // use 100,000 to account for refresh rates 120Hz+ (at 120Hz rate == 12,000)
            int weight = 100000 - refreshRateDifference + 100000 - resolutionDifference;

            if (weight > curWeight) {
                Timber.d("preferring mode: %s - %dx%d@%f", mode.getModeId(), mode.getPhysicalWidth(), mode.getPhysicalHeight(), mode.getRefreshRate());
                curWeight = weight;
                bestMode = mode;
            }
        }

        return bestMode;
    }

    @TargetApi(23)
    private void setRefreshRate(MediaStream videoStream) {
        if (videoStream == null || mFragment == null) {
            Timber.e("Null video stream attempting to set refresh rate");
            return;
        }

        Display.Mode current = mFragment.requireActivity().getWindowManager().getDefaultDisplay().getMode();
        Display.Mode best = findBestDisplayMode(videoStream);
        if (best != null) {
            Timber.i("*** Best refresh mode is: %s - %dx%d/%f",
                    best.getModeId(), best.getPhysicalWidth(), best.getPhysicalHeight(), best.getRefreshRate());
            if (current.getModeId() != best.getModeId()) {
                Timber.i("*** Attempting to change refresh rate from: %s - %dx%d@%f", current.getModeId(), current.getPhysicalWidth(),
                                                                                                current.getPhysicalHeight(),current.getRefreshRate());
                WindowManager.LayoutParams params = mFragment.requireActivity().getWindow().getAttributes();
                params.preferredDisplayModeId = best.getModeId();
                mFragment.requireActivity().getWindow().setAttributes(params);
            } else {
                Timber.i("Display is already in best mode");
            }
        } else {
            Timber.i("*** Unable to find display mode for refresh rate: %f", videoStream.getRealFrameRate());
        }
    }

    // central place to update mCurrentPosition
    private void refreshCurrentPosition() {
        long newPos = -1;

        if (isLiveTv && mCurrentProgramStartTime > 0) {
            newPos = getRealTimeProgress();
            // live tv
        } else if (hasInitializedVideoManager()) {
            if (!isPlaying() && mSeekPosition != -1) {
                newPos = mSeekPosition;
                // use seekPosition until playback starts
            } else if (isPlaying()) {
                if (finishedInitialSeek) {
                    // playback has started following initial seek for direct play and hls
                    // get current position and reset seekPosition
                    newPos = mVideoManager.getCurrentPosition();
                    mSeekPosition = -1;
                } else if (wasSeeking) {
                    // the initial seek for direct play and hls completed
                    finishedInitialSeek = true;
                } else if (mSeekPosition != -1) {
                    // the initial seek for direct play and hls hasn't happened yet
                    newPos = mSeekPosition;
                }
                wasSeeking = false;
            }
        }
        // use original value if new one isn't available
        mCurrentPosition = newPos != -1 ? newPos : mCurrentPosition;
    }

    public void play(long position) {
        play(position, null);
    }

    private void play(long position, @Nullable Integer forcedSubtitleIndex) {
        Timber.d("Play called from state: %s with pos: %d and sub index: %d", mPlaybackState, position, forcedSubtitleIndex);

        if (position < 0) {
            Timber.i("Negative start requested - adjusting to zero");
            position = 0;
        }

        switch (mPlaybackState) {
            case PLAYING:
                // do nothing
                break;
            case PAUSED:
                if (!hasInitializedVideoManager()) {
                    return;
                }
                // just resume
                mVideoManager.play();
                if (mVideoManager.isNativeMode())
                    mPlaybackState = PlaybackState.PLAYING; //won't get another onprepared call
                if (mFragment != null)
                    mFragment.setFadingEnabled(true);
                startReportLoop();
                break;
            case BUFFERING:
                // onPrepared should take care of it
                break;
            case IDLE:
                // start new playback

                // set mSeekPosition so the seekbar will not default to 0:00
                mSeekPosition = position;
                mCurrentPosition = 0;

                if (mFragment != null) {
                    mFragment.setFadingEnabled(false);
                }

                BaseItemDto item = getCurrentlyPlayingItem();

                if (item == null) {
                    Timber.d("item is null - aborting play");
                    if (mFragment != null) {
                        Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.msg_cannot_play));
                        mFragment.requireActivity().finish();
                    }
                    return;
                }

                // make sure item isn't missing
                if (item.getLocationType() == LocationType.Virtual && mFragment != null) {
                    if (hasNextItem()) {
                        new AlertDialog.Builder(mFragment.getContext())
                                .setTitle(R.string.episode_missing)
                                .setMessage(R.string.episode_missing_message)
                                .setPositiveButton(R.string.lbl_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        next();
                                    }
                                })
                                .setNegativeButton(R.string.lbl_no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mFragment.requireActivity().finish();
                                    }
                                })
                                .create()
                                .show();
                    } else {
                        new AlertDialog.Builder(mFragment.getContext())
                                .setTitle(R.string.episode_missing)
                                .setMessage(R.string.episode_missing_message_2)
                                .setPositiveButton(R.string.lbl_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mFragment.requireActivity().finish();
                                    }
                                })
                                .create()
                                .show();
                    }
                    return;
                }

                // confirm we actually can play
                if (item.getPlayAccess() != PlayAccess.Full) {
                    if (mFragment == null) return;

                    String msg = item.getIsPlaceHolder() ? mFragment.getString(R.string.msg_cannot_play) : mFragment.getString(R.string.msg_cannot_play_time);
                    Utils.showToast(mFragment.getContext(), msg);
                    return;
                }

                isLiveTv = item.getBaseItemType() == BaseItemType.TvChannel;
                startSpinner();

                // undo setting mSeekPosition for liveTV
                if (isLiveTv) mSeekPosition = -1;

                int maxBitrate = Utils.getMaxBitrate();
                Timber.d("Max bitrate is: %d", maxBitrate);
                VideoOptions vlcOptions = buildVLCOptions(forcedSubtitleIndex, item, maxBitrate);
                VideoOptions internalOptions = buildExoPlayerOptions(forcedSubtitleIndex, item, maxBitrate);

                playInternal(getCurrentlyPlayingItem(), position, vlcOptions, internalOptions);
                mPlaybackState = PlaybackState.BUFFERING;
                if (mFragment != null) {
                    mFragment.setPlayPauseActionState(0);
                    mFragment.setCurrentTime(position);
                }

                long duration = getCurrentlyPlayingItem().getRunTimeTicks() != null ? getCurrentlyPlayingItem().getRunTimeTicks() / 10000 : -1;
                if (mVideoManager != null)
                    mVideoManager.setMetaDuration(duration);

                break;
        }
    }

    @NonNull
    private VideoOptions buildExoPlayerOptions(@Nullable Integer forcedSubtitleIndex, BaseItemDto item, int maxBitrate) {
        VideoOptions internalOptions = new VideoOptions();
        internalOptions.setItemId(item.getId());
        internalOptions.setMediaSources(item.getMediaSources());
        internalOptions.setMaxBitrate(maxBitrate);
        if (exoErrorEncountered || (isLiveTv && !directStreamLiveTv))
            internalOptions.setEnableDirectStream(false);
        internalOptions.setMaxAudioChannels(Utils.downMixAudio(mFragment.getContext()) ? 2 : null); //have to downmix at server
        internalOptions.setSubtitleStreamIndex(forcedSubtitleIndex);
        internalOptions.setMediaSourceId(forcedSubtitleIndex != null ? getCurrentMediaSource().getId() : null);
        DeviceProfile internalProfile = new BaseProfile();
        if (DeviceUtils.is60() || userPreferences.getValue().get(UserPreferences.Companion.getAc3Enabled())) {
            internalProfile = new ExoPlayerProfile(
                    mFragment.getContext(),
                    isLiveTv,
                    userPreferences.getValue().get(UserPreferences.Companion.getLiveTvDirectPlayEnabled()),
                    userPreferences.getValue().get(UserPreferences.Companion.getAc3Enabled())
            );
            Timber.i("*** Using extended Exoplayer profile options");
        } else {
            Timber.i("*** Using default android profile");
        }
        internalOptions.setProfile(internalProfile);
        return internalOptions;
    }

    @NonNull
    private VideoOptions buildVLCOptions(@Nullable Integer forcedSubtitleIndex, BaseItemDto item, int maxBitrate) {
        VideoOptions vlcOptions = new VideoOptions();
        vlcOptions.setItemId(item.getId());
        vlcOptions.setMediaSources(item.getMediaSources());
        vlcOptions.setMaxBitrate(maxBitrate);
        if (vlcErrorEncountered) {
            Timber.i("*** Disabling direct play/stream due to previous error");
            vlcOptions.setEnableDirectStream(false);
            vlcOptions.setEnableDirectPlay(false);
        }
        vlcOptions.setSubtitleStreamIndex(forcedSubtitleIndex);
        vlcOptions.setMediaSourceId(forcedSubtitleIndex != null ? getCurrentMediaSource().getId() : null);
        DeviceProfile vlcProfile = new LibVlcProfile(mFragment.getContext(), isLiveTv);
        vlcOptions.setProfile(vlcProfile);
        return vlcOptions;
    }

    public int getBufferAmount() {
        return 600;
    }

    private void playInternal(final BaseItemDto item, final Long position, final VideoOptions vlcOptions, final VideoOptions internalOptions) {
        if (isLiveTv) {
            liveTvChannelName = " (" + item.getName() + ")";
            updateTvProgramInfo();
            TvManager.setLastLiveTvChannel(item.getId());
            //Choose appropriate player now to avoid opening two streams
            if (!directStreamLiveTv || userPreferences.getValue().get(UserPreferences.Companion.getLiveTvVideoPlayer()) != PreferredVideoPlayer.VLC) {
                //internal/exo player
                Timber.i("Using internal player for Live TV");
                playbackManager.getValue().getVideoStreamInfo(api.getValue().getDeviceInfo(), internalOptions, position * 10000, apiClient.getValue(), new Response<StreamInfo>() {
                    @Override
                    public void onResponse(StreamInfo response) {
                        if (mVideoManager == null)
                            return;
                        mVideoManager.init(getBufferAmount(), false);
                        mCurrentOptions = internalOptions;
                        useVlc = false;
                        startItem(item, position, response);
                    }

                    @Override
                    public void onError(Exception exception) {
                        handlePlaybackInfoError(exception);
                    }
                });

            } else {
                //VLC
                Timber.i("Using VLC for Live TV");
                playbackManager.getValue().getVideoStreamInfo(api.getValue().getDeviceInfo(), vlcOptions, position * 10000, apiClient.getValue(), new Response<StreamInfo>() {
                    @Override
                    public void onResponse(StreamInfo response) {
                        if (mVideoManager == null)
                            return;
                        mVideoManager.init(getBufferAmount(), response.getMediaSource().getVideoStream().getIsInterlaced() && (response.getMediaSource().getVideoStream().getWidth() == null || response.getMediaSource().getVideoStream().getWidth() > 1200));
                        mCurrentOptions = vlcOptions;
                        useVlc = true;
                        startItem(item, position, response);
                    }

                    @Override
                    public void onError(Exception exception) {
                        handlePlaybackInfoError(exception);
                    }
                });


            }
        } else {
            // Get playback info for each player and then decide on which one to use
            playbackManager.getValue().getVideoStreamInfo(api.getValue().getDeviceInfo(), vlcOptions, position * 10000, apiClient.getValue(), new Response<StreamInfo>() {
                @Override
                public void onResponse(final StreamInfo vlcResponse) {
                    Timber.i("VLC would %s", vlcResponse.getPlayMethod().equals(PlayMethod.Transcode) ? "transcode" : "direct stream");
                    playbackManager.getValue().getVideoStreamInfo(api.getValue().getDeviceInfo(), internalOptions, position * 10000, apiClient.getValue(), new Response<StreamInfo>() {
                        @Override
                        public void onResponse(StreamInfo internalResponse) {
                            Timber.i("Internal player would %s", internalResponse.getPlayMethod().equals(PlayMethod.Transcode) ? "transcode" : "direct stream");
                            boolean useDeinterlacing = vlcResponse.getMediaSource().getVideoStream() != null &&
                                    vlcResponse.getMediaSource().getVideoStream().getIsInterlaced() &&
                                    (vlcResponse.getMediaSource().getVideoStream().getWidth() == null ||
                                            vlcResponse.getMediaSource().getVideoStream().getWidth() > 1200);
                            Timber.i(useDeinterlacing ? "Explicit deinterlacing will be used" : "Explicit deinterlacing will NOT be used");

                            PreferredVideoPlayer preferredVideoPlayer = userPreferences.getValue().get(UserPreferences.Companion.getVideoPlayer());

                            Timber.i("User preferred player is: %s", preferredVideoPlayer);

                            if (preferredVideoPlayer == PreferredVideoPlayer.VLC) {
                                // Force VLC
                                useVlc = true;
                            } else if (preferredVideoPlayer == PreferredVideoPlayer.EXOPLAYER) {
                                // Make sure to not use VLC
                                useVlc = false;
                            } else if (preferredVideoPlayer == PreferredVideoPlayer.AUTO) {
                                // TODO: Clean up this logic
                                // Now look at both responses and choose the one that direct plays or bitstreams - favor VLC
                                useVlc = !vlcErrorEncountered &&
                                        !vlcResponse.getPlayMethod().equals(PlayMethod.Transcode) &&
                                        (DeviceUtils.is60() ||
                                                !userPreferences.getValue().get(UserPreferences.Companion.getAc3Enabled()) ||
                                                vlcResponse.getMediaSource() == null ||
                                                vlcResponse.getMediaSource().getDefaultAudioStream() == null ||
                                                (!"ac3".equals(vlcResponse.getMediaSource().getDefaultAudioStream().getCodec()) &&
                                                        !"truehd".equals(vlcResponse.getMediaSource().getDefaultAudioStream().getCodec()))) &&
                                        (Utils.downMixAudio(mFragment.getContext()) ||
                                                !DeviceUtils.is60() ||
                                                internalResponse.getPlayMethod().equals(PlayMethod.Transcode) ||
                                                !userPreferences.getValue().get(UserPreferences.Companion.getDtsEnabled()) ||
                                                internalResponse.getMediaSource() == null ||
                                                internalResponse.getMediaSource().getDefaultAudioStream() == null ||
                                                (vlcResponse.getMediaSource().getVideoStream() != null && vlcResponse.getMediaSource().getVideoStream().getWidth() < 1000));
                            } else if (preferredVideoPlayer == PreferredVideoPlayer.CHOOSE) {
                                PreferredVideoPlayer preferredVideoPlayerByPlayWith = systemPreferences.getValue().get(SystemPreferences.Companion.getChosenPlayer());

                                useVlc = preferredVideoPlayerByPlayWith == PreferredVideoPlayer.VLC;

                                Timber.i("PREFERRED PLAYER %s", preferredVideoPlayerByPlayWith.name());
                            }

                            Timber.i(useVlc ? "Preferring VLC" : "Will use internal player");
                            if (mVideoManager == null)
                                return;
                            mVideoManager.init(getBufferAmount(), useDeinterlacing);

                            Timber.d("server default: %s inferred first track: %s", internalResponse.getMediaSource().getDefaultAudioStreamIndex(), bestGuessAudioTrack(internalResponse.getMediaSource()));
                            mCurrentOptions = useVlc ? vlcOptions : internalOptions;
                            startItem(item, position, useVlc ? vlcResponse : internalResponse);
                        }

                        @Override
                        public void onError(Exception exception) {
                            Timber.e(exception, "Unable to get internal stream info");
                            mCurrentOptions = vlcOptions;
                            startItem(item, position, vlcResponse);
                        }
                    });

                }

                @Override
                public void onError(Exception exception) {
                    handlePlaybackInfoError(exception);
                }
            });
        }
    }

    private void handlePlaybackInfoError(Exception exception) {
        Timber.e(exception, "Error getting playback stream info");
        if (mFragment == null) return;
        if (exception instanceof PlaybackException) {
            PlaybackException ex = (PlaybackException) exception;
            switch (ex.getErrorCode()) {
                case NotAllowed:
                    Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.msg_playback_not_allowed));
                    break;
                case NoCompatibleStream:
                    Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.msg_playback_incompatible));
                    break;
                case RateLimitExceeded:
                    Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.msg_playback_restricted));
                    break;
            }
        } else {
            Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.msg_cannot_play));
        }
        // give the user a second to read the error message
        try {
            Thread.sleep(750);
        } catch (InterruptedException e) {
            Timber.e(e);
        }
        if (mFragment != null) mFragment.finish();
    }

    private void startItem(BaseItemDto item, long position, StreamInfo response) {
        if (!hasInitializedVideoManager() || !hasFragment()) {
            Timber.d("Error - attempting to play without:%s%s", hasInitializedVideoManager() ? "" : " [videoManager]", hasFragment() ? "" : " [overlay fragment]");
            return;
        }

        mStartPosition = position;

        mCurrentStreamInfo = response;

        // set the audio index so that it is preserved if the stream is rebuilt when seeking
        mCurrentOptions.setAudioStreamIndex(response.getMediaSource().getDefaultAudioStreamIndex());
        mCurrentOptions.setMediaSourceId(response.getMediaSource().getId());
        setDefaultAudioIndex(response);


        // get subtitle info
        mSubtitleStreams = response.GetSubtitleProfiles(false, apiClient.getValue().getApiUrl(), apiClient.getValue().getAccessToken());
        mDefaultSubIndex = response.getMediaSource().getDefaultSubtitleStreamIndex() != null ? response.getMediaSource().getDefaultSubtitleStreamIndex() : mDefaultSubIndex;
        Timber.d("default sub index set to %s remote default %s", mDefaultSubIndex, response.getMediaSource().getDefaultSubtitleStreamIndex());

        // if burning in, set the subtitle index and the burningSubs flag so that onPrepared and switchSubtitleStream will know that we already have subtitles enabled
        burningSubs = false;
        if (mCurrentStreamInfo.getPlayMethod() == PlayMethod.Transcode && getSubtitleStreamInfo(mDefaultSubIndex) != null &&
                getSubtitleStreamInfo(mDefaultSubIndex).getDeliveryMethod() == SubtitleDeliveryMethod.Encode) {
            mCurrentOptions.setSubtitleStreamIndex(mDefaultSubIndex);
            Timber.d("stream started with burnt in subs");
            burningSubs = true;
        } else {
            mCurrentOptions.setSubtitleStreamIndex(null);
        }

        Long mbPos = position * 10000;

        setPlaybackMethod(response.getPlayMethod());

        // Force VLC when media is not live TV and the preferred player is VLC
        boolean forceVlc = !isLiveTv && userPreferences.getValue().get(UserPreferences.Companion.getVideoPlayer()) == PreferredVideoPlayer.VLC;

        if (mVideoManager != null && (forceVlc || (useVlc && (!getPlaybackMethod().equals(PlayMethod.Transcode) || isLiveTv)))) {
            Timber.i("Playing back in VLC.");
            mVideoManager.setNativeMode(false);
        } else if (mVideoManager != null) {
            mVideoManager.setNativeMode(true);
            Timber.i("Playing back in native mode.");
        }

        // set refresh rate
        if (refreshRateSwitchingBehavior != RefreshRateSwitchingBehavior.DISABLED) {
            setRefreshRate(response.getMediaSource().getVideoStream());
        }

        // set playback speed to user selection, or 1 if we're watching live-tv
        if (mVideoManager != null)
            mVideoManager.setPlaybackSpeed(isLiveTv() ? 1.0f : mRequestedPlaybackSpeed);

        if (mFragment != null) mFragment.updateDisplay();

        // when using VLC if source is stereo or we're on the Fire platform with AC3 - use most compatible output
        if (mVideoManager != null && !mVideoManager.isNativeMode() &&
                ((isLiveTv && DeviceUtils.isFireTv()) ||
                        (response.getMediaSource() != null &&
                                response.getMediaSource().getDefaultAudioStream() != null &&
                                response.getMediaSource().getDefaultAudioStream().getChannels() != null &&
                                (response.getMediaSource().getDefaultAudioStream().getChannels() <= 2 ||
                                        (DeviceUtils.isFireTv() &&
                                                ("ac3".equals(response.getMediaSource().getDefaultAudioStream().getCodec()) ||
                                                        "truehd".equals(response.getMediaSource().getDefaultAudioStream().getCodec()))))))) {
            mVideoManager.setCompatibleAudio();
            Timber.i("Setting compatible audio mode...");
        } else if (mVideoManager != null) {
            mVideoManager.setAudioMode();
        }

        if (mVideoManager != null) {
            mVideoManager.setVideoPath(response.getMediaUrl());
            mVideoManager.setVideoTrack(response.getMediaSource());
        }

        // save the position where the stream starts. vlc gettime() will return ms since this point, which will be
        // added to this to get actual position
        if (mVideoManager != null && (forceVlc || useVlc) && getPlaybackMethod().equals(PlayMethod.Transcode)) {
            mVideoManager.setMetaVLCStreamStartPosition(position);
        }

        //wait a beat before attempting to start so the player surface is fully initialized and video is ready
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mVideoManager != null)
                    mVideoManager.start();
            }
        }, 750);

        dataRefreshService.getValue().setLastPlayedItem(ModelCompat.asSdk(item));
        ReportingHelper.reportStart(item, mbPos);
    }

    public void startSpinner() {
        spinnerOff = false;
    }

    public void stopSpinner() {
        spinnerOff = true;
    }

    public int getAudioStreamIndex() {
        int currIndex = mDefaultAudioIndex;

        // if not transcoding, libVLC & exoplayer are able to switch tracks and report their selected track
        if (hasInitializedVideoManager() && !isTranscoding() && mVideoManager.getVLCAudioTrack() > -1) {
            currIndex = mVideoManager.getVLCAudioTrack();
        } else if (hasInitializedVideoManager() && !isTranscoding() && isNativeMode() && mVideoManager.getExoPlayerTrack(MediaStreamType.Audio, getCurrentlyPlayingItem().getMediaStreams()) > -1) {
            currIndex = mVideoManager.getExoPlayerTrack(MediaStreamType.Audio, getCurrentlyPlayingItem().getMediaStreams());
        } else if (!isTranscoding() && isNativeMode() && mCurrentOptions.getAudioStreamIndex() != null) {
            currIndex = mCurrentOptions.getAudioStreamIndex();
        } else if (getCurrentMediaSource().getDefaultAudioStreamIndex() != null) {
            currIndex = getCurrentMediaSource().getDefaultAudioStreamIndex();
        }
        return currIndex;
    }

    private Integer bestGuessAudioTrack(MediaSourceInfo info) {
        if (info == null)
            return null;

        boolean videoFound = false;
        for (MediaStream track : info.getMediaStreams()) {
            if (track.getType() == MediaStreamType.Video) {
                videoFound = true;
            } else {
                if (videoFound && track.getType() == MediaStreamType.Audio)
                    return track.getIndex();
            }
        }
        return null;
    }

    private void setDefaultAudioIndex(StreamInfo info) {
        if (mDefaultAudioIndex != -1)
            return;

        if (bestGuessAudioTrack(info.getMediaSource()) != null)
            mDefaultAudioIndex = bestGuessAudioTrack(info.getMediaSource());
        Timber.d("default audio index set to %s", mDefaultAudioIndex);
    }

    public void switchAudioStream(int index) {
        if (!(isPlaying() || isPaused()) || index < 0)
            return;

        int currAudioIndex = getAudioStreamIndex();
        Timber.d("trying to switch audio stream from %s to %s", currAudioIndex, index);
        if (currAudioIndex == index) {
            Timber.d("skipping setting audio stream, already set to requested index %s", index);
            return;
        }

        // get current timestamp first
        refreshCurrentPosition();

        if (isNativeMode() && !isTranscoding() && mVideoManager.setExoPlayerTrack(index, MediaStreamType.Audio, getCurrentlyPlayingItem().getMediaStreams())) {
            mCurrentOptions.setMediaSourceId(getCurrentMediaSource().getId());
            mCurrentOptions.setAudioStreamIndex(index);
        } else if (!isNativeMode() && !isTranscoding() && mVideoManager.setVLCAudioTrack(index) == index) {
            // if setAudioTrack succeeded it will return the requested index
            mCurrentOptions.setMediaSourceId(getCurrentMediaSource().getId());
            mCurrentOptions.setAudioStreamIndex(index);
            mVideoManager.setAudioMode();
        } else {
            startSpinner();
            mCurrentOptions.setMediaSourceId(getCurrentMediaSource().getId());
            mCurrentOptions.setAudioStreamIndex(index);
            stop();
            playInternal(getCurrentlyPlayingItem(), mCurrentPosition, mCurrentOptions, mCurrentOptions);
            mPlaybackState = PlaybackState.BUFFERING;
        }
    }


    public void switchSubtitleStream(int index) {
        if (!hasInitializedVideoManager())
            return;
        // get current timestamp first
        refreshCurrentPosition();
        Timber.d("Setting subtitle index to: %d", index);

        // clear subtitles first
        if (mFragment != null) mFragment.addManualSubtitles(null);
        mVideoManager.disableSubs();
        // clear the default in case there's an error loading the subtitles
        mDefaultSubIndex = -1;

        // handle setting subtitles as disabled
        // restart playback if turning off burnt-in subtitles
        if (index < 0) {
            mCurrentOptions.setSubtitleStreamIndex(-1);
            if (burningSubs) {
                stop();
                play(mCurrentPosition, -1);
            }
            return;
        }

        MediaStream stream = StreamHelper.getMediaStream(getCurrentMediaSource(), index);
        if (stream == null) {
            if (mFragment != null)
                Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.subtitle_error));
            return;
        }
        SubtitleStreamInfo streamInfo = getSubtitleStreamInfo(index);
        if (streamInfo == null) {
            if (mFragment != null)
                Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.msg_unable_load_subs));
            return;
        }

        // handle switching on or between burnt-in subtitles, or switching to non-burnt subtitles
        // if switching from burnt-in subtitles to another type, playback still needs to be restarted
        if (burningSubs || streamInfo.getDeliveryMethod() == SubtitleDeliveryMethod.Encode) {
            stop();
            if (mFragment != null && streamInfo.getDeliveryMethod() == SubtitleDeliveryMethod.Encode)
                Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.msg_burn_sub_warning));
            play(mCurrentPosition, index);
            return;
        }

        // when burnt-in subtitles are selected, mCurrentOptions SubtitleStreamIndex is set in startItem() as soon as playback starts
        // otherwise mCurrentOptions SubtitleStreamIndex is kept null until now so we knew subtitles needed to be enabled but weren't already

        switch (streamInfo.getDeliveryMethod()) {
            case Embed:
                if (!mVideoManager.isNativeMode()) {
                    if (!mVideoManager.setSubtitleTrack(index, getCurrentlyPlayingItem().getMediaStreams())) {
                        // error selecting internal subs
                        if (mFragment != null)
                            Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.msg_unable_load_subs));
                    } else {
                        mCurrentOptions.setSubtitleStreamIndex(index);
                        mDefaultSubIndex = index;
                    }
                    break;
                }
                // not using vlc - fall through to external handling
            case External:
                if (mFragment != null) mFragment.showSubLoadingMsg(true);
                stream.setDeliveryMethod(SubtitleDeliveryMethod.External);
                stream.setDeliveryUrl(String.format("%1$s/Videos/%2$s/%3$s/Subtitles/%4$s/0/Stream.JSON", apiClient.getValue().getApiUrl(), mCurrentStreamInfo.getItemId(), mCurrentStreamInfo.getMediaSourceId(), String.valueOf(stream.getIndex())));
                apiClient.getValue().getSubtitles(stream.getDeliveryUrl(), new Response<SubtitleTrackInfo>() {

                    @Override
                    public void onResponse(final SubtitleTrackInfo info) {

                        if (info != null) {
                            Timber.d("Adding json subtitle track to player");
                            if (mFragment != null) mFragment.addManualSubtitles(info);
                            mCurrentOptions.setSubtitleStreamIndex(index);
                            mDefaultSubIndex = index;
                        } else {
                            Timber.e("Empty subtitle result");
                            if (mFragment != null) {
                                Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.msg_unable_load_subs));
                                mFragment.showSubLoadingMsg(false);
                            }
                        }
                    }

                    @Override
                    public void onError(Exception ex) {
                        Timber.e(ex, "Error downloading subtitles");
                        if (mFragment != null) {
                            Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.msg_unable_load_subs));
                            mFragment.showSubLoadingMsg(false);
                        }
                    }

                });
                break;
            case Hls:
                break;
        }
    }

    public void pause() {
        Timber.d("pause called at %s", mCurrentPosition);
        // if playback is paused and the seekbar is scrubbed, it will call pause even if already paused
        if (mPlaybackState == PlaybackState.PAUSED) {
            Timber.d("already paused, ignoring");
            return;
        }
        mPlaybackState = PlaybackState.PAUSED;
        if (hasInitializedVideoManager()) mVideoManager.pause();
        if (mFragment != null) {
            mFragment.setFadingEnabled(false);
            mFragment.setPlayPauseActionState(0);
        }

        stopReportLoop();
        // start a slower report for pause state to keep session alive
        startPauseReportLoop();

    }

    public void playPause() {
        switch (mPlaybackState) {
            case PLAYING:
                pause();
                break;
            case PAUSED:
            case IDLE:
                stopReportLoop();
                play(getCurrentPosition());
                break;
        }
    }

    public void stop() {
        refreshCurrentPosition();
        Timber.d("stop called at %s", mCurrentPosition);
        stopReportLoop();
        if (mPlaybackState != PlaybackState.IDLE && mPlaybackState != PlaybackState.UNDEFINED) {
            mPlaybackState = PlaybackState.IDLE;
            if (mVideoManager != null && mVideoManager.isPlaying()) mVideoManager.stopPlayback();
            //give it a just a beat to actually stop - this keeps it from re-requesting the stream after we tell the server we've stopped
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Timber.e(e);
            }
            Long mbPos = mCurrentPosition * 10000;
            ReportingHelper.reportStopped(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mbPos);
            if (!isLiveTv) {
                // update the actual items resume point
                getCurrentlyPlayingItem().getUserData().setPlaybackPositionTicks(mbPos);
            }
            clearPlaybackSessionOptions();
        }
    }

    public void endPlayback(Boolean closeActivity) {
        if (closeActivity) mFragment.getActivity().finish();
        stop();
        removePreviousQueueItems();
        if (mVideoManager != null)
            mVideoManager.destroy();
        mFragment = null;
        mVideoManager = null;
        resetPlayerErrors();
    }

    public void endPlayback(){
        endPlayback(false);
    }

    private void resetPlayerErrors() {
        vlcErrorEncountered = false;
        exoErrorEncountered = false;
    }

    private void clearPlaybackSessionOptions() {
        mDefaultSubIndex = -1;
        mDefaultAudioIndex = -1;
        mSeekPosition = -1;
        finishedInitialSeek = false;
        wasSeeking = false;
        burningSubs = false;
        if (mVideoManager != null)
            mVideoManager.setMetaVLCStreamStartPosition(-1);
    }

    public void next() {
        Timber.d("Next called.");
        if (mCurrentIndex < mItems.size() - 1) {
            stop();
            resetPlayerErrors();
            mCurrentIndex++;
            Timber.d("Moving to index: %d out of %d total items.", mCurrentIndex, mItems.size());
            spinnerOff = false;
            play(0);
        }
    }

    public void prev() {
        Timber.d("Prev called.");
        if (mCurrentIndex > 0 && mItems.size() > 0) {
            stop();
            resetPlayerErrors();
            mCurrentIndex--;
            Timber.d("Moving to index: %d out of %d total items.", mCurrentIndex, mItems.size());
            spinnerOff = false;
            play(0);
        }
    }

    public void fastForward() {
        UserSettingPreferences prefs = KoinJavaComponent.<UserSettingPreferences>get(UserSettingPreferences.class);
        skip(prefs.get(UserSettingPreferences.Companion.getSkipForwardLength()));
    }

    public void rewind() {
        UserSettingPreferences prefs = KoinJavaComponent.<UserSettingPreferences>get(UserSettingPreferences.class);
        skip(-prefs.get(UserSettingPreferences.Companion.getSkipBackLength()));
    }

    public void seek(long pos) {
        pos = Utils.getSafeSeekPosition(pos, getDuration());

        Timber.d("Trying to seek from %s to %d", mCurrentPosition, pos);
        Timber.d("Container: %s", mCurrentStreamInfo == null ? "unknown" : mCurrentStreamInfo.getContainer());

        if (!hasInitializedVideoManager()) {
            return;
        }

        if (wasSeeking) {
            Timber.d("Previous seek has not finished - cancelling seek from %s to %d", mCurrentPosition, pos);
            if (isPaused()) {
                refreshCurrentPosition();
                play(mCurrentPosition);
            }
            return;
        }
        wasSeeking = true;

        // set seekPosition so real position isn't used until playback starts again
        mSeekPosition = pos;

        // rebuild the stream for libVLC
        // if an older device uses exoplayer to play a transcoded stream but falls back to the generic http stream instead of hls, rebuild the stream
        if (!mVideoManager.isSeekable()) {
            Timber.d("Seek method - rebuilding the stream");
            //mkv transcodes require re-start of stream for seek
            mVideoManager.stopPlayback();
            mPlaybackState = PlaybackState.BUFFERING;

            // this value should only NOT be -1 if vlc is being used for transcoding
            if (!isNativeMode())
                mVideoManager.setMetaVLCStreamStartPosition(pos);

            playbackManager.getValue().changeVideoStream(mCurrentStreamInfo, api.getValue().getDeviceInfo(), mCurrentOptions, pos * 10000, apiClient.getValue(), new Response<StreamInfo>() {
                @Override
                public void onResponse(StreamInfo response) {
                    mCurrentStreamInfo = response;
                    if (mVideoManager != null) {
                        mVideoManager.setVideoPath(response.getMediaUrl());
                        mVideoManager.start();
                    }
                }

                @Override
                public void onError(Exception exception) {
                    if (mFragment != null)
                        Utils.showToast(mFragment.getContext(), R.string.msg_video_playback_error);
                    Timber.e(exception, "Error trying to seek transcoded stream");
                    // call stop so playback can be retried by the user
                    stop();
                }
            });
        } else {
            // use the same approach to directplay seeking as setOnProgressListener
            // set state to SEEKING
            // if seek succeeds call play and mirror the logic in play() for unpausing. if fails call pause()
            // stopProgressLoop() being called at the beginning of startProgressLoop keeps this from breaking. otherwise it would start twice
            // if seek() is called from skip()
            Timber.d("Seek method - native");
            mPlaybackState = PlaybackState.SEEKING;
            if (mVideoManager.seekTo(pos) < 0) {
                if (mFragment != null)
                    Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.seek_error));
                pause();
            } else {
                mVideoManager.play();
                mPlaybackState = PlaybackState.PLAYING;
                if (mFragment != null) mFragment.setFadingEnabled(true);
                startReportLoop();
            }
        }
    }

    private long currentSkipPos = 0;
    private final Runnable skipRunnable = () -> {
        if (!(isPlaying() || isPaused())) return; // in case we completed since this was requested

        seek(currentSkipPos);
        currentSkipPos = 0;
    };

    public void skip(int msec) {
        if (hasInitializedVideoManager() && (isPlaying() || isPaused()) && spinnerOff && mVideoManager.getCurrentPosition() > 0) { //guard against skipping before playback has truly begun
            pause();
            mHandler.removeCallbacks(skipRunnable);
            stopReportLoop();
            refreshCurrentPosition();
            currentSkipPos = Utils.getSafeSeekPosition((currentSkipPos == 0 ? mCurrentPosition : currentSkipPos) + msec, getDuration());

            Timber.d("Skip amount requested was %s. Calculated position is %s", msec, currentSkipPos);
            Timber.d("Duration reported as: %s current pos: %s", getDuration(), mCurrentPosition);

            mSeekPosition = currentSkipPos;
            mHandler.postDelayed(skipRunnable, 800);
        }
    }

    public void updateTvProgramInfo() {
        // Get the current program info when playing a live TV channel
        final BaseItemDto channel = getCurrentlyPlayingItem();
        if (channel.getBaseItemType() == BaseItemType.TvChannel) {
            apiClient.getValue().GetLiveTvChannelAsync(channel.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<ChannelInfoDto>() {
                @Override
                public void onResponse(ChannelInfoDto response) {
                    BaseItemDto program = response.getCurrentProgram();
                    if (program != null) {
                        channel.setName(program.getName() + liveTvChannelName);
                        channel.setPremiereDate(program.getStartDate());
                        channel.setEndDate(program.getEndDate());
                        channel.setOfficialRating(program.getOfficialRating());
                        channel.setOverview(program.getOverview());
                        channel.setRunTimeTicks(program.getRunTimeTicks());
                        channel.setCurrentProgram(program);
                        mCurrentProgramEndTime = channel.getEndDate() != null ? TimeUtils.convertToLocalDate(channel.getEndDate()).getTime() : 0;
                        mCurrentProgramStartTime = channel.getPremiereDate() != null ? TimeUtils.convertToLocalDate(channel.getPremiereDate()).getTime() : 0;
                        if (mFragment != null) mFragment.updateDisplay();
                    }
                }
            });

        }
    }

    private long getRealTimeProgress() {
        return System.currentTimeMillis() - mCurrentProgramStartTime;
    }

    private long getTimeShiftedProgress() {
        refreshCurrentPosition();
        return !directStreamLiveTv ? mCurrentPosition + (mCurrentTranscodeStartTime - mCurrentProgramStartTime) : getRealTimeProgress();
    }

    private void startReportLoop() {
        stopReportLoop();
        ReportingHelper.reportProgress(this, getCurrentlyPlayingItem(), getCurrentStreamInfo(), mCurrentPosition * 10000, false);
        mReportLoop = new Runnable() {
            @Override
            public void run() {
                if (isPlaying()) {
                    refreshCurrentPosition();
                    long currentTime = isLiveTv ? getTimeShiftedProgress() : mCurrentPosition;

                    ReportingHelper.reportProgress(PlaybackController.this, getCurrentlyPlayingItem(), getCurrentStreamInfo(), currentTime * 10000, false);
                }
                if (mPlaybackState != PlaybackState.UNDEFINED && mPlaybackState != PlaybackState.IDLE) {
                    mHandler.postDelayed(this, PROGRESS_REPORTING_INTERVAL);
                }
            }
        };
        mHandler.postDelayed(mReportLoop, PROGRESS_REPORTING_INTERVAL);
    }

    private void startPauseReportLoop() {
        stopReportLoop();
        ReportingHelper.reportProgress(this, getCurrentlyPlayingItem(), getCurrentStreamInfo(), mCurrentPosition * 10000, true);
        mReportLoop = new Runnable() {
            @Override
            public void run() {
                BaseItemDto currentItem = getCurrentlyPlayingItem();
                if (currentItem == null) {
                    // Loop was called while nothing was playing!
                    stopReportLoop();
                    return;
                }

                if (mPlaybackState != PlaybackState.PAUSED) {
                    // Playback is not paused anymore, stop reporting
                    return;
                }
                refreshCurrentPosition();
                long currentTime = isLiveTv ? getTimeShiftedProgress() : mCurrentPosition;
                if (isLiveTv && !directStreamLiveTv && mFragment != null) {
                    mFragment.setSecondaryTime(getRealTimeProgress());
                }

                ReportingHelper.reportProgress(PlaybackController.this, currentItem, getCurrentStreamInfo(), currentTime * 10000, true);
                mHandler.postDelayed(this, PROGRESS_REPORTING_PAUSE_INTERVAL);
            }
        };
        mHandler.postDelayed(mReportLoop, PROGRESS_REPORTING_PAUSE_INTERVAL);
    }

    private void stopReportLoop() {
        if (mHandler != null && mReportLoop != null) {
            mHandler.removeCallbacks(mReportLoop);
        }
    }

    private void initialSeek(final long position) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mVideoManager == null)
                    return;
                if (mVideoManager.getDuration() <= 0) {
                    // use mVideoManager.getDuration here for accurate results
                    // wait until we have valid duration
                    mHandler.postDelayed(this, 25);
                } else if (mVideoManager.isSeekable()) {
                    seek(position);
                } else {
                    finishedInitialSeek = true;
                }
            }
        });
    }

    public void removePreviousQueueItems() {
        DataRefreshService dataRefreshService = KoinJavaComponent.<DataRefreshService>get(DataRefreshService.class);
        dataRefreshService.setLastVideoQueueChange(System.currentTimeMillis());
        if (isLiveTv || !mediaManager.getValue().isVideoQueueModified()) {
            mediaManager.getValue().clearVideoQueue();
            return;
        }

        if (mCurrentIndex < 0) return;

        // removing from mItems doesn't work properly when using remote control - modify via mediaManager instead
        for (int i = 0; i < mCurrentIndex && i < mediaManager.getValue().getCurrentVideoQueue().size(); i++) {
            mediaManager.getValue().getCurrentVideoQueue().remove(0);
        }

        //Now - look at last item played and, if beyond default resume point, remove it too
        Long duration = mCurrentStreamInfo != null ? mCurrentStreamInfo.getRunTimeTicks() : null;
        if (duration != null && mediaManager.getValue().getCurrentVideoQueue().size() > 0) {
            if (duration < 300000 || mCurrentPosition * 10000 > Math.floor(.90 * duration))
                mediaManager.getValue().getCurrentVideoQueue().remove(0);
        } else if (duration == null) mediaManager.getValue().getCurrentVideoQueue().remove(0);
        setItems(mediaManager.getValue().getCurrentVideoQueue());
    }

    private void itemComplete() {
        stop();
        resetPlayerErrors();

        BaseItemDto nextItem = getNextItem();
        if (nextItem != null) {
            Timber.d("Moving to next queue item. Index: %s", (mCurrentIndex + 1));

            BaseItemDto curItem = getCurrentlyPlayingItem();

            if (userPreferences.getValue().get(UserPreferences.Companion.getNextUpBehavior()) != NextUpBehavior.DISABLED
                    && (curItem == null || curItem.getBaseItemType() != BaseItemType.Trailer)) {
                // Show "Next Up" fragment
                spinnerOff = false;
                mediaManager.getValue().setCurrentVideoQueue(mItems);
                mediaManager.getValue().setVideoQueueModified(true);
                if (mFragment != null) mFragment.showNextUp(nextItem.getId());
                endPlayback();
            } else {
                mCurrentIndex++;
                play(0);
            }
        } else {
            // exit activity
            Timber.d("Last item completed. Finishing activity.");
            if (mFragment != null) mFragment.finish();
        }
    }

    @Override
    public void onPlaybackSpeedChange(float newSpeed) {
        // TODO, implement speed change handling
    }

    @Override
    public void onPrepared() {
        if (mPlaybackState == PlaybackState.BUFFERING) {
            if (mFragment != null) mFragment.setFadingEnabled(true);

            mPlaybackState = PlaybackState.PLAYING;
            mCurrentTranscodeStartTime = mCurrentStreamInfo.getPlayMethod() == PlayMethod.Transcode ? System.currentTimeMillis() : 0;
            startReportLoop();
        }

        Timber.i("Play method: %s", mCurrentStreamInfo.getPlayMethod() == PlayMethod.Transcode ? "Trans" : "Direct");

        if (mPlaybackState == PlaybackState.PAUSED) {
            mPlaybackState = PlaybackState.PLAYING;
        } else {
            // select or disable subtitles
            Integer currentSubtitleIndex = mCurrentOptions.getSubtitleStreamIndex();
            if (mDefaultSubIndex >= 0 && currentSubtitleIndex != null && currentSubtitleIndex == mDefaultSubIndex) {
                Timber.i("subtitle stream %s is already selected", mDefaultSubIndex);
            } else {
                if (mDefaultSubIndex < 0)
                    Timber.i("Turning off subs");
                else
                    Timber.i("Enabling default sub stream: %d", mDefaultSubIndex);
                switchSubtitleStream(mDefaultSubIndex);
            }

            // select an audio track
            int eligibleAudioTrack = mDefaultAudioIndex;

            // if track switching is done without rebuilding the stream, mCurrentOptions is updated
            // otherwise, use the server default
            if (mCurrentOptions.getAudioStreamIndex() != null) {
                eligibleAudioTrack = mCurrentOptions.getAudioStreamIndex();
            } else if (getCurrentMediaSource().getDefaultAudioStreamIndex() != null) {
                eligibleAudioTrack = getCurrentMediaSource().getDefaultAudioStreamIndex();
            }
            switchAudioStream(eligibleAudioTrack);
        }
    }

    @Override
    public void onError() {
        if (mFragment == null) {
            playerErrorEncountered();
            return;
        }

        if (isLiveTv && directStreamLiveTv) {
            Utils.showToast(mFragment.getContext(), mFragment.getString(R.string.msg_error_live_stream));
            directStreamLiveTv = false;
        } else {
            String msg = mFragment.getString(R.string.video_error_unknown_error);
            Timber.e("Playback error - %s", msg);
        }
        playerErrorEncountered();
    }

    @Override
    public void onCompletion() {
        Timber.d("On Completion fired");
        itemComplete();
    }

    @Override
    public void onProgress() {
        refreshCurrentPosition();
        if (isPlaying()) {
            if (!spinnerOff) {
                if (mStartPosition > 0) {
                    initialSeek(mStartPosition);
                    mStartPosition = 0;
                } else {
                    finishedInitialSeek = true;
                    stopSpinner();
                }
            }

            if (isLiveTv && mCurrentProgramEndTime > 0 && System.currentTimeMillis() >= mCurrentProgramEndTime) {
                // crossed fire off an async routine to update the program info
                updateTvProgramInfo();
            }
            if (mFragment != null && finishedInitialSeek)
                mFragment.updateSubtitles(mCurrentPosition);
        }
        if (mFragment != null)
            mFragment.setCurrentTime(mCurrentPosition);
    }

    public long getDuration() {
        long duration = 0;

        if (hasInitializedVideoManager()) {
            duration = mVideoManager.getDuration();
        } else if (getCurrentlyPlayingItem() != null && getCurrentlyPlayingItem().getRunTimeTicks() != null) {
            duration = getCurrentlyPlayingItem().getRunTimeTicks() / 10000;
        }
        return duration > 0 ? duration : 0;
    }

    public long getBufferedPosition() {
        long bufferedPosition = -1;

        if (hasInitializedVideoManager())
            bufferedPosition = mVideoManager.getBufferedPosition();

        if (bufferedPosition < 0)
            bufferedPosition = getDuration();

        return bufferedPosition;
    }

    public long getCurrentPosition() {
        // don't report the real position if seeking
        return !isPlaying() && mSeekPosition != -1 ? mSeekPosition : mCurrentPosition;
    }

    public boolean isPaused() {
        return mPlaybackState == PlaybackState.PAUSED;
    }

    public int getZoomMode() {
        return hasInitializedVideoManager() ? mVideoManager.getZoomMode() : 0;
    }

    public void setZoom(int mode) {
        if (hasInitializedVideoManager())
            mVideoManager.setZoom(mode);
    }

    /**
     * List of various states that we can be in
     */
    public enum PlaybackState {
        PLAYING,
        PAUSED,
        BUFFERING,
        IDLE,
        SEEKING,
        UNDEFINED,
        ERROR
    }
}
