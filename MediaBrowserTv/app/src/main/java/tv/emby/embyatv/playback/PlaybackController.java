package tv.emby.embyatv.playback;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.util.List;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.profiles.AndroidProfile;
import mediabrowser.model.dlna.DeviceProfile;
import mediabrowser.model.dlna.DirectPlayProfile;
import mediabrowser.model.dlna.PlaybackException;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dlna.SubtitleDeliveryMethod;
import mediabrowser.model.dlna.SubtitleStreamInfo;
import mediabrowser.model.dlna.VideoOptions;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.mediainfo.SubtitleTrackInfo;
import mediabrowser.model.session.PlayMethod;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.livetv.TvManager;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.util.MediaCodecCapabilitiesTest;
import tv.emby.embyatv.util.ProfileHelper;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 12/9/2014.
 */
public class PlaybackController {
    List<BaseItemDto> mItems;
    VideoManager mVideoManager;
    SubtitleHelper mSubHelper;
    int mCurrentIndex = 0;
    private long mCurrentPosition = 0;
    private PlaybackState mPlaybackState = PlaybackState.IDLE;
    private TvApp mApplication;

    private StreamInfo mCurrentStreamInfo;
    private List<SubtitleStreamInfo> mSubtitleStreams;

    private IPlaybackOverlayFragment mFragment;
    private View mSpinner;
    private Boolean spinnerOff = false;

    private VideoOptions mCurrentOptions;
    private int mDefaultSubIndex = -1;
    private int mDefaultAudioIndex = -1;

    private PlayMethod mPlaybackMethod = PlayMethod.Transcode;

    private Runnable mReportLoop;
    private Handler mHandler;
    private static int REPORT_INTERVAL = 3000;

    private long mNextItemThreshold = Long.MAX_VALUE;
    private boolean nextItemReported;
    private long mStartPosition = 0;
    private long mCurrentProgramEndTime;
    private long mCurrentProgramStartTime;
    private boolean isLiveTv;
    private String liveTvChannelName = "";
    private boolean useVlc = false;

    private boolean updateProgress = true;

    private Display.Mode[] mDisplayModes;
    private boolean refreshRateSwitchingEnabled;

    public PlaybackController(List<BaseItemDto> items, IPlaybackOverlayFragment fragment) {
        mItems = items;
        mFragment = fragment;
        mApplication = TvApp.getApplication();
        mHandler = new Handler();
        mSubHelper = new SubtitleHelper(TvApp.getApplication().getCurrentActivity());

        refreshRateSwitchingEnabled = Utils.is60() && mApplication.getPrefs().getBoolean("pref_refresh_switching", false);
        if (refreshRateSwitchingEnabled) getDisplayModes();

    }

    public void init(VideoManager mgr, View spinner) {
        mVideoManager = mgr;
        mSpinner = spinner;
        setupCallbacks();
    }

    public PlayMethod getPlaybackMethod() {
        return mPlaybackMethod;
    }

    public void setPlaybackMethod(PlayMethod value) {
        mPlaybackMethod = value;
    }

