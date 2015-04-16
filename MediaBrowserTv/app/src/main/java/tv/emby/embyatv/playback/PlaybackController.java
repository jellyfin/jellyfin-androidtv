package tv.emby.embyatv.playback;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.VideoView;

import java.util.Date;
import java.util.List;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.profiles.AndroidProfile;
import mediabrowser.model.dlna.PlaybackException;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dlna.SubtitleProfile;
import mediabrowser.model.dlna.VideoOptions;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.session.PlayMethod;
import mediabrowser.model.session.PlaybackStartInfo;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 12/9/2014.
 */
public class PlaybackController {
    List<BaseItemDto> mItems;
    VideoView mVideoView;
    int mCurrentIndex = 0;
    private int mCurrentPosition = 0;
    private PlaybackState mPlaybackState = PlaybackState.IDLE;
    private TvApp mApplication;

    private StreamInfo mCurrentStreamInfo;

    private PlaybackOverlayFragment mFragment;
    private View mSpinner;
    private Boolean spinnerOff = false;

    private VideoOptions mCurrentOptions;

    private PlayMethod mPlaybackMethod = PlayMethod.Transcode;

    private Runnable mReportLoop;
    private Runnable mProgressLoop;
    private Handler mHandler;
    private static int REPORT_INTERVAL = 3000;
    private static final int DEFAULT_UPDATE_PERIOD = 1000;
    private static final int UPDATE_PERIOD = 500;

    private int mFreezeCheckPoint = Integer.MAX_VALUE;
    private int mLastReportedTime;
    private boolean mayBeFrozen = false;
    private int mPositionOffset = 0;
    private int mStartPosition = 0;
    private long mCurrentProgramEndTime;
    private long mCurrentProgramStartTime;
    private boolean isLiveTv;
    private String liveTvChannelName = "";

    public PlaybackController(List<BaseItemDto> items, PlaybackOverlayFragment fragment) {
        mItems = items;
        mFragment = fragment;
        mApplication = TvApp.getApplication();
        mHandler = new Handler();

    }

    public void init(VideoView view, View spinner) {
        mVideoView = view;
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
    public boolean canSeek() {return getCurrentlyPlayingItem() != null && !"TvChannel".equals(getCurrentlyPlayingItem().getType());}

    public boolean isPlaying() {
        return mPlaybackState == PlaybackState.PLAYING;
    }

    public void play(int position) {
        if (!TvApp.getApplication().isValid()) {
            Utils.showToast(TvApp.getApplication(), "Playback not supported. Please unlock or become a supporter.");
            return;
        }

        if (TvApp.getApplication().isTrial()) {
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getRegistrationString()+". Unlock or become a supporter for unlimited playback.");

        }

        mayBeFrozen = false;
        mApplication.getLogger().Debug("Play called with pos: "+position);
        switch (mPlaybackState) {
            case PLAYING:
                // do nothing
                break;
            case PAUSED:
                // just resume
                mVideoView.start();
                mPlaybackState = PlaybackState.PLAYING;
                startProgressAutomation();
                if (mFragment != null) mFragment.setFadingEnabled(true);
                startReportLoop();
                break;
            case BUFFERING:
                // onPrepared should take care of it
                break;
            case IDLE:
                // start new playback
                BaseItemDto item = getCurrentlyPlayingItem();
                // confirm we actually can play
                if (item.getPlayAccess() != PlayAccess.Full) {
                    String msg = item.getIsPlaceHolder() ? mApplication.getString(R.string.msg_cannot_play) : mApplication.getString(R.string.msg_cannot_play_time);
                    Utils.showToast(TvApp.getApplication(), msg);
                    return;
                }

                startSpinner();
                mCurrentOptions = new VideoOptions();
                mCurrentOptions.setDeviceId(mApplication.getApiClient().getDeviceId());
                mCurrentOptions.setItemId(item.getId());
                mCurrentOptions.setMediaSources(item.getMediaSources());
                mCurrentOptions.setMaxBitrate(getMaxBitrate());

                // Create our profile and clear out subtitles so that they will burn in
                AndroidProfile profile = new AndroidProfile(PreferenceManager.getDefaultSharedPreferences(mApplication).getBoolean("pref_enable_hls",true), true);
                profile.setSubtitleProfiles(new SubtitleProfile[] {});
                mCurrentOptions.setProfile(profile);

                playInternal(getCurrentlyPlayingItem(), position, mVideoView, mCurrentOptions);
                if (mFragment != null) {
                    mFragment.setFadingEnabled(true);
                    mFragment.getPlaybackControlsRow().setCurrentTime(position);
                }
                mPlaybackState = PlaybackState.BUFFERING;
                break;
        }
    }

