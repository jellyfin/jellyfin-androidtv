package org.jellyfin.androidtv.ui.playback;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.Display;
import android.view.WindowManager;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.ContainerTypes;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.data.compat.PlaybackException;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.data.compat.SubtitleStreamInfo;
import org.jellyfin.androidtv.data.compat.VideoOptions;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer;
import org.jellyfin.androidtv.ui.ImageButton;
import org.jellyfin.androidtv.util.DeviceUtils;
import org.jellyfin.androidtv.util.ProfileHelper;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.androidtv.util.apiclient.ReportingHelper;
import org.jellyfin.androidtv.util.apiclient.StreamHelper;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile;
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

import java.util.List;

import timber.log.Timber;

public class PlaybackController {
    // Frequency to report playback progress
    private final static long PROGRESS_REPORTING_INTERVAL = TimeUtils.secondsToMillis(3);
    // Frequency to report paused state
    private static final long PROGRESS_REPORTING_PAUSE_INTERVAL = TimeUtils.secondsToMillis(15);

    List<BaseItemDto> mItems;
    VideoManager mVideoManager;
    int mCurrentIndex = 0;
    private long mCurrentPosition = 0;
    private PlaybackState mPlaybackState = PlaybackState.IDLE;
    private TvApp mApplication;

    private StreamInfo mCurrentStreamInfo;
    private List<SubtitleStreamInfo> mSubtitleStreams;

    private IPlaybackOverlayFragment mFragment;
    private Boolean spinnerOff = false;

    private VideoOptions mCurrentOptions;
    private int mDefaultSubIndex = -1;
    private int mDefaultAudioIndex = -1;

    private PlayMethod mPlaybackMethod = PlayMethod.Transcode;

    private Runnable mReportLoop;
    private Handler mHandler;

    private long mStartPosition = 0;
    private long mCurrentProgramEndTime;
    private long mCurrentProgramStartTime;
    private long mCurrentTranscodeStartTime;
    private boolean isLiveTv;
    private boolean directStreamLiveTv;
    private String liveTvChannelName = "";
    private boolean useVlc;

    private boolean vlcErrorEncountered;
    private boolean exoErrorEncountered;
    private int playbackRetries = 0;

    private boolean updateProgress = true;

    private Display.Mode[] mDisplayModes;
    private boolean refreshRateSwitchingEnabled;

    public PlaybackController(List<BaseItemDto> items, IPlaybackOverlayFragment fragment) {
        mItems = items;
        mFragment = fragment;
        mApplication = TvApp.getApplication();
        mHandler = new Handler();

        refreshRateSwitchingEnabled = DeviceUtils.is60() && mApplication.getUserPreferences().get(UserPreferences.Companion.getRefreshRateSwitchingEnabled());
        if (refreshRateSwitchingEnabled) getDisplayModes();

        // Set default value for useVlc field
        // when set to auto the default will be exoplayer
        useVlc = mApplication.getUserPreferences().get(UserPreferences.Companion.getVideoPlayer()) == PreferredVideoPlayer.VLC;
    }

    public void init(VideoManager mgr) {
        mVideoManager = mgr;
        directStreamLiveTv = mApplication.getUserPreferences().get(UserPreferences.Companion.getLiveTvDirectPlayEnabled());
        setupCallbacks();
    }

    public void setItems(List<BaseItemDto> items) {
        mItems = items;
        mCurrentIndex = 0;
    }

    public PlayMethod getPlaybackMethod() {
        return mPlaybackMethod;
    }

    public void setPlaybackMethod(PlayMethod value) {
        mPlaybackMethod = value;
    }

    public BaseItemDto getCurrentlyPlayingItem() {
        return mItems.size() > mCurrentIndex ? mItems.get(mCurrentIndex) : null;
    }
    public MediaSourceInfo getCurrentMediaSource() { return mCurrentStreamInfo != null && mCurrentStreamInfo.getMediaSource() != null ? mCurrentStreamInfo.getMediaSource() : getCurrentlyPlayingItem().getMediaSources().get(0);}
    public StreamInfo getCurrentStreamInfo() { return mCurrentStreamInfo; }
    public boolean canSeek() {return !isLiveTv;}
    public boolean isLiveTv() { return isLiveTv; }
    public int getSubtitleStreamIndex() {return (mCurrentOptions != null && mCurrentOptions.getSubtitleStreamIndex() != null) ? mCurrentOptions.getSubtitleStreamIndex() : -1; }
    public Integer getAudioStreamIndex() {
        return isTranscoding() ? mCurrentStreamInfo.getAudioStreamIndex() != null ? mCurrentStreamInfo.getAudioStreamIndex() : mCurrentOptions.getAudioStreamIndex() : mVideoManager.getAudioTrack() > -1 ? Integer.valueOf(mVideoManager.getAudioTrack()) : bestGuessAudioTrack(getCurrentMediaSource());
    }
    public List<SubtitleStreamInfo> getSubtitleStreams() { return mSubtitleStreams; }
    public SubtitleStreamInfo getSubtitleStreamInfo(int index) {
        for (SubtitleStreamInfo info : mSubtitleStreams) {
            if (info.getIndex() == index) return info;
        }

        return null;
    }