    public BaseItemDto getCurrentlyPlayingItem() {
        return mItems.get(mCurrentIndex);
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

    @TargetApi(23)
    private void getDisplayModes() {
        Display display = mApplication.getCurrentActivity().getWindowManager().getDefaultDisplay();
        mDisplayModes = display.getSupportedModes();
        mApplication.getLogger().Info("** Available display refresh rates:");
        for (Display.Mode mDisplayMode : mDisplayModes) {
            mApplication.getLogger().Info(Float.toString(mDisplayMode.getRefreshRate()));
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
            mApplication.getLogger().Error("Null video stream attempting to set refresh rate");
            return;
        }

        Display.Mode current = mApplication.getCurrentActivity().getWindowManager().getDefaultDisplay().getMode();
        Display.Mode best = findBestDisplayMode(videoStream.getRealFrameRate());
        if (best != null) {
            mApplication.getLogger().Info("*** Best refresh mode is: %s/%s",best.getModeId(), best.getRefreshRate());
            if (current.getModeId() != best.getModeId()) {
                mApplication.getLogger().Info("*** Attempting to change refresh rate from %s/%s",current.getModeId(), current.getRefreshRate());
                WindowManager.LayoutParams params = mApplication.getCurrentActivity().getWindow().getAttributes();
                params.preferredDisplayModeId = best.getModeId();
                mApplication.getCurrentActivity().getWindow().setAttributes(params);
            } else {
                mApplication.getLogger().Info("Display is already in best mode");
            }
        } else {
            mApplication.getLogger().Info("*** Unable to find display mode for refresh rate: %s",videoStream.getRealFrameRate());
        }


    }

    public void play(long position) {
        play(position, -1);
    }

    private void play(long position, int transcodedSubtitle) {
        if (!TvApp.getApplication().isValid()) {
            Utils.showToast(TvApp.getApplication(), "Playback not supported. Please unlock or become a supporter.");
            return;
        }

        if (TvApp.getApplication().isTrial()) {
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getRegistrationString()+". Unlock or become a supporter for unlimited playback.");

        }

        mApplication.getLogger().Debug("Play called with pos: " + position + " and sub index: "+transcodedSubtitle);
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
                    mFragment.updateEndTime(mVideoManager.getDuration() - getCurrentPosition());
                }
                startReportLoop();
                break;
            case BUFFERING:
                // onPrepared should take care of it
                break;
            case IDLE:
                // start new playback
                BaseItemDto item = getCurrentlyPlayingItem();
                lastProgressPosition = 0;

                // make sure item isn't missing
                if (item.getLocationType() == LocationType.Virtual) {
                    if (hasNextItem()) {
                        new AlertDialog.Builder(mApplication.getCurrentActivity())
                                .setTitle("Episode Missing")
                                .setMessage("This episode is missing from your library.  Would you like to skip it and continue to the next one?")
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
                                .setTitle("Episode Missing")
                                .setMessage("This episode is missing from your library.  Playback will stop.")
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

                isLiveTv = item.getType().equals("TvChannel");
                startSpinner();

                //Build options for each player
                VideoOptions vlcOptions = new VideoOptions();
                vlcOptions.setDeviceId(mApplication.getApiClient().getDeviceId());
                vlcOptions.setItemId(item.getId());
                vlcOptions.setMediaSources(item.getMediaSources());
                vlcOptions.setMaxBitrate(getMaxBitrate());
                vlcOptions.setSubtitleStreamIndex(transcodedSubtitle >= 0 ? transcodedSubtitle : null);
                vlcOptions.setMediaSourceId(transcodedSubtitle >= 0 ? getCurrentMediaSource().getId() : null);
                DeviceProfile vlcProfile = ProfileHelper.getBaseProfile();
                ProfileHelper.setVlcOptions(vlcProfile, isLiveTv);
                vlcOptions.setProfile(vlcProfile);

                VideoOptions internalOptions = new VideoOptions();
                internalOptions.setDeviceId(mApplication.getApiClient().getDeviceId());
                internalOptions.setItemId(item.getId());
                internalOptions.setMediaSources(item.getMediaSources());
                internalOptions.setMaxBitrate(getMaxBitrate());
                internalOptions.setMaxAudioChannels(Utils.downMixAudio() ? 2 : null); //have to downmix at server
                internalOptions.setSubtitleStreamIndex(transcodedSubtitle >= 0 ? transcodedSubtitle : null);
                internalOptions.setMediaSourceId(transcodedSubtitle >= 0 ? getCurrentMediaSource().getId() : null);
                DeviceProfile internalProfile = ProfileHelper.getBaseProfile();
                if (Utils.is60() || mApplication.getPrefs().getBoolean("pref_bitstream_ac3", true)) {
                    ProfileHelper.setExoOptions(internalProfile, isLiveTv, true);
                    ProfileHelper.addAc3Streaming(internalProfile, true);
                    mApplication.getLogger().Info("*** Using extended Exoplayer profile options");

                } else {
                    mApplication.getLogger().Info("*** Using default android profile");
                }
                internalOptions.setProfile(internalProfile);

                mDefaultSubIndex = transcodedSubtitle;
                TvApp.getApplication().getLogger().Debug("Max bitrate is: " + getMaxBitrate());

                playInternal(getCurrentlyPlayingItem(), position, vlcOptions, internalOptions);
                mPlaybackState = PlaybackState.BUFFERING;
                if (mFragment != null) {
                    mFragment.setPlayPauseActionState(ImageButton.STATE_SECONDARY);
                    mFragment.setFadingEnabled(true);
                    mFragment.setCurrentTime(position);
                }

                long duration = getCurrentlyPlayingItem().getRunTimeTicks()!= null ? getCurrentlyPlayingItem().getRunTimeTicks() / 10000 : -1;
                mVideoManager.setMetaDuration(duration);

                if (hasNextItem()) {
                    // Determine the "next up" threshold
                    if (duration > 600000) {
                        //only items longer than 10min to have this feature
                        nextItemReported = false;
                        if (duration > 4500000) {
                            //longer than 1hr 15 it probably has pretty long credits
                            mNextItemThreshold = duration - 180000; // 3 min
                        } else {
                            //std 30 min episode or less
                            mNextItemThreshold = duration - 50000; // 50 seconds
                        }
                        TvApp.getApplication().getLogger().Debug("Next item threshold set to "+ mNextItemThreshold);
                    } else {
                        mNextItemThreshold = Long.MAX_VALUE;
                    }
                } else {
                    mNextItemThreshold = Long.MAX_VALUE;
                }

                break;
        }
    }

