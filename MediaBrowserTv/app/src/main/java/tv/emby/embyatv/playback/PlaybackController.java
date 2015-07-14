package tv.emby.embyatv.playback;

import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;

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
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.session.PlayMethod;
import mediabrowser.model.session.PlaybackStartInfo;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 12/9/2014.
 */
public class PlaybackController {
    List<BaseItemDto> mItems;
    VlcManager mVideoManager;
    int mCurrentIndex = 0;
    private long mCurrentPosition = 0;
    private PlaybackState mPlaybackState = PlaybackState.IDLE;
    private TvApp mApplication;

    private StreamInfo mCurrentStreamInfo;

    private IPlaybackOverlayFragment mFragment;
    private View mSpinner;
    private Boolean spinnerOff = false;

    private VideoOptions mCurrentOptions;

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

    private boolean updateProgress = true;

    public PlaybackController(List<BaseItemDto> items, IPlaybackOverlayFragment fragment) {
        mItems = items;
        mFragment = fragment;
        mApplication = TvApp.getApplication();
        mHandler = new Handler();

    }

    public void init(VlcManager mgr, View spinner) {
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
    public boolean canSeek() {return getCurrentlyPlayingItem() != null && !"TvChannel".equals(getCurrentlyPlayingItem().getType());}
    public int getSubtitleStreamIndex() {return (mCurrentOptions != null && mCurrentOptions.getSubtitleStreamIndex() != null) ? mCurrentOptions.getSubtitleStreamIndex() : -1; }
    public int getAudioStreamIndex() {return (mCurrentOptions != null) ? mCurrentOptions.getAudioStreamIndex() : 0; }

    public boolean hasNextItem() { return mCurrentIndex < mItems.size() - 1; }
    public BaseItemDto getNextItem() { return hasNextItem() ? mItems.get(mCurrentIndex+1) : null; }

    public boolean isPlaying() {
        return mPlaybackState == PlaybackState.PLAYING;
    }

    public void play(long position) {
        if (!TvApp.getApplication().isValid()) {
            Utils.showToast(TvApp.getApplication(), "Playback not supported. Please unlock or become a supporter.");
            return;
        }

        if (TvApp.getApplication().isTrial()) {
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getRegistrationString()+". Unlock or become a supporter for unlimited playback.");

        }

        mApplication.getLogger().Debug("Play called with pos: "+position);
        switch (mPlaybackState) {
            case PLAYING:
                // do nothing
                break;
            case PAUSED:
                // just resume
                mVideoManager.play();
                mPlaybackState = PlaybackState.PLAYING;
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
                TvApp.getApplication().getLogger().Debug("Max bitrate is: " + getMaxBitrate());

                // Create our profile and clear out subtitles so that they will burn in
                AndroidProfile profile = new AndroidProfile("vlc");
                mCurrentOptions.setProfile(profile);

                playInternal(getCurrentlyPlayingItem(), position, mVideoManager, mCurrentOptions);
                mPlaybackState = PlaybackState.BUFFERING;
                if (mFragment != null) {
                    mFragment.setPlayPauseActionState(ImageButton.STATE_SECONDARY);
                    mFragment.setFadingEnabled(true);
                    mFragment.setCurrentTime(position);
                }

                if (hasNextItem() && getCurrentlyPlayingItem().getRunTimeTicks() != null) {
                    // Determine the "next up" threshold
                    long duration = getCurrentlyPlayingItem().getRunTimeTicks() / 10000;
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
        String maxRate = sharedPref.getString("pref_max_bitrate", "15");
        Float factor = Float.parseFloat(maxRate) * 10;
        return (factor.intValue() * 100000);
    }

    private void playInternal(final BaseItemDto item, final long position, final VlcManager vlcManager, VideoOptions options) {
        final ApiClient apiClient = mApplication.getApiClient();
        mApplication.setCurrentPlayingItem(item);
        isLiveTv = item.getType().equals("TvChannel");
        if (isLiveTv) {
            liveTvChannelName = " ("+item.getName()+")";
            updateTvProgramInfo();
            mApplication.setLastLiveTvChannel(item.getId());
        }

        mApplication.getPlaybackManager().getVideoStreamInfo(apiClient.getServerInfo().getId(), options, false, apiClient, new Response<StreamInfo>() {
            @Override
            public void onResponse(StreamInfo response) {
                mCurrentStreamInfo = response;
                mFragment.updateDisplay();
                Long mbPos = position * 10000;

                String path = response.ToUrl(apiClient.getApiUrl(), apiClient.getAccessToken()); //apiClient.getApiUrl()+"/videos/"+item.getId()+"?static=true&mediasourceid="+response.getMediaSourceId()+"&api_key="+apiClient.getAccessToken(); 
                vlcManager.setVideoPath(path);
                setPlaybackMethod(response.getPlayMethod());
                if (mPlaybackMethod != PlayMethod.Transcode) {
                    mCurrentOptions.setAudioStreamIndex(response.getMediaSource().getDefaultAudioStreamIndex());
                    mCurrentOptions.setSubtitleStreamIndex(response.getMediaSource().getDefaultSubtitleStreamIndex());
                }
                vlcManager.start();
                mStartPosition = position;

                PlaybackStartInfo startInfo = new PlaybackStartInfo();
                startInfo.setItemId(item.getId());
                startInfo.setPositionTicks(mbPos);
                TvApp.getApplication().getPlaybackManager().reportPlaybackStart(startInfo, false, apiClient, new EmptyResponse());
                //TvApp.getApplication().getLogger().Info("Playback of " + item.getName() + "(" + path + ") started.");
            }

            @Override
            public void onError(Exception exception) {
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

    private void switchStreamInternal(final StreamInfo current, final long position, final VlcManager vlcManager) {

        TvApp.getApplication().getPlaybackManager().changeVideoStream(
                current,
                TvApp.getApplication().getApiClient().getServerInfo().getId(),
                mCurrentOptions,
                TvApp.getApplication().getApiClient(),
                new Response<StreamInfo>() {
                    @Override
                    public void onResponse(StreamInfo response) {
                        mCurrentStreamInfo = response;
                        Long mbPos = position * 10000;

                        String path = response.ToUrl(TvApp.getApplication().getApiClient().getApiUrl(), TvApp.getApplication().getApiClient().getAccessToken());
                        mStartPosition = position;
                        vlcManager.setVideoPath(path);
                        setPlaybackMethod(response.getPlayMethod());
                        vlcManager.start();

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

    public void switchAudioStream(int index) {
        if (!isPlaying()) return;

        mCurrentOptions.setAudioStreamIndex(index);
        if (mCurrentStreamInfo.getPlayMethod() == PlayMethod.Transcode) {
            startSpinner();
            mApplication.getLogger().Debug("Setting audio index to: " + index);
            mCurrentOptions.setMediaSourceId(getCurrentMediaSource().getId());
            stop();
            switchStreamInternal(mCurrentStreamInfo, mCurrentPosition, mVideoManager);
        } else {
            mVideoManager.setAudioTrack(index);
        }
    }

    public void switchSubtitleStream(int index) {
        if (!isPlaying()) return;
        mCurrentOptions.setSubtitleStreamIndex(index >= 0 ? index : null);
        if (mCurrentStreamInfo.getPlayMethod() == PlayMethod.Transcode) {
            startSpinner();
            mApplication.getLogger().Debug("Setting subtitle index to: " + index);
            mCurrentOptions.setMediaSourceId(getCurrentMediaSource().getId());
            stop();
            switchStreamInternal(mCurrentStreamInfo, mCurrentPosition, mVideoManager);

        } else  {
            MediaStream stream = Utils.GetMediaStream(getCurrentMediaSource(), index);
            if (index == -1 || (stream != null && !stream.getIsExternal())) {
                mVideoManager.setSubtitleTrack(index);
            } else {
                Utils.showToast(mApplication, "External subs not supported yet");
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
        // call once more to be sure everything up to date
        Utils.ReportProgress(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mVideoManager.getCurrentPosition() * 10000, true);

    }

    public void playPause() {
        switch (mPlaybackState) {
            case PLAYING:
                pause();
                break;
            case PAUSED:
            case IDLE:
                play(getCurrentPosition());
                break;
        }
    }

    public void stop() {
        if (mPlaybackState != PlaybackState.IDLE && mPlaybackState != PlaybackState.UNDEFINED) {
            mPlaybackState = PlaybackState.IDLE;
            stopReportLoop();
            if (mVideoManager.isPlaying()) mVideoManager.stopPlayback();
            //give it a just a beat to actually stop - this keeps it from re-requesting the stream after we tell the server we've stopped
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Long mbPos = mCurrentPosition * 10000;
            Utils.ReportStopped(getCurrentlyPlayingItem(), getCurrentStreamInfo(), mbPos);
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
            mFragment.removeQueueItem(0);
            spinnerOff = false;
            play(0);
        }
    }

    public void prev() {

    }

    public void seek(final long pos) {
        mApplication.getLogger().Debug("Seeking to " + pos);
        mVideoManager.seekTo(pos);
        if (mFragment != null) {
            mFragment.updateEndTime(mVideoManager.getDuration() - pos);
        }

    }

    private long currentSkipAmt = 0;
    private Runnable skipRunnable = new Runnable() {
        @Override
        public void run() {
            seek(mVideoManager.getCurrentPosition() + currentSkipAmt);
            currentSkipAmt = 0;
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
            currentSkipAmt += msec;
            updateProgress = false; // turn this off so we can show where it will be jumping to
            mFragment.setCurrentTime(mVideoManager.getCurrentPosition() + currentSkipAmt);
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
                    ProgramInfoDto program = response.getCurrentProgram();
                    if (program != null) {
                        channel.setName(program.getName() + liveTvChannelName);
                        channel.setPremiereDate(program.getStartDate());
                        channel.setEndDate(program.getEndDate());
                        channel.setOfficialRating(program.getOfficialRating());
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

    private void stopReportLoop() {
        if (mHandler != null && mReportLoop != null) {
            mHandler.removeCallbacks(mReportLoop);

        }

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
            mFragment.removeQueueItem(0);
            spinnerOff = false;
            play(0);
        } else {
            // exit activity
            mApplication.getLogger().Debug("Last item completed. Finishing activity.");
            mFragment.finish();
        }
    }

    private void setupCallbacks() {

        mVideoManager.setOnErrorListener(new PlaybackListener() {

            @Override
            public void onEvent() {
                String msg =  mApplication.getString(R.string.video_error_unknown_error);
                Utils.showToast(mApplication, mApplication.getString(R.string.msg_video_playback_error) + msg);
                mApplication.getLogger().Error("Playback error - " + msg);
                mPlaybackState = PlaybackState.IDLE;
                stopReportLoop();

            }
        });


        mVideoManager.setOnPreparedListener(new PlaybackListener() {
            @Override
            public void onEvent() {

                long timeLeft = mVideoManager.getDuration();
                if (mStartPosition > 0) {
                    seek(mStartPosition);
                    timeLeft -= mStartPosition;
                    mStartPosition = 0; // clear for next item
                }
                if (mPlaybackState == PlaybackState.BUFFERING) {
                    mPlaybackState = PlaybackState.PLAYING;
                    mFragment.updateEndTime(timeLeft);
                    startReportLoop();
                }
                TvApp.getApplication().getLogger().Info("VLC status: ", mVideoManager.getState());

            }
        });

        mVideoManager.setOnProgressListener(new PlaybackListener() {
            @Override
            public void onEvent() {
                if (isPlaying() && updateProgress) {
                    if (!spinnerOff) {
                        stopSpinner();
                        mVideoManager.setSubtitleTrack(mCurrentOptions.getSubtitleStreamIndex() != null ? mCurrentOptions.getSubtitleStreamIndex() : -1);
                        mVideoManager.setAudioTrack(mCurrentOptions.getAudioStreamIndex() != null ? mCurrentOptions.getAudioStreamIndex() : 0);
                    }
                    mApplication.setLastUserInteraction(System.currentTimeMillis()); // don't want to auto logoff during playback
                    if (isLiveTv && mCurrentProgramEndTime > 0 && System.currentTimeMillis() >= mCurrentProgramEndTime) {
                        // crossed fire off an async routine to update the program info
                        updateTvProgramInfo();
                    }
                    final Long currentTime = isLiveTv && mCurrentProgramStartTime > 0 ? getRealTimeProgress() : mVideoManager.getCurrentPosition();
                    mFragment.setCurrentTime(currentTime);
                    mCurrentPosition = currentTime;
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


    /*
 * List of various states that we can be in
 */
    public enum PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE, SEEKING, UNDEFINED;
    }

}