    public boolean isNativeMode() { return mVideoManager == null || mVideoManager.isNativeMode(); }

    public boolean isTranscoding() { return mCurrentStreamInfo != null && mCurrentStreamInfo.getPlayMethod() == PlayMethod.Transcode; }

    public boolean hasNextItem() { return mCurrentIndex < mItems.size() - 1; }
    public BaseItemDto getNextItem() { return hasNextItem() ? mItems.get(mCurrentIndex+1) : null; }

    public boolean isPlaying() {
        return mPlaybackState == PlaybackState.PLAYING;
    }

    public void setAudioDelay(long value) { if (mVideoManager != null) mVideoManager.setAudioDelay(value);}
    public long getAudioDelay() { return mVideoManager != null ? mVideoManager.getAudioDelay() : 0;}

    private Integer bestGuessAudioTrack(MediaSourceInfo info) {

        if (info != null) {
            boolean videoFound = false;
            for (MediaStream track : info.getMediaStreams()) {
                if (track.getType() == MediaStreamType.Video) {
                    videoFound = true;
                } else {
                    if (videoFound && track.getType() == MediaStreamType.Audio) return track.getIndex();
                }
            }
        }

        return null;
    }

    public void playerErrorEncountered() {
        if (mVideoManager.isNativeMode()) exoErrorEncountered = true; else vlcErrorEncountered = true;
        playbackRetries++;

        if (playbackRetries < 3) {
            Utils.showToast(mApplication, mApplication.getString(R.string.player_error));
            Timber.i("Player error encountered - retrying");
            stop();
            play(mCurrentPosition);

        } else {
            Utils.showToast(mApplication, mApplication.getString(R.string.too_many_errors));
            mPlaybackState = PlaybackState.ERROR;
            stop();
            mFragment.finish();
        }
    }

    @TargetApi(23)
    private void getDisplayModes() {
        Display display = mApplication.getCurrentActivity().getWindowManager().getDefaultDisplay();
        mDisplayModes = display.getSupportedModes();
        Timber.i("** Available display refresh rates:");
        for (Display.Mode mDisplayMode : mDisplayModes) {
            Timber.i("%f", mDisplayMode.getRefreshRate());
        }

    }

    @TargetApi(23)
    private Display.Mode findBestDisplayMode(Float refreshRate) {
        if (mDisplayModes == null || refreshRate == null) return null;

        int sourceRate = Math.round(refreshRate);
        for (Display.Mode mode : mDisplayModes){
            int rate = Math.round(mode.getRefreshRate());
            if (rate == sourceRate || rate == sourceRate * 2) return mode;
        }

        return null;
    }

    @TargetApi(23)
    private void setRefreshRate(MediaStream videoStream) {
        if (videoStream == null) {
            Timber.e("Null video stream attempting to set refresh rate");
            return;
        }

        Display.Mode current = mApplication.getCurrentActivity().getWindowManager().getDefaultDisplay().getMode();
        Display.Mode best = findBestDisplayMode(videoStream.getRealFrameRate());
        if (best != null) {
            Timber.i("*** Best refresh mode is: %s/%s",best.getModeId(), best.getRefreshRate());
            if (current.getModeId() != best.getModeId()) {
                Timber.i("*** Attempting to change refresh rate from %s/%s",current.getModeId(), current.getRefreshRate());
                WindowManager.LayoutParams params = mApplication.getCurrentActivity().getWindow().getAttributes();
                params.preferredDisplayModeId = best.getModeId();
                mApplication.getCurrentActivity().getWindow().setAttributes(params);
            } else {
                Timber.i("Display is already in best mode");
            }
        } else {
            Timber.i("*** Unable to find display mode for refresh rate: %s",videoStream.getRealFrameRate());
        }


    }

    public void play(long position) {
        play(position, -1);
    }