    public int getMaxBitrate() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mApplication);
        String maxRate = sharedPref.getString("pref_max_bitrate", "0");
        Float factor = Float.parseFloat(maxRate) * 10;
        return factor == 0 ? TvApp.getApplication().getAutoBitrate() : (factor.intValue() * 100000);
    }

    public int getBufferAmount() {
        return 600;
    }

    private void playInternal(final BaseItemDto item, final long position, final VideoOptions vlcOptions, final VideoOptions internalOptions) {
        final ApiClient apiClient = mApplication.getApiClient();
        mApplication.setCurrentPlayingItem(item);
        if (isLiveTv) {
            liveTvChannelName = " ("+item.getName()+")";
            updateTvProgramInfo();
            TvManager.setLastLiveTvChannel(item.getId());
        }

        // Get playback info for each player and then decide on which one to use
        mApplication.getPlaybackManager().getVideoStreamInfo(apiClient.getServerInfo().getId(), vlcOptions, false, apiClient, new Response<StreamInfo>() {
            @Override
            public void onResponse(final StreamInfo vlcResponse) {
                mApplication.getLogger().Info("VLC would " + (vlcResponse.getPlayMethod().equals(PlayMethod.Transcode) ? "transcode" : "direct stream"));
                mApplication.getPlaybackManager().getVideoStreamInfo(apiClient.getServerInfo().getId(), internalOptions, false, apiClient, new Response<StreamInfo>() {
                    @Override
                    public void onResponse(StreamInfo internalResponse) {
                        mApplication.getLogger().Info("Internal player would " + (internalResponse.getPlayMethod().equals(PlayMethod.Transcode) ? "transcode" : "direct stream"));
                        boolean isInterlaced = vlcResponse.getMediaSource().getVideoStream().getIsInterlaced();
                        mApplication.getLogger().Info(isInterlaced ? "Stream is interlaced" : "Stream is not interlaced");

                        // Now look at both responses and choose the one that direct plays or bitstreams - favor VLC
                        useVlc = !vlcResponse.getPlayMethod().equals(PlayMethod.Transcode)
                                && (Utils.is60() || !mApplication.getPrefs().getBoolean("pref_bitstream_ac3", false) || !"ac3".equals(vlcResponse.getMediaSource().getDefaultAudioStream().getCodec()))
                                && (Utils.downMixAudio() || !Utils.is60() || internalResponse.getPlayMethod().equals(PlayMethod.Transcode) || !mApplication.getPrefs().getBoolean("pref_bitstream_dts", false) || internalResponse.getMediaSource() == null || internalResponse.getMediaSource().getDefaultAudioStream() == null || (!internalResponse.getMediaSource().getDefaultAudioStream().getCodec().equals("dca") && !internalResponse.getMediaSource().getDefaultAudioStream().getCodec().equals("DCA")))
                                && (!isLiveTv || (mApplication.directStreamLiveTv() && mApplication.useVlcForLiveTv()))
                                && (!Utils.isFireTvStick() || (vlcResponse.getMediaSource().getVideoStream() != null && vlcResponse.getMediaSource().getVideoStream().getWidth() < 1000));

                        mApplication.getLogger().Info(useVlc ? "Preferring VLC" : "Will use internal player");
                        mVideoManager.init(getBufferAmount(), isInterlaced);
                        if (!useVlc && (internalOptions.getAudioStreamIndex() != null && !internalOptions.getAudioStreamIndex().equals(bestGuessAudioTrack(internalResponse.getMediaSource())))) {
                            // requested specific audio stream that is different from default so we need to force a transcode to get it (ExoMedia currently cannot switch)
                            // remove direct play profiles to force the transcode
                            final DeviceProfile save = internalOptions.getProfile();
                            DeviceProfile newProfile = ProfileHelper.getBaseProfile();
                            ProfileHelper.setExoOptions(newProfile, isLiveTv, true);
                            if (!Utils.downMixAudio()) ProfileHelper.addAc3Streaming(newProfile, true);
                            newProfile.setDirectPlayProfiles(new DirectPlayProfile[]{});
                            internalOptions.setProfile(newProfile);
                            mApplication.getLogger().Info("Forcing transcode due to non-default audio chosen");
                            mApplication.getPlaybackManager().getVideoStreamInfo(apiClient.getServerInfo().getId(), internalOptions, false, apiClient, new Response<StreamInfo>() {
                                @Override
                                public void onResponse(StreamInfo response) {
                                    //re-set this
                                    internalOptions.setProfile(save);
                                    mCurrentOptions = internalOptions;
                                    startItem(item, position, apiClient, response);
                                }
                            });
                        } else {
                            mCurrentOptions = useVlc ? vlcOptions : internalOptions;
                            startItem(item, position, apiClient, useVlc ? vlcResponse : internalResponse);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        mApplication.getLogger().ErrorException("Unable to get internal stream info", exception);
                        useVlc = true;
                        mCurrentOptions = vlcOptions;
                        startItem(item, position, apiClient, vlcResponse);
                    }
                });

            }

            @Override
            public void onError(Exception exception) {
                mApplication.getLogger().ErrorException("Error getting playback stream info", exception);
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
        });

    }

    private void startItem(BaseItemDto item, long position, ApiClient apiClient, StreamInfo response) {
        mCurrentStreamInfo = response;
        Long mbPos = position * 10000;

        setPlaybackMethod(response.getPlayMethod());

        if (useVlc && !getPlaybackMethod().equals(PlayMethod.Transcode)) {
            TvApp.getApplication().getLogger().Info("Playing back in VLC.");
            mVideoManager.setNativeMode(false);
        } else {
            mVideoManager.setNativeMode(true);
            TvApp.getApplication().getLogger().Info("Playing back in native mode.");
            if (Utils.downMixAudio()) {
                TvApp.getApplication().getLogger().Info("Setting max audio to 2-channels");
                mCurrentStreamInfo.setMaxAudioChannels(2);
            }

        }

        // set refresh rate
        if (refreshRateSwitchingEnabled) {
            setRefreshRate(response.getMediaSource().getVideoStream());
        }

        // get subtitle info
        mSubtitleStreams = response.GetSubtitleProfiles(false, mApplication.getApiClient().getApiUrl(), mApplication.getApiClient().getAccessToken());

        // set start point if transcoding to mkv
        if (mPlaybackMethod == PlayMethod.Transcode && response.getContainer().equals("mkv")) {
            response.setStartPositionTicks(position * 10000);
        }

        mFragment.updateDisplay();
        String path = response.ToUrl(apiClient.getApiUrl(), apiClient.getAccessToken());

        // if source is stereo or we're not on at least 6.0 with AC3 - use most compatible output
        if (!mVideoManager.isNativeMode() && (isLiveTv && !Utils.is60()) || (response.getMediaSource() != null && response.getMediaSource().getDefaultAudioStream() != null && response.getMediaSource().getDefaultAudioStream().getChannels() != null && (response.getMediaSource().getDefaultAudioStream().getChannels() <= 2
                || (!Utils.is60() && "ac3".equals(response.getMediaSource().getDefaultAudioStream().getCodec()))))) {
            mVideoManager.setCompatibleAudio();
            mApplication.getLogger().Info("Setting compatible audio mode...");
            //Utils.showToast(mApplication, "Compatible");
        } else {
            //Utils.showToast(mApplication, "Default");
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
        if (!isRestart) Utils.ReportStart(item, mbPos);
        isRestart = false;

    }

    private int getDefaultAudioIndex(StreamInfo info) {
        return mPlaybackMethod != PlayMethod.Transcode && info.getMediaSource().getDefaultAudioStreamIndex() != null ? info.getMediaSource().getDefaultAudioStreamIndex() : -1;
    }

    public void startSpinner() {
        if (mApplication.getCurrentActivity() != null) {
            mApplication.getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mSpinner != null) mSpinner.setVisibility(View.VISIBLE);
                    spinnerOff = false;
                }
            });

        }

    }

    public void stopSpinner() {
        if (mApplication.getCurrentActivity() != null) {
            mApplication.getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    spinnerOff = true;
                    if (mSpinner != null) mSpinner.setVisibility(View.GONE);
                }
            });

        }

    }

    public void switchAudioStream(int index) {
        if (!isPlaying()) return;

        mCurrentOptions.setAudioStreamIndex(index);
        if (mVideoManager.isNativeMode()) {
            startSpinner();
            mApplication.getLogger().Debug("Setting audio index to: " + index);
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
        mApplication.getLogger().Debug("Setting subtitle index to: " + index);
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

        MediaStream stream = Utils.GetMediaStream(getCurrentMediaSource(), index);
        if (stream == null) {
            Utils.showToast(mApplication, "Unable to select subtitle");
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
                    stream.setDeliveryUrl(String.format("%1$s/Videos/%2$s/%3$s/Subtitles/%4$s/0/Stream.JSON", mApplication.getApiClient().getApiUrl(), mCurrentStreamInfo.getItemId(), mCurrentStreamInfo.getMediaSourceId(), StringHelper.ToStringCultureInvariant(stream.getIndex())));
                    mApplication.getApiClient().getSubtitles(stream.getDeliveryUrl(), new Response<SubtitleTrackInfo>() {

                        @Override
                        public void onResponse(final SubtitleTrackInfo info) {

                            if (info != null) {
                                TvApp.getApplication().getLogger().Debug("Adding json subtitle track to player");
                                mFragment.addManualSubtitles(info);
                            } else {
                                TvApp.getApplication().getLogger().Error("Empty subtitle result");
                                Utils.showToast(mApplication, mApplication.getResources().getString(R.string.msg_unable_load_subs));
                                mFragment.showSubLoadingMsg(false);
                            }
                        }

                        @Override
                        public void onError(Exception ex) {
                            TvApp.getApplication().getLogger().ErrorException("Error downloading subtitles", ex);
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
                e.printStackTrace();
            }
            Long mbPos = mCurrentPosition * 10000;
            Utils.ReportStopped(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mbPos);
            if (!isLiveTv) {
                // update the actual items resume point
                getCurrentlyPlayingItem().getUserData().setPlaybackPositionTicks(mbPos);
            }

            // be sure to unmute audio in case it was muted
            TvApp.getApplication().setAudioMuted(false);

        }
    }

    public void next() {
        mApplication.getLogger().Debug("Next called.");
        if (mCurrentIndex < mItems.size() - 1) {
            stop();
            mCurrentIndex++;
            mApplication.getLogger().Debug("Moving to index: " + mCurrentIndex + " out of " + mItems.size() + " total items.");
            spinnerOff = false;
            play(0);
        }
    }

    public void prev() {

    }

    public void seek(final long pos) {
        mApplication.getLogger().Debug("Seeking to " + pos);
        mApplication.getLogger().Debug("Container: "+mCurrentStreamInfo.getContainer());
        if (mPlaybackMethod == PlayMethod.Transcode) {
            //mkv transcodes require re-start of stream for seek
            mVideoManager.stopPlayback();
            mCurrentStreamInfo.setStartPositionTicks(pos * 10000);

            mVideoManager.setVideoPath(mCurrentStreamInfo.ToUrl(mApplication.getApiClient().getApiUrl(), mApplication.getApiClient().getAccessToken()));
            mVideoManager.start();
        } else {
            if (mVideoManager.isNativeMode() && "ts".equals(mCurrentStreamInfo.getContainer())) {
                //Exo does not support seeking in .ts
                Utils.showToast(TvApp.getApplication(), "Unable to seek");
            } else if (mVideoManager.seekTo(pos) >= 0) {
                if (mFragment != null) {
                    mFragment.updateEndTime(mVideoManager.getDuration() - pos);
                }
            } else {
                Utils.showToast(TvApp.getApplication(), "Unable to seek");
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

    private float playSpeed = 1;
    public void togglePlaySpeed() {
        if (playSpeed < 4) playSpeed += 1f;
        else playSpeed = 1;
        mVideoManager.setPlaySpeed(playSpeed);
    }

    public void skip(int msec) {
        if (isPlaying()) {
            mHandler.removeCallbacks(skipRunnable);
            stopReportLoop();
            updateProgress = false; // turn this off so we can show where it will be jumping to
            currentSkipPos = (currentSkipPos == 0 ? mVideoManager.getCurrentPosition() : currentSkipPos)  + msec;
            if (currentSkipPos < 0) currentSkipPos = 0;
            mApplication.getLogger().Debug("Duration reported as: "+mVideoManager.getDuration());
            if (currentSkipPos > mVideoManager.getDuration()) currentSkipPos = mVideoManager.getDuration() - 1000;
            mFragment.setCurrentTime(currentSkipPos);
            mHandler.postDelayed(skipRunnable, 800);
        }
    }

    private void updateTvProgramInfo() {
        // Get the current program info when playing a live TV channel
        final BaseItemDto channel = getCurrentlyPlayingItem();
        if (channel.getType().equals("TvChannel")) {
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
                        mCurrentProgramEndTime = channel.getEndDate() != null ? Utils.convertToLocalDate(channel.getEndDate()).getTime() : 0;
                        mCurrentProgramStartTime = channel.getPremiereDate() != null ? Utils.convertToLocalDate(channel.getPremiereDate()).getTime() : 0;
                        mFragment.updateDisplay();
                    }
                }
            });

        }
    }

    private long getRealTimeProgress() {
        return System.currentTimeMillis() - mCurrentProgramStartTime;
    }

    private void startReportLoop() {
        Utils.ReportProgress(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mVideoManager.getCurrentPosition() * 10000, false);
        mReportLoop = new Runnable() {
            @Override
            public void run() {
                if (mPlaybackState == PlaybackState.PLAYING) {
                    long currentTime = mVideoManager.getCurrentPosition();

                    Utils.ReportProgress(getCurrentlyPlayingItem(), getCurrentStreamInfo(), currentTime * 10000, false);

                    //Do this next up processing here because every 3 seconds is good enough
                    if (!nextItemReported && hasNextItem() && currentTime >= mNextItemThreshold){
                        nextItemReported = true;
                        mFragment.nextItemThresholdHit(getNextItem());
                    }
                }
                mApplication.setLastUserInteraction(System.currentTimeMillis());
                if (mPlaybackState != PlaybackState.UNDEFINED && mPlaybackState != PlaybackState.IDLE) mHandler.postDelayed(this, REPORT_INTERVAL);
            }
        };
        mHandler.postDelayed(mReportLoop, REPORT_INTERVAL);
    }

    private void startPauseReportLoop() {
        Utils.ReportProgress(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mVideoManager.getCurrentPosition() * 10000, false);
        mReportLoop = new Runnable() {
            @Override
            public void run() {

                long currentTime = mVideoManager.getCurrentPosition();

                Utils.ReportProgress(getCurrentlyPlayingItem(), getCurrentStreamInfo(), currentTime * 10000, true);
                mHandler.postDelayed(this, 15000);
            }
        };
        mHandler.postDelayed(mReportLoop, 15000);
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
                        Utils.showToast(TvApp.getApplication(), "Unable to seek");

                    mPlaybackState = PlaybackState.PLAYING;
                    updateProgress = true;
                    mFragment.updateEndTime(mVideoManager.getDuration() - position);
                }
            }
        });
    }

    public void removePreviousQueueItems() {
        TvApp.getApplication().setLastVideoQueueChange(System.currentTimeMillis());
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
        Utils.ReportStopped(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mbPos);
        if (mCurrentIndex < mItems.size() - 1) {
            // move to next in queue
            mCurrentIndex++;
            mApplication.getLogger().Debug("Moving to next queue item. Index: "+mCurrentIndex);
            spinnerOff = false;
            play(0);
        } else {
            // exit activity
            mApplication.getLogger().Debug("Last item completed. Finishing activity.");
            mFragment.finish();
        }
    }

    private long lastProgressPosition;
    private boolean isRestart = false;

    private void setupCallbacks() {

        mVideoManager.setOnErrorListener(new PlaybackListener() {

            @Override
            public void onEvent() {
                if (isLiveTv && mApplication.directStreamLiveTv()) {
                    Utils.showToast(mApplication, mApplication.getString(R.string.msg_error_live_stream));
                    mApplication.setDirectStreamLiveTv(false);
                    Utils.retrieveAndPlay(getCurrentlyPlayingItem().getId(), false, mApplication);
                    mFragment.finish();

                } else {
                    String msg = mApplication.getString(R.string.video_error_unknown_error);
                    Utils.showToast(mApplication, mApplication.getString(R.string.msg_video_playback_error) + msg);
                    mApplication.getLogger().Error("Playback error - " + msg);
                    mPlaybackState = PlaybackState.ERROR;
                    stop();
                }

            }
        });


        mVideoManager.setOnPreparedListener(new PlaybackListener() {
            @Override
            public void onEvent() {

                if (mPlaybackState == PlaybackState.BUFFERING) {
                    mPlaybackState = PlaybackState.PLAYING;
                    mFragment.updateEndTime(isLiveTv && getCurrentlyPlayingItem().getEndDate() != null ? Utils.convertToLocalDate(getCurrentlyPlayingItem().getEndDate()).getTime() - System.currentTimeMillis() : mVideoManager.getDuration() - mStartPosition);
                    startReportLoop();
                }
                TvApp.getApplication().getLogger().Info("Play method: ", mCurrentStreamInfo.getPlayMethod() == PlayMethod.Transcode ? "Trans" : "Direct");

                if (mPlaybackState == PlaybackState.PAUSED) {
                    mPlaybackState = PlaybackState.PLAYING;
                } else {
                    if (mDefaultSubIndex >= 0) {
                        //Default subs requested select them
                        mApplication.getLogger().Info("Selecting default sub stream: " + mDefaultSubIndex);
                        switchSubtitleStream(mDefaultSubIndex);
                    } else {
                        TvApp.getApplication().getLogger().Info("Turning off subs by default");
                        mVideoManager.disableSubs();
                    }

                    if (!mVideoManager.isNativeMode() && mDefaultAudioIndex >= 0) {
                        TvApp.getApplication().getLogger().Info("Selecting default audio stream: " + mDefaultAudioIndex);
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
                    if (isLiveTv && mVideoManager.isNativeMode() && lastProgressPosition > 0 && lastProgressPosition == mVideoManager.getCurrentPosition()) {
                        mApplication.getLogger().Debug("************** playback appears to have stalled - attempting re-start");
                        mVideoManager.stopPlayback();
                        mPlaybackState = PlaybackState.IDLE;
                        isRestart = true;
                        play(0);
                    } else {
                        lastProgressPosition = mVideoManager.getCurrentPosition();
                        //mApplication.getLogger().Debug("******* progress listener fired");
                    }
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
                        mApplication.setLastUserInteraction(System.currentTimeMillis()); // don't want to auto logoff during playback
                        if (isLiveTv && mCurrentProgramEndTime > 0 && System.currentTimeMillis() >= mCurrentProgramEndTime) {
                            // crossed fire off an async routine to update the program info
                            updateTvProgramInfo();
                        }
                        final Long currentTime = isLiveTv && mCurrentProgramStartTime > 0 ? getRealTimeProgress() : mVideoManager.getCurrentPosition();
                        mFragment.setCurrentTime(currentTime);
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
                TvApp.getApplication().getLogger().Debug("On Completion fired");
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

    public boolean isIdle() {
        return mPlaybackState == PlaybackState.IDLE;
    }

    public int getZoomMode() {
        return mVideoManager.getZoomMode();
    }

    public void setZoom(int mode) { mVideoManager.setZoom(mode); }

    public Integer translateVlcAudioId(Integer vlcId) {
        return mVideoManager.translateVlcAudioId(vlcId);
    }


    /*
 * List of various states that we can be in
 */
    public enum PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE, SEEKING, UNDEFINED, ERROR;
    }

}