    public int getMaxBitrate() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mApplication);
        String maxRate = sharedPref.getString("pref_max_bitrate", "15");
        return Integer.parseInt(maxRate) * 1000000;
    }

    private int getCurrentOffset(Date start) {
        Long millis = System.currentTimeMillis() - start.getTime();
        return millis.intValue();
    }

    private void playInternal(final BaseItemDto item, final int position, final VideoView view, VideoOptions options) {
        final ApiClient apiClient = mApplication.getApiClient();
        mPositionOffset = 0;
        mApplication.setCurrentPlayingItem(item);
        isLiveTv = item.getType().equals("TvChannel");
        if (isLiveTv) {
            liveTvChannelName = " ("+item.getName()+")";
            updateTvProgramInfo();
        }

        mApplication.getPlaybackManager().getVideoStreamInfo(apiClient.getServerInfo().getId(), options, false, apiClient, new Response<StreamInfo>() {
            @Override
            public void onResponse(StreamInfo response) {
                mCurrentStreamInfo = response;
                Long mbPos = (long)position * 10000;
                if (!PreferenceManager.getDefaultSharedPreferences(mApplication).getBoolean("pref_enable_hls",true)) {
                    response.setStartPositionTicks(mbPos);
                    mPositionOffset = position;
                }

                String path = response.ToUrl(apiClient.getApiUrl(), apiClient.getAccessToken());
                view.setVideoPath(path);
                setPlaybackMethod(response.getPlayMethod());
                view.start();
                mStartPosition = position;

                PlaybackStartInfo startInfo = new PlaybackStartInfo();
                startInfo.setItemId(item.getId());
                startInfo.setPositionTicks(mbPos);
                TvApp.getApplication().getPlaybackManager().reportPlaybackStart(startInfo, false, apiClient, new EmptyResponse());
                TvApp.getApplication().getLogger().Info("Playback of "+item.getName()+"("+path+") started.");
            }

            @Override
            public void onError(Exception exception) {
                if (exception instanceof PlaybackException) {
                    PlaybackException ex = (PlaybackException) exception;
                    switch (ex.getErrorCode()){
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

    private void switchStreamInternal(final StreamInfo current, final int position, final VideoView view) {

        TvApp.getApplication().getPlaybackManager().changeVideoStream(
                current,
                TvApp.getApplication().getApiClient().getServerInfo().getId(),
                mCurrentOptions,
                TvApp.getApplication().getApiClient(),
                new Response<StreamInfo>() {
                    @Override
                    public void onResponse(StreamInfo response) {
                        mCurrentStreamInfo = response;
                        Long mbPos = (long)position * 10000;
                        if (!PreferenceManager.getDefaultSharedPreferences(mApplication).getBoolean("pref_enable_hls",true)) {
                            response.setStartPositionTicks(mbPos);
                            mPositionOffset = position;
                        }

                        String path = response.ToUrl(TvApp.getApplication().getApiClient().getApiUrl(), TvApp.getApplication().getApiClient().getAccessToken());
                        view.setVideoPath(path);
                        setPlaybackMethod(response.getPlayMethod());
                        if (position > 0) {
                            mApplication.getPlaybackController().seek(position);
                        }
                        view.start();

                        PlaybackStartInfo startInfo = new PlaybackStartInfo();

                        startInfo.setItemId(current.getItemId());
                        startInfo.setPositionTicks(mbPos);
                        TvApp.getApplication().getPlaybackManager().reportPlaybackStart(startInfo, false, TvApp.getApplication().getApiClient(), new EmptyResponse());
                        TvApp.getApplication().getLogger().Info("Playback of "+getCurrentlyPlayingItem().getName()+"("+path+") re-started.");
                    }
                }
        );

    }

    public void startSpinner() {
        if (mSpinner != null) mSpinner.setVisibility(View.VISIBLE);
        spinnerOff = false;

    }

    public void stopSpinner() {
        spinnerOff = true;
        if (mSpinner != null) mSpinner.setVisibility(View.GONE);

    }

    public void setPlayPauseIndicatorState(int state) {
        mFragment.setPlayPauseActionState(state);

    }
    public void switchAudioStream(int index) {
        if (!isPlaying()) return;

        startSpinner();
        mCurrentOptions.setAudioStreamIndex(index);
        mApplication.getLogger().Debug("Setting audio index to: " + index);
        mCurrentOptions.setMediaSourceId(getCurrentMediaSource().getId());
        stop();
        switchStreamInternal(mCurrentStreamInfo, mCurrentPosition, mVideoView);
    }

    public void switchSubtitleStream(int index) {
        if (!isPlaying()) return;

        startSpinner();
        mCurrentOptions.setSubtitleStreamIndex(index >= 0 ? index : null);
        mApplication.getLogger().Debug("Setting subtitle index to: " + index);
        mCurrentOptions.setMediaSourceId(getCurrentMediaSource().getId());
        stop();
        //playInternal(getCurrentlyPlayingItem(), mCurrentPosition, mVideoView, mCurrentOptions);
        switchStreamInternal(mCurrentStreamInfo, mCurrentPosition, mVideoView);
    }

    public void pause() {
        mPlaybackState = PlaybackState.PAUSED;
        stopProgressAutomation();
        mVideoView.pause();
        if (mFragment != null) {
            mFragment.setFadingEnabled(false);
        }
        stopReportLoop();

    }

    public void stop() {
        if (mPlaybackState != PlaybackState.IDLE && mPlaybackState != PlaybackState.UNDEFINED) {
            mPlaybackState = PlaybackState.IDLE;
            stopReportLoop();
            stopProgressAutomation();
            if (mVideoView.isPlaying()) mVideoView.stopPlayback();
            //give it a just a beat to actually stop - this keeps it from re-requesting the stream after we tell the server we've stopped
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Long mbPos = (long)mCurrentPosition * 10000;
            Utils.ReportStopped(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mbPos);
        }
    }

    public void next() {
        stop();
        mApplication.getLogger().Debug("Next called.");
        if (mCurrentIndex < mItems.size() - 1) {
            mCurrentIndex++;
            mApplication.getLogger().Debug("Moving to index: "+mCurrentIndex+" out of "+mItems.size() + " total items.");
            mFragment.removeQueueItem(0);
            mFragment.updatePlaybackControls();
            spinnerOff = false;
            play(0);
        }
    }

    public void prev() {

    }

    public void seek(int pos) {
        stopReportLoop();
        stopProgressAutomation();
        mPlaybackState = PlaybackState.SEEKING;
        mApplication.getLogger().Debug("Seeking to "+pos);
        startSpinner();
        mVideoView.seekTo(pos);

    }

    public void skip(int msec) {
        seek(mVideoView.getCurrentPosition() + msec);
    }

    private int getUpdatePeriod() {
        if (mPlaybackState != PlaybackState.PLAYING) {
            return DEFAULT_UPDATE_PERIOD;
        }
        return UPDATE_PERIOD;
    }

    private void updateTvProgramInfo() {
        // Get the current program info when playing a live TV channel
        final BaseItemDto channel = getCurrentlyPlayingItem();
        if (channel.getType().equals("TvChannel")) {
            TvApp.getApplication().getApiClient().GetLiveTvChannelAsync(channel.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<ChannelInfoDto>() {
                @Override
                public void onResponse(ChannelInfoDto response) {
                    ProgramInfoDto program = response.getCurrentProgram();
                    if (program != null) {
                        channel.setName(program.getName() + liveTvChannelName);
                        channel.setPremiereDate(program.getStartDate());
                        channel.setEndDate(program.getEndDate());
                        channel.setOfficialRating(program.getOfficialRating());
                        channel.setRunTimeTicks(program.getRunTimeTicks());
                        mCurrentProgramEndTime = channel.getEndDate() != null ? Utils.convertToLocalDate(channel.getEndDate()).getTime() : 0;
                        mCurrentProgramStartTime = channel.getPremiereDate() != null ? Utils.convertToLocalDate(channel.getPremiereDate()).getTime() : 0;
                        mFragment.updatePlaybackControls();
                    }
                }
            });

        }
    }

    private int getRealTimeProgress() {
        Long time = System.currentTimeMillis() - mCurrentProgramStartTime;
        return time.intValue();
    }

    private void startProgressAutomation() {
        mProgressLoop = new Runnable() {
            @Override
            public void run() {
                int updatePeriod = getUpdatePeriod();
                PlaybackControlsRow controls = mFragment.getPlaybackControlsRow();
                if (isPlaying()) {
                    if (!spinnerOff) {
                        stopSpinner();
                    }
                    if (isLiveTv && mCurrentProgramEndTime > 0 && System.currentTimeMillis() >= mCurrentProgramEndTime) {
                        // crossed fire off an async routine to update the program info
                        updateTvProgramInfo();
                    }
                    final int currentTime = isLiveTv && mCurrentProgramStartTime > 0 ? getRealTimeProgress() : mVideoView.getCurrentPosition() + mPositionOffset;
                    controls.setCurrentTime(currentTime);
                    mCurrentPosition = currentTime;
                    //The very end of some videos over hls cause the VideoView to freeze which freezes our whole app
                    //Try and avoid this by skipping the last few seconds of the video
                    if (currentTime >= mFreezeCheckPoint && mCurrentStreamInfo.getSubProtocol() != null && mCurrentStreamInfo.getSubProtocol().equals("hls")) {
                        mVideoView.stopPlayback();
                        itemComplete();

                    } else {
                        mLastReportedTime = currentTime;
                        mHandler.postDelayed(this, updatePeriod);
                    }
                } else {
                    mHandler.postDelayed(this, updatePeriod);
                }
            }
        };
        mHandler.postDelayed(mProgressLoop, getUpdatePeriod());
    }

    public void stopProgressAutomation() {
        if (mHandler != null && mProgressLoop != null) {
            mHandler.removeCallbacks(mProgressLoop);
        }
    }


    private void startReportLoop() {
        mReportLoop = new Runnable() {
            @Override
            public void run() {
                if (mPlaybackState == PlaybackState.PLAYING) {
                    int currentTime = mVideoView.getCurrentPosition();

                    Utils.ReportProgress(getCurrentlyPlayingItem(), getCurrentStreamInfo(), (long)currentTime * 10000);
                }
                mApplication.setLastUserInteraction(System.currentTimeMillis());
                if (mPlaybackState != PlaybackState.UNDEFINED && mPlaybackState != PlaybackState.IDLE) mHandler.postDelayed(this, REPORT_INTERVAL);
            }
        };
        mHandler.postDelayed(mReportLoop, REPORT_INTERVAL);
    }

    private void stopReportLoop() {
        if (mHandler != null && mReportLoop != null) {
            mHandler.removeCallbacks(mReportLoop);
        }

    }

    private void itemComplete() {
        mayBeFrozen = false;
        mPlaybackState = PlaybackState.IDLE;
        stopProgressAutomation();
        stopReportLoop();
        Long mbPos = (long) mVideoView.getCurrentPosition() * 10000;
        Utils.ReportStopped(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mbPos);
        if (mCurrentIndex < mItems.size() - 1) {
            // move to next in queue
            mCurrentIndex++;
            mApplication.getLogger().Debug("Moving to next queue item. Index: "+mCurrentIndex);
            mFragment.removeQueueItem(0);
            mFragment.updatePlaybackControls();
            spinnerOff = false;
            play(0);
        } else {
            // exit activity
            mApplication.getLogger().Debug("Last item completed. Finishing activity.");
            mFragment.finish();
        }
    }

    private void setupCallbacks() {

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                String msg = "";
                if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                    msg = mApplication.getString(R.string.video_error_media_load_timeout);
                } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    msg = mApplication.getString(R.string.video_error_server_inaccessible);
                } else {
                    msg = mApplication.getString(R.string.video_error_unknown_error);
                }
                Utils.showToast(mApplication, mApplication.getString(R.string.msg_video_playback_error) + msg);
                mApplication.getLogger().Error("Playback error - " + msg);
                mPlaybackState = PlaybackState.IDLE;
                stopProgressAutomation();
                stopReportLoop();
                return false;
            }
        });


        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

                mFreezeCheckPoint = mp.getDuration() > 60000 ? mp.getDuration() - 9000 : Integer.MAX_VALUE;
                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        mApplication.getLogger().Debug("Seek complete...");
                        mPlaybackState = PlaybackState.PLAYING;
                        mFragment.getPlaybackControlsRow().setCurrentTime(mp.getCurrentPosition());
                        startProgressAutomation();
                        startReportLoop();
                    }
                });

                if (mStartPosition > 0) {
                    mVideoView.seekTo(mStartPosition);
                    mStartPosition = 0; // clear for next item
                } else {
                    if (mPlaybackState == PlaybackState.BUFFERING) {
                        mPlaybackState = PlaybackState.PLAYING;
                        startProgressAutomation();
                        startReportLoop();
                    }
                }
            }
        });


        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                TvApp.getApplication().getLogger().Debug("On Completion fired");
                itemComplete();
            }
        });

    }

    public int getCurrentPosition() {
        return mFragment.getPlaybackControlsRow().getCurrentTime();
    }

    public boolean isPaused() {
        return mPlaybackState == PlaybackState.PAUSED;
    }

    public boolean isIdle() {
        return mPlaybackState == PlaybackState.IDLE;
    }


    /*
 * List of various states that we can be in
 */
    public static enum PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE, SEEKING, UNDEFINED;
    }

}