    private void play(long position, int transcodedSubtitle) {
        Timber.d("Play called with pos: %d and sub index: %d", position, transcodedSubtitle);

        if (position < 0) {
            Timber.i("Negative start requested - adjusting to zero");
            position = 0;
        }

        switch (mPlaybackState) {
            case PLAYING:
                // do nothing
                break;
            case PAUSED:
                // just resume
                mVideoManager.play();
                if (mVideoManager.isNativeMode()) mPlaybackState = PlaybackState.PLAYING; //won't get another onprepared call
                if (mFragment != null) {
                    mFragment.setFadingEnabled(true);
                    mFragment.setPlayPauseActionState(ImageButton.STATE_SECONDARY);
                }
                startReportLoop();
                break;
            case BUFFERING:
                // onPrepared should take care of it
                break;
            case IDLE:
                // start new playback
                BaseItemDto item = getCurrentlyPlayingItem();

                // make sure item isn't missing
                if (item.getLocationType() == LocationType.Virtual) {
                    if (hasNextItem()) {
                        new AlertDialog.Builder(mApplication.getCurrentActivity())
                                .setTitle(R.string.episode_missing)
                                .setMessage(R.string.episode_missing_message)
                                .setPositiveButton(mApplication.getResources().getString(R.string.lbl_yes), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        next();
                                    }
                                })
                                .setNegativeButton(mApplication.getResources().getString(R.string.lbl_no), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mApplication.getCurrentActivity().finish();
                                    }
                                })
                                .create()
                                .show();
                        return;
                    } else {
                        new AlertDialog.Builder(mApplication.getCurrentActivity())
                                .setTitle(R.string.episode_missing)
                                .setMessage(R.string.episode_missing_message_2)
                                .setPositiveButton(mApplication.getResources().getString(R.string.lbl_ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mApplication.getCurrentActivity().finish();
                                    }
                                })
                                .create()
                                .show();
                        return;

                    }
                }

                // confirm we actually can play
                if (item.getPlayAccess() != PlayAccess.Full) {
                    String msg = item.getIsPlaceHolder() ? mApplication.getString(R.string.msg_cannot_play) : mApplication.getString(R.string.msg_cannot_play_time);
                    Utils.showToast(TvApp.getApplication(), msg);
                    return;
                }

                isLiveTv = item.getBaseItemType() == BaseItemType.TvChannel;
                startSpinner();

                //Build options for each player
                VideoOptions vlcOptions = new VideoOptions();
                vlcOptions.setDeviceId(mApplication.getApiClient().getDeviceId());
                vlcOptions.setItemId(item.getId());
                vlcOptions.setMediaSources(item.getMediaSources());
                vlcOptions.setMaxBitrate(Utils.getMaxBitrate());
                if (vlcErrorEncountered) {
                    Timber.i("*** Disabling direct play/stream due to previous error");
                    vlcOptions.setEnableDirectStream(false);
                    vlcOptions.setEnableDirectPlay(false);
                }
                vlcOptions.setSubtitleStreamIndex(transcodedSubtitle >= 0 ? transcodedSubtitle : null);
                vlcOptions.setMediaSourceId(transcodedSubtitle >= 0 ? getCurrentMediaSource().getId() : null);
                DeviceProfile vlcProfile = ProfileHelper.getBaseProfile(isLiveTv);
                ProfileHelper.setVlcOptions(vlcProfile, isLiveTv);
                vlcOptions.setProfile(vlcProfile);

                VideoOptions internalOptions = new VideoOptions();
                internalOptions.setDeviceId(mApplication.getApiClient().getDeviceId());
                internalOptions.setItemId(item.getId());
                internalOptions.setMediaSources(item.getMediaSources());
                internalOptions.setMaxBitrate(Utils.getMaxBitrate());
                if (exoErrorEncountered || (isLiveTv && !directStreamLiveTv)) internalOptions.setEnableDirectStream(false);
                internalOptions.setMaxAudioChannels(Utils.downMixAudio() ? 2 : null); //have to downmix at server
                internalOptions.setSubtitleStreamIndex(transcodedSubtitle >= 0 ? transcodedSubtitle : null);
                internalOptions.setMediaSourceId(transcodedSubtitle >= 0 ? getCurrentMediaSource().getId() : null);
                DeviceProfile internalProfile = ProfileHelper.getBaseProfile(isLiveTv);
                if (DeviceUtils.is60() || mApplication.getUserPreferences().get(UserPreferences.Companion.getAc3Enabled())) {
                    ProfileHelper.setExoOptions(internalProfile, isLiveTv, true);
                    ProfileHelper.addAc3Streaming(internalProfile, true);
                    Timber.i("*** Using extended Exoplayer profile options");

                } else {
                    Timber.i("*** Using default android profile");
                }
                internalOptions.setProfile(internalProfile);

                mDefaultSubIndex = transcodedSubtitle;
                Timber.d("Max bitrate is: %d", Utils.getMaxBitrate());

                playInternal(getCurrentlyPlayingItem(), position, vlcOptions, internalOptions);
                mPlaybackState = PlaybackState.BUFFERING;
                if (mFragment != null) {
                    mFragment.setPlayPauseActionState(ImageButton.STATE_SECONDARY);
                    mFragment.setFadingEnabled(true);
                    mFragment.setCurrentTime(position);
                }

                long duration = getCurrentlyPlayingItem().getRunTimeTicks()!= null ? getCurrentlyPlayingItem().getRunTimeTicks() / 10000 : -1;
                mVideoManager.setMetaDuration(duration);

                break;
        }
    }

    public int getBufferAmount() {
        return 600;
    }

    private void playInternal(final BaseItemDto item, final Long position, final VideoOptions vlcOptions, final VideoOptions internalOptions) {
        final ApiClient apiClient = mApplication.getApiClient();
        if (isLiveTv) {
            liveTvChannelName = " ("+item.getName()+")";
            updateTvProgramInfo();
            TvManager.setLastLiveTvChannel(item.getId());
            //Choose appropriate player now to avoid opening two streams
            if (!directStreamLiveTv || mApplication.getUserPreferences().get(UserPreferences.Companion.getLiveTvVideoPlayer()) != PreferredVideoPlayer.VLC) {
                //internal/exo player
                Timber.i("Using internal player for Live TV");
                mApplication.getPlaybackManager().getVideoStreamInfo(apiClient.getServerInfo().getId(), internalOptions, position * 10000, false, apiClient, new Response<StreamInfo>() {
                    @Override
                    public void onResponse(StreamInfo response) {
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
                mApplication.getPlaybackManager().getVideoStreamInfo(apiClient.getServerInfo().getId(), vlcOptions, position * 10000, false, apiClient, new Response<StreamInfo>() {
                    @Override
                    public void onResponse(StreamInfo response) {
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
            mApplication.getPlaybackManager().getVideoStreamInfo(apiClient.getServerInfo().getId(), vlcOptions, position * 10000, false, apiClient, new Response<StreamInfo>() {
                @Override
                public void onResponse(final StreamInfo vlcResponse) {
                    Timber.i("VLC would %s", vlcResponse.getPlayMethod().equals(PlayMethod.Transcode) ? "transcode" : "direct stream");
                    mApplication.getPlaybackManager().getVideoStreamInfo(apiClient.getServerInfo().getId(), internalOptions, position * 10000, false, apiClient, new Response<StreamInfo>() {
                        @Override
                        public void onResponse(StreamInfo internalResponse) {
                            Timber.i("Internal player would %s", internalResponse.getPlayMethod().equals(PlayMethod.Transcode) ? "transcode" : "direct stream");
                            boolean useDeinterlacing = vlcResponse.getMediaSource().getVideoStream() != null &&
                                    vlcResponse.getMediaSource().getVideoStream().getIsInterlaced() &&
                                    (vlcResponse.getMediaSource().getVideoStream().getWidth() == null ||
                                            vlcResponse.getMediaSource().getVideoStream().getWidth() > 1200);
                            Timber.i(useDeinterlacing ? "Explicit deinterlacing will be used" : "Explicit deinterlacing will NOT be used");

                            PreferredVideoPlayer preferredVideoPlayer = mApplication.getUserPreferences().get(UserPreferences.Companion.getVideoPlayer());

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
                                                !mApplication.getUserPreferences().get(UserPreferences.Companion.getAc3Enabled()) ||
                                                vlcResponse.getMediaSource() == null ||
                                                vlcResponse.getMediaSource().getDefaultAudioStream() == null ||
                                                (!"ac3".equals(vlcResponse.getMediaSource().getDefaultAudioStream().getCodec()) &&
                                                        !"truehd".equals(vlcResponse.getMediaSource().getDefaultAudioStream().getCodec()))) &&
                                        (Utils.downMixAudio() ||
                                                !DeviceUtils.is60() ||
                                                internalResponse.getPlayMethod().equals(PlayMethod.Transcode) ||
                                                !mApplication.getUserPreferences().get(UserPreferences.Companion.getDtsEnabled()) ||
                                                internalResponse.getMediaSource() == null ||
                                                internalResponse.getMediaSource().getDefaultAudioStream() == null ||
                                                (!internalResponse.getMediaSource().getDefaultAudioStream().getCodec().equals("dca") &&
                                                        !internalResponse.getMediaSource().getDefaultAudioStream().getCodec().equals("dts"))) &&
                                        (!DeviceUtils.isFireTvStick() ||
                                                (vlcResponse.getMediaSource().getVideoStream() != null && vlcResponse.getMediaSource().getVideoStream().getWidth() < 1000));
                            }

                            Timber.i(useVlc ? "Preferring VLC" : "Will use internal player");
                            mVideoManager.init(getBufferAmount(), useDeinterlacing);
                            if (!useVlc && (internalOptions.getAudioStreamIndex() != null && !internalOptions.getAudioStreamIndex().equals(bestGuessAudioTrack(internalResponse.getMediaSource())))) {
                                // requested specific audio stream that is different from default so we need to force a transcode to get it (ExoMedia currently cannot switch)
                                // remove direct play profiles to force the transcode
                                final DeviceProfile save = internalOptions.getProfile();
                                DeviceProfile newProfile = ProfileHelper.getBaseProfile(isLiveTv);
                                ProfileHelper.setExoOptions(newProfile, isLiveTv, true);
                                if (!Utils.downMixAudio()) ProfileHelper.addAc3Streaming(newProfile, true);
                                newProfile.setDirectPlayProfiles(new DirectPlayProfile[]{});
                                internalOptions.setProfile(newProfile);
                                Timber.i("Forcing transcode due to non-default audio chosen");
                                mApplication.getPlaybackManager().getVideoStreamInfo(apiClient.getServerInfo().getId(), internalOptions, position * 10000, false, apiClient, new Response<StreamInfo>() {
                                    @Override
                                    public void onResponse(StreamInfo response) {
                                        //re-set this
                                        internalOptions.setProfile(save);
                                        mCurrentOptions = internalOptions;
                                        startItem(item, position, response);
                                    }
                                });
                            } else {
                                mCurrentOptions = useVlc ? vlcOptions : internalOptions;
                                startItem(item, position, useVlc ? vlcResponse : internalResponse);
                            }
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
        if (exception instanceof PlaybackException) {
            PlaybackException ex = (PlaybackException) exception;
            switch (ex.getErrorCode()) {
                case NotAllowed:
                    Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_playback_not_allowed));
                    break;
                case NoCompatibleStream:
                    Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_playback_incompatible));
                    break;
                case RateLimitExceeded:
                    Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_playback_restricted));
                    break;
            }
        }

    }

    private void startItem(BaseItemDto item, long position, StreamInfo response) {
        mCurrentStreamInfo = response;
        Long mbPos = position * 10000;

        setPlaybackMethod(response.getPlayMethod());

        // Force VLC when media is not live TV and the preferred player is VLC
        boolean forceVlc = !isLiveTv && mApplication.getUserPreferences().get(UserPreferences.Companion.getVideoPlayer()) == PreferredVideoPlayer.VLC;

        if (forceVlc || (useVlc && (!getPlaybackMethod().equals(PlayMethod.Transcode) || isLiveTv))) {
            Timber.i("Playing back in VLC.");
            mVideoManager.setNativeMode(false);
        } else {
            mVideoManager.setNativeMode(true);
            Timber.i("Playing back in native mode.");
            if (Utils.downMixAudio()) {
                Timber.i("Setting max audio to 2-channels");
                mCurrentStreamInfo.setMaxAudioChannels(2);
            }

        }

        // set refresh rate
        if (refreshRateSwitchingEnabled) {
            setRefreshRate(response.getMediaSource().getVideoStream());
        }

        // get subtitle info
        mSubtitleStreams = response.GetSubtitleProfiles(false, mApplication.getApiClient().getApiUrl(), mApplication.getApiClient().getAccessToken());

        mFragment.updateDisplay();
        String path = response.getMediaUrl();

        // when using VLC if source is stereo or we're on the Fire platform with AC3 - use most compatible output
        if (!mVideoManager.isNativeMode() &&
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
        } else {
            mVideoManager.setAudioMode();
        }

        mVideoManager.setVideoPath(path);
        mVideoManager.setVideoTrack(response.getMediaSource());

        //wait a beat before attempting to start so the player surface is fully initialized and video is ready
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mVideoManager.start();
            }
        },750);

        mStartPosition = position;

        mDefaultAudioIndex = getDefaultAudioIndex(response);
        mDefaultSubIndex = mPlaybackMethod != PlayMethod.Transcode && response.getMediaSource().getDefaultSubtitleStreamIndex() != null ? response.getMediaSource().getDefaultSubtitleStreamIndex() : mDefaultSubIndex;

        mApplication.setLastPlayedItem(item);
        if (!isRestart) ReportingHelper.reportStart(item, mbPos);
        isRestart = false;
    }

    private int getDefaultAudioIndex(StreamInfo info) {
        return mPlaybackMethod != PlayMethod.Transcode && info.getMediaSource().getDefaultAudioStreamIndex() != null ? info.getMediaSource().getDefaultAudioStreamIndex() : -1;
    }

    public void startSpinner() {
        spinnerOff = false;
    }

    public void stopSpinner() {
        spinnerOff = true;
    }

    public void switchAudioStream(int index) {
        if (!isPlaying()) return;

        mCurrentOptions.setAudioStreamIndex(index);
        if (mVideoManager.isNativeMode()) {
            startSpinner();
            Timber.d("Setting audio index to: %d", index);
            mCurrentOptions.setMediaSourceId(getCurrentMediaSource().getId());
            stop();
            playInternal(getCurrentlyPlayingItem(), mCurrentPosition, mCurrentOptions, mCurrentOptions);
            mPlaybackState = PlaybackState.BUFFERING;
        } else {
            mVideoManager.setAudioTrack(index, getCurrentMediaSource().getMediaStreams());
            mVideoManager.setAudioMode();
        }
    }

    private boolean burningSubs = false;

    public void switchSubtitleStream(int index) {
        Timber.d("Setting subtitle index to: %d", index);
        mCurrentOptions.setSubtitleStreamIndex(index >= 0 ? index : null);

        if (index < 0) {
            if (burningSubs) {
                stop();
                play(mCurrentPosition);
                burningSubs = false;
            } else {
                mFragment.addManualSubtitles(null);
                mVideoManager.disableSubs();
            }
            return;
        }

        MediaStream stream = StreamHelper.getMediaStream(getCurrentMediaSource(), index);
        if (stream == null) {
            Utils.showToast(mApplication, mApplication.getString(R.string.subtitle_error));
            return;
        }


        // handle according to delivery method
        SubtitleStreamInfo streamInfo = getSubtitleStreamInfo(index);
        if (streamInfo == null) {
            Utils.showToast(mApplication, mApplication.getResources().getString(R.string.msg_unable_load_subs));
        } else {
            switch (streamInfo.getDeliveryMethod()) {

                case Encode:
                    // Gonna need to burn in so start a transcode with the sub index
                    stop();
                    if (!mVideoManager.isNativeMode()) {
                        Utils.showToast(mApplication, mApplication.getResources().getString(R.string.msg_burn_sub_warning));
                    }
                    play(mCurrentPosition, index);
                    break;
                case Embed:
                    if (!mVideoManager.isNativeMode()) {
                        mFragment.addManualSubtitles(null); // in case these were on
                        if (!mVideoManager.setSubtitleTrack(index, getCurrentlyPlayingItem().getMediaStreams())) {
                            // error selecting internal subs
                            Utils.showToast(mApplication, mApplication.getResources().getString(R.string.msg_unable_load_subs));
                        }
                        break;
                    }
                    // not using vlc - fall through to external handling
                case External:
                    mFragment.addManualSubtitles(null);
                    mVideoManager.disableSubs();
                    mFragment.showSubLoadingMsg(true);
                    stream.setDeliveryMethod(SubtitleDeliveryMethod.External);
                    stream.setDeliveryUrl(String.format("%1$s/Videos/%2$s/%3$s/Subtitles/%4$s/0/Stream.JSON", mApplication.getApiClient().getApiUrl(), mCurrentStreamInfo.getItemId(), mCurrentStreamInfo.getMediaSourceId(), String.valueOf(stream.getIndex())));
                    mApplication.getApiClient().getSubtitles(stream.getDeliveryUrl(), new Response<SubtitleTrackInfo>() {

                        @Override
                        public void onResponse(final SubtitleTrackInfo info) {

                            if (info != null) {
                                Timber.d("Adding json subtitle track to player");
                                mFragment.addManualSubtitles(info);
                            } else {
                                Timber.e("Empty subtitle result");
                                Utils.showToast(mApplication, mApplication.getResources().getString(R.string.msg_unable_load_subs));
                                mFragment.showSubLoadingMsg(false);
                            }
                        }

                        @Override
                        public void onError(Exception ex) {
                            Timber.e(ex, "Error downloading subtitles");
                            Utils.showToast(mApplication, mApplication.getResources().getString(R.string.msg_unable_load_subs));
                            mFragment.showSubLoadingMsg(false);
                        }

                    });
                    break;
                case Hls:
                    break;
            }

        }

    }

    public void pause() {
        mPlaybackState = PlaybackState.PAUSED;
        mVideoManager.pause();
        if (mFragment != null) {
            mFragment.setFadingEnabled(false);
            mFragment.setPlayPauseActionState(ImageButton.STATE_PRIMARY);
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
        stopReportLoop();
        if (mPlaybackState != PlaybackState.IDLE && mPlaybackState != PlaybackState.UNDEFINED) {
            mPlaybackState = PlaybackState.IDLE;
            if (mVideoManager.isPlaying()) mVideoManager.stopPlayback();
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
        }
    }

    public void next() {
        Timber.d("Next called.");
        vlcErrorEncountered = false;
        exoErrorEncountered = false;
        if (mCurrentIndex < mItems.size() - 1) {
            stop();
            mCurrentIndex++;
            Timber.d("Moving to index: %d out of %d total items.", mCurrentIndex, mItems.size());
            spinnerOff = false;
            play(0);
        }
    }

    public void prev() {

    }

    public void seek(final long pos) {
        Timber.d("Seeking to %d", pos);
        Timber.d("Container: %s", mCurrentStreamInfo.getContainer());
        if (mPlaybackMethod == PlayMethod.Transcode && ContainerTypes.MKV.equals(mCurrentStreamInfo.getContainer())) {
            //mkv transcodes require re-start of stream for seek
            mVideoManager.stopPlayback();
            mApplication.getPlaybackManager().changeVideoStream(mCurrentStreamInfo, mApplication.getApiClient().getServerInfo().getId(), mCurrentOptions, pos * 10000, mApplication.getApiClient(), new Response<StreamInfo>() {
                @Override
                public void onResponse(StreamInfo response) {
                    mCurrentStreamInfo = response;
                    mVideoManager.setVideoPath(response.getMediaUrl());
                    mVideoManager.start();
                }

                @Override
                public void onError(Exception exception) {
                    Utils.showToast(mApplication.getCurrentActivity(), R.string.msg_video_playback_error);
                    Timber.e(exception, "Error trying to seek transcoded stream");
                }
            });
        } else {
            if (mVideoManager.isNativeMode() && !isLiveTv && ContainerTypes.TS.equals(mCurrentStreamInfo.getContainer())) {
                //Exo does not support seeking in .ts
                Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.seek_error));
            } else if (mVideoManager.seekTo(pos) >= 0) {
            } else {
                Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.seek_error));
            }

        }

    }

    private long currentSkipPos = 0;
    private Runnable skipRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPlaying()) return; // in case we completed since this was requested

            seek(currentSkipPos);
            currentSkipPos = 0;
            startReportLoop();
            updateProgress = true; // re-enable true progress updates
        }
    };

    public void skip(int msec) {
        if (isPlaying() && spinnerOff && mVideoManager.getCurrentPosition() > 0) { //guard against skipping before playback has truly begun
            mHandler.removeCallbacks(skipRunnable);
            stopReportLoop();
            updateProgress = false; // turn this off so we can show where it will be jumping to
            currentSkipPos = (currentSkipPos == 0 ? mVideoManager.getCurrentPosition() : currentSkipPos)  + msec;
            Timber.d("Skip amount requested was %s.  Calculated position is %s",msec, currentSkipPos);
            if (currentSkipPos < 0) currentSkipPos = 0;
            Timber.d("Duration reported as: %s current pos: %s",mVideoManager.getDuration(), mVideoManager.getCurrentPosition());
            if (currentSkipPos > mVideoManager.getDuration()) currentSkipPos = mVideoManager.getDuration() - 1000;
            mFragment.setCurrentTime(currentSkipPos);
            mHandler.postDelayed(skipRunnable, 800);
        }
    }

    public void updateTvProgramInfo() {
        // Get the current program info when playing a live TV channel
        final BaseItemDto channel = getCurrentlyPlayingItem();
        if (channel.getBaseItemType() == BaseItemType.TvChannel) {
            TvApp.getApplication().getApiClient().GetLiveTvChannelAsync(channel.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<ChannelInfoDto>() {
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
                        mFragment.updateDisplay();
                    }
                }
            });

        }
    }

    private long getRealTimeProgress() {
        return System.currentTimeMillis() - mCurrentProgramStartTime;
    }

    private long getTimeShiftedProgress() {
        return !directStreamLiveTv ? mVideoManager.getCurrentPosition() + (mCurrentTranscodeStartTime - mCurrentProgramStartTime) : getRealTimeProgress();
    }

    private void startReportLoop() {
        ReportingHelper.reportProgress(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mVideoManager.getCurrentPosition() * 10000, false);
        mReportLoop = new Runnable() {
            @Override
            public void run() {
                if (mPlaybackState == PlaybackState.PLAYING) {
                    long currentTime = isLiveTv ? getTimeShiftedProgress() : mVideoManager.getCurrentPosition();

                    ReportingHelper.reportProgress(getCurrentlyPlayingItem(), getCurrentStreamInfo(), currentTime * 10000, false);
                }
                if (mPlaybackState != PlaybackState.UNDEFINED && mPlaybackState != PlaybackState.IDLE) {
                    mHandler.postDelayed(this, PROGRESS_REPORTING_INTERVAL);
                }
            }
        };
        mHandler.postDelayed(mReportLoop, PROGRESS_REPORTING_INTERVAL);
    }

    private void startPauseReportLoop() {
        ReportingHelper.reportProgress(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mVideoManager.getCurrentPosition() * 10000, false);
        mReportLoop = new Runnable() {
            @Override
            public void run() {
                BaseItemDto currentItem = getCurrentlyPlayingItem();
                if (currentItem == null) {
                    // Loop was called while nothing was playing!
                    stopReportLoop();
                    return;
                }

                if (mPlaybackState != PlaybackState.PLAYING) {
                    // Playback was stopped, don't report progress anymore
                    return;
                }

                long currentTime = isLiveTv ? getTimeShiftedProgress() : mVideoManager.getCurrentPosition();
                if (isLiveTv && !directStreamLiveTv) {
                    mFragment.setSecondaryTime(getRealTimeProgress());
                }

                ReportingHelper.reportProgress(currentItem, getCurrentStreamInfo(), currentTime * 10000, true);
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

    private void delayedSeek(final long position) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mVideoManager.getDuration() <= 0) {
                    // wait until we have valid duration
                    mHandler.postDelayed(this, 25);
                } else {
                    // do the seek
                    if (mVideoManager.seekTo(position) < 0)
                        Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.seek_error));

                    mPlaybackState = PlaybackState.PLAYING;
                    updateProgress = true;
                }
            }
        });
    }

    public void removePreviousQueueItems() {
        TvApp.getApplication().dataRefreshService.setLastVideoQueueChange(System.currentTimeMillis());
        if (isLiveTv || !MediaManager.isVideoQueueModified()) {
            MediaManager.clearVideoQueue();
            return;
        }

        if (mCurrentIndex < 0) return;
        for (int i = 0; i < mCurrentIndex; i++) {
            mItems.remove(0);
        }

        //Now - look at last item played and, if beyond default resume point, remove it too
        Long duration = mCurrentStreamInfo != null ? mCurrentStreamInfo.getRunTimeTicks() : null;
        if (duration != null && mItems.size() > 0) {
            if (duration < 300000 || mCurrentPosition * 10000 > Math.floor(.90 * duration)) mItems.remove(0);
        } else if (duration == null) mItems.remove(0);
    }

    private void itemComplete() {
        mPlaybackState = PlaybackState.IDLE;
        stopReportLoop();
        Long mbPos = mVideoManager.getCurrentPosition() * 10000;
        ReportingHelper.reportStopped(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mbPos);
        vlcErrorEncountered = false;
        exoErrorEncountered = false;

        BaseItemDto nextItem = getNextItem();
        if (nextItem != null) {
            Timber.d("Moving to next queue item. Index: " + (mCurrentIndex + 1));

            if (mApplication.getUserPreferences().get(UserPreferences.Companion.getNextUpEnabled())) {
                // Show "Next Up" fragment
                spinnerOff = false;
                mFragment.showNextUp(nextItem.getId());
            } else {
                mCurrentIndex++;
                play(0);
            }
        } else {
            // exit activity
            Timber.d("Last item completed. Finishing activity.");
            mFragment.finish();
        }
    }

    private boolean isRestart = false;

    private void setupCallbacks() {

        mVideoManager.setOnErrorListener(new PlaybackListener() {

            @Override
            public void onEvent() {
                if (isLiveTv && directStreamLiveTv) {
                    Utils.showToast(mApplication, mApplication.getString(R.string.msg_error_live_stream));
                    directStreamLiveTv = false;
                    PlaybackHelper.retrieveAndPlay(getCurrentlyPlayingItem().getId(), false, mApplication);
                    mFragment.finish();
                } else {
                    String msg = mApplication.getString(R.string.video_error_unknown_error);
                    Timber.e("Playback error - %s", msg);
                    playerErrorEncountered();
                }

            }
        });


        mVideoManager.setOnPreparedListener(new PlaybackListener() {
            @Override
            public void onEvent() {

                if (mPlaybackState == PlaybackState.BUFFERING) {
                    mPlaybackState = PlaybackState.PLAYING;
                    mCurrentTranscodeStartTime = mCurrentStreamInfo.getPlayMethod() == PlayMethod.Transcode ? System.currentTimeMillis() : 0;
                    startReportLoop();
                }
                Timber.i("Play method: %s", mCurrentStreamInfo.getPlayMethod() == PlayMethod.Transcode ? "Trans" : "Direct");

                if (mPlaybackState == PlaybackState.PAUSED) {
                    mPlaybackState = PlaybackState.PLAYING;
                } else {
                    if (mDefaultSubIndex >= 0) {
                        //Default subs requested select them
                        Integer currentIndex = mCurrentOptions.getSubtitleStreamIndex();

                        if (currentIndex != null && currentIndex == mDefaultSubIndex) {
                            Timber.i("Not selecting default subtitle stream because it is already selected");
                        } else {
                            Timber.i("Selecting default sub stream: %d", mDefaultSubIndex);
                            switchSubtitleStream(mDefaultSubIndex);
                        }
                    } else {
                        Timber.i("Turning off subs by default");
                        mVideoManager.disableSubs();
                    }

                    if (!mVideoManager.isNativeMode() && mDefaultAudioIndex >= 0) {
                        Timber.i("Selecting default audio stream: %d", mDefaultAudioIndex);
                        switchAudioStream(mDefaultAudioIndex);
                    }
                }

            }
        });


        mVideoManager.setOnProgressListener(new PlaybackListener() {
            @Override
            public void onEvent() {
                if (isPlaying() && updateProgress) {
                    updateProgress = false;
                    boolean continueUpdate = true;
                    if (!spinnerOff) {
                        if (mStartPosition > 0) {
                            if (mPlaybackMethod == PlayMethod.Transcode) {
                                // we started the stream at seek point
                                mStartPosition = 0;
                            } else {
                                mPlaybackState = PlaybackState.SEEKING;
                                delayedSeek(mStartPosition);
                                continueUpdate = false;
                                mStartPosition = 0;

                            }
                        } else {
                            stopSpinner();
                        }
                        if (getPlaybackMethod() != PlayMethod.Transcode) {
                            if (mCurrentOptions.getAudioStreamIndex() != null)
                                mVideoManager.setAudioTrack(mCurrentOptions.getAudioStreamIndex(), getCurrentMediaSource().getMediaStreams());
                        }
                    }
                    if (continueUpdate) {
                        if (isLiveTv && mCurrentProgramEndTime > 0 && System.currentTimeMillis() >= mCurrentProgramEndTime) {
                            // crossed fire off an async routine to update the program info
                            updateTvProgramInfo();
                        }
                        final Long currentTime = isLiveTv && mCurrentProgramStartTime > 0 ? getRealTimeProgress() : mVideoManager.getCurrentPosition();
                        mFragment.setCurrentTime(currentTime);
                        //if (isLiveTv && !directStreamLiveTv) mFragment.setSecondaryTime(getRealTimeProgress());
                        mCurrentPosition = currentTime;
                        mFragment.updateSubtitles(currentTime);
                    }

                    updateProgress = continueUpdate;
                }
            }
        });

        mVideoManager.setOnCompletionListener(new PlaybackListener() {
            @Override
            public void onEvent() {
                Timber.d("On Completion fired");
                itemComplete();
            }
        });

    }

    public long getCurrentPosition() {
        return mCurrentPosition;
    }

    public boolean isPaused() {
        return mPlaybackState == PlaybackState.PAUSED;
    }

    public int getZoomMode() {
        return mVideoManager.getZoomMode();
    }

    public void setZoom(int mode) { mVideoManager.setZoom(mode); }

    public Integer translateVlcAudioId(Integer vlcId) {
        return mVideoManager.translateVlcAudioId(vlcId);
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
