package org.jellyfin.androidtv.ui.playback;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dto.MediaSourceInfo;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.entities.MediaStreamType;
import org.koin.java.KoinJavaComponent;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class VideoManager implements IVLCVout.OnNewVideoLayoutListener {
    public final static int ZOOM_FIT = 0;
    public final static int ZOOM_AUTO_CROP = 1;
    public final static int ZOOM_STRETCH = 2;

    private int mZoomMode = ZOOM_FIT;

    private PlaybackOverlayActivity mActivity;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private SurfaceView mSubtitlesSurface;
    private FrameLayout mSurfaceFrame;
    private ExoPlayer mExoPlayer;
    private PlayerView mExoPlayerView;
    private AspectRatioFrameLayout mAspectRatioFrameLayout;
    private LibVLC mLibVLC;
    private org.videolan.libvlc.MediaPlayer mVlcPlayer;
    private Media mCurrentMedia;
    private VlcEventHandler mVlcHandler = new VlcEventHandler();
    private Handler mHandler = new Handler();
    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;

    private long mForcedTime = -1;
    private long mLastTime = -1;
    private long mMetaDuration = -1;
    private long mMetaVLCStreamStartPosition = -1;
    private long lastExoPlayerPosition = -1;

    private boolean nativeMode = false;
    private boolean mSurfaceReady = false;
    public boolean isContracted = false;
    private PlaybackListener errorListener;
    private PlaybackListener completionListener;
    private PlaybackListener preparedListener;

    public VideoManager(PlaybackOverlayActivity activity, View view) {
        mActivity = activity;
        mSurfaceView = view.findViewById(R.id.player_surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(mSurfaceCallback);
        mSurfaceFrame = view.findViewById(R.id.player_surface_frame);
        mSubtitlesSurface = view.findViewById(R.id.subtitles_surface);
        mSubtitlesSurface.setZOrderMediaOverlay(true);
        mSubtitlesSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mExoPlayer = configureExoplayerBuilder(activity).build();
        mExoPlayerView = view.findViewById(R.id.exoPlayerView);
        mExoPlayerView.setPlayer(mExoPlayer);
        mExoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Timber.e("***** Got error from player");
                if (errorListener != null) errorListener.onEvent();
                stopProgressLoop();
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    Timber.d("Player is buffering");
                }
                // Do not call listener when paused
                if (playbackState == Player.STATE_READY && playWhenReady) {
                    if (preparedListener != null) preparedListener.onEvent();
                    startProgressLoop();
                } else if (playbackState == Player.STATE_ENDED) {
                    if (completionListener != null) completionListener.onEvent();
                    stopProgressLoop();
                }
            }

            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                // discontinuity for reason internal usually indicates an error, and that the player will reset to its default timestamp
                if (reason == Player.DISCONTINUITY_REASON_INTERNAL) {
                    Timber.d("Caught player discontinuity (reason internal) - oldPos: %s newPos: %s", oldPosition.positionMs, newPosition.positionMs);
                }
            }

            @Override
            public void onTimelineChanged(Timeline timeline, int reason) {
                Timber.d("Caught player timeline change - reason: %s", reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED ? "PLAYLIST_CHANGED" : "SOURCE_UPDATE");
            }
        });
    }

    /**
     * Configures Exoplayer for video playback. Initially we try with core decoders, but allow
     * ExoPlayer to silently fallback to software renderers.
     *
     * @param context The associated context
     * @return A configured builder for Exoplayer
     */
    private ExoPlayer.Builder configureExoplayerBuilder(Context context) {
        ExoPlayer.Builder exoPlayerBuilder = new ExoPlayer.Builder(context);

        DefaultRenderersFactory defaultRendererFactory =
                new DefaultRenderersFactory(context) {
                    @Override
                    protected void buildTextRenderers(Context context, TextOutput output,
                                                      Looper outputLooper, int extensionRendererMode,
                                                      ArrayList<Renderer> out) {
                        // Do not add text renderers since we handle subtitles
                    }
                };
        defaultRendererFactory.setEnableDecoderFallback(true);
        defaultRendererFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        exoPlayerBuilder.setRenderersFactory(defaultRendererFactory);

        return exoPlayerBuilder;
    }

    public boolean isInitialized() {
        return mSurfaceReady && (isNativeMode() ? mExoPlayer != null : mVlcPlayer != null);
    }

    public void init(int buffer, boolean isInterlaced) {
        createPlayer(buffer, isInterlaced);
    }

    public void setNativeMode(boolean value) {
        nativeMode = value;
        if (nativeMode) {
            mExoPlayerView.setVisibility(View.VISIBLE);
        } else {
            mExoPlayerView.setVisibility(View.GONE);
        }
    }

    public boolean isNativeMode() {
        return nativeMode;
    }

    public int getZoomMode() {
        return mZoomMode;
    }

    public void setZoom(int mode) {
        mZoomMode = mode;
        switch (mode) {
            case ZOOM_FIT:
                mExoPlayerView.setResizeMode(mAspectRatioFrameLayout.RESIZE_MODE_FIT);
                break;
            case ZOOM_AUTO_CROP:
                mExoPlayerView.setResizeMode(mAspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                break;
            case ZOOM_STRETCH:
                mExoPlayerView.setResizeMode(mAspectRatioFrameLayout.RESIZE_MODE_FILL);
                break;
        }
    }

    // set by playbackController when a new vlc transcode stream starts, and before seeking
    public void setMetaVLCStreamStartPosition(long value) {
        mMetaVLCStreamStartPosition = value;
    }

    public long getMetaVLCStreamStartPosition() {
        return mMetaVLCStreamStartPosition;
    }

    public void setMetaDuration(long duration) {
        mMetaDuration = duration;
    }

    public long getDuration() {
        if (nativeMode) {
            return isInitialized() && mExoPlayer.getDuration() > 0 ? mExoPlayer.getDuration() : mMetaDuration;
        } else {
            return isInitialized() && mVlcPlayer.getLength() > 0 ? mVlcPlayer.getLength() : mMetaDuration;
        }
    }

    public long getBufferedPosition() {
        if (!isInitialized())
            return -1;

        // only exoplayer supports reporting buffered position
        if (!isNativeMode())
            return -1;

        long bufferedPosition = mExoPlayer.getBufferedPosition();

        if (bufferedPosition > -1 && bufferedPosition < getDuration()) {
            return bufferedPosition;
        }
        return -1;
    }

    public long getCurrentPosition() {
        if (nativeMode) {
            if (mExoPlayer == null || !isPlaying()) {
                return lastExoPlayerPosition == -1 ? 0 : lastExoPlayerPosition;
            } else {
                long mExoPlayerCurrentPosition = mExoPlayer.getCurrentPosition();
                lastExoPlayerPosition = mExoPlayerCurrentPosition;
                return mExoPlayerCurrentPosition;
            }
        }

        if (mVlcPlayer == null) return 0;

        long time = mVlcPlayer.getTime();

        // vlc returns ms from stream start. metaStartPosition + time = actual position
        time = mMetaVLCStreamStartPosition != -1 ? time + getMetaVLCStreamStartPosition() : time;
        if (mForcedTime != -1 && mLastTime != -1) {
            /* XXX: After a seek, mLibVLC.getTime can return the position before or after
             * the seek position. Therefore we return mForcedTime in order to avoid the seekBar
             * to move between seek position and the actual position.
             * We have to wait for a valid position (that is after the seek position).
             * to re-init mLastTime and mForcedTime to -1 and return the actual position.
             */
            if (mLastTime > mForcedTime) {
                if (time <= mLastTime && time > mForcedTime)
                    mLastTime = mForcedTime = -1;
            } else {
                if (time > mForcedTime)
                    mLastTime = mForcedTime = -1;
            }
        }
        return mForcedTime == -1 ? time : mForcedTime;
    }

    public boolean isPlaying() {
        return nativeMode ? mExoPlayer.isPlaying() : mVlcPlayer != null && mVlcPlayer.isPlaying();
    }

    public void start() {
        if (nativeMode) {
            if (mExoPlayer == null) {
                Timber.e("mExoPlayer should not be null!!");
                mActivity.finish();
                return;
            }
            mExoPlayer.setPlayWhenReady(true);
            normalWidth = mExoPlayerView.getLayoutParams().width;
            normalHeight = mExoPlayerView.getLayoutParams().height;
        } else {
            if (!mSurfaceReady) {
                Timber.e("Attempt to play before surface ready");
                return;
            }
            if (!mVlcPlayer.isPlaying()) {
                mVlcPlayer.play();
            }
        }
    }

    public void play() {
        if (nativeMode) {
            mExoPlayer.setPlayWhenReady(true);
        } else {
            mVlcPlayer.play();
        }
    }

    public void pause() {
        if (nativeMode) {
            mExoPlayer.setPlayWhenReady(false);
        } else {
            mVlcPlayer.pause();
        }
    }

    public void stopPlayback() {
        if (nativeMode && mExoPlayer != null) {
            mExoPlayer.stop();
        } else if (mVlcPlayer != null) {
            mVlcPlayer.stop();
        }
        stopProgressLoop();
    }

    public boolean isSeekable() {
        if (!isInitialized())
            return false;
        boolean canSeek = false;
        if (isNativeMode())
            canSeek = mExoPlayer.isCurrentMediaItemSeekable();
        else {
            canSeek = mVlcPlayer.isSeekable() && mVlcPlayer.getLength() > 0;
        }
        Timber.d("current media item is%s seekable", canSeek ? "" : " not");
        return canSeek;
    }

    public long seekTo(long pos) {
        if (!isInitialized())
            return -1;
        if (nativeMode) {
            Timber.i("Exo length in seek is: %d", getDuration());
            mExoPlayer.seekTo(pos);
            return pos;
        } else {
            if (mVlcPlayer == null || !mVlcPlayer.isSeekable()) return -1;
            mForcedTime = pos;
            mLastTime = mVlcPlayer.getTime();
            Timber.i("VLC length in seek is: %d", getDuration());
            try {
                if (getDuration() > 0) mVlcPlayer.setPosition((float) pos / getDuration());
                else mVlcPlayer.setTime(pos);

                return pos;

            } catch (Exception e) {
                Timber.e(e, "Error seeking in VLC");
                Utils.showToast(mActivity, mActivity.getString(R.string.seek_error));
                return -1;
            }
        }
    }

    public void setVideoPath(@Nullable String path) {
        if (path == null) {
            Timber.w("Video path is null cannot continue");
            return;
        }
        Timber.i("Video path set to: %s", path);

        if (nativeMode) {
            try {
                mExoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(path)));
                mExoPlayer.prepare();
            } catch (IllegalStateException e) {
                Timber.e(e, "Unable to set video path.  Probably backing out.");
            }
        } else {
            mCurrentMedia = new Media(mLibVLC, Uri.parse(path));
            mCurrentMedia.parse();
            mVlcPlayer.setMedia(mCurrentMedia);

            mCurrentMedia.release();
        }
    }

    public void disableSubs() {
        if (!nativeMode && mVlcPlayer != null) mVlcPlayer.setSpuTrack(-1);
    }

    public boolean setSubtitleTrack(int index, @Nullable List<MediaStream> allStreams) {
        if (!nativeMode && allStreams != null) {
            //find the relative order of our sub index within the sub tracks in VLC
            int vlcIndex = 1; // start at 1 to account for "disabled"
            for (MediaStream stream : allStreams) {
                if (stream.getType() == MediaStreamType.Subtitle && !stream.getIsExternal()) {
                    if (stream.getIndex() == index) {
                        break;
                    }
                    vlcIndex++;
                }
            }

            org.videolan.libvlc.MediaPlayer.TrackDescription vlcSub;
            try {
                vlcSub = getSubtitleTracks()[vlcIndex];

            } catch (IndexOutOfBoundsException e) {
                Timber.e("Could not locate subtitle with index %s in vlc track info", index);
                return false;
            } catch (NullPointerException e) {
                Timber.e("No subtitle tracks found in player trying to set subtitle with index %s in vlc track info", index);
                return false;
            }

            Timber.i("Setting Vlc sub to %s", vlcSub.name);
            return mVlcPlayer.setSpuTrack(vlcSub.id);

        }

        return false;
    }

    public int getVLCAudioTrack() {
        if (!isInitialized() || nativeMode)
            return -1;
        return mVlcPlayer.getAudioTrack();
    }

    public int setVLCAudioTrack(int ndx) {
        if (!isInitialized() || ndx < 0)
            return -1;

        if (isNativeMode()) {
            Timber.e("Cannot set audio track in native mode");
            return -1;
        }

        //debug
        Timber.d("Setting VLC audio track index to: %d", ndx);
        for (org.videolan.libvlc.MediaPlayer.TrackDescription track : mVlcPlayer.getAudioTracks()) {
            Timber.d("VLC Audio Track: %s / %d", track.name, track.id);
        }
        //

        boolean matched = false;
        for (org.videolan.libvlc.MediaPlayer.TrackDescription track : mVlcPlayer.getAudioTracks()) {
            if (track.id == ndx) {
                matched = true;
                break;
            }
        }

        org.videolan.libvlc.MediaPlayer.TrackDescription vlcTrack = null;

        if (matched) {
            try {
                vlcTrack = mVlcPlayer.getAudioTracks()[ndx];
            } catch (IndexOutOfBoundsException e) {
                Timber.e("Could not locate audio with index %s in vlc track info", ndx);
                return -1;
            } catch (NullPointerException e) {
                Timber.e("Could not locate audio track with index %s in vlc, null exception", ndx);
                return -1;
            }
        }

        if (!matched || vlcTrack == null) {
            Timber.e("Could not locate audio track with index %s in vlc", ndx);
            return -1;
        } else if (mVlcPlayer.getAudioTrack() == ndx) {
            Timber.d("provided index points to the audio track already in use, aborting");
            return -1;
        }

        if (mVlcPlayer.setAudioTrack(vlcTrack.id)) {
            Timber.i("Setting by ID was successful");
        } else {
            Timber.i("Setting by ID not successful, trying index");
            mVlcPlayer.setAudioTrack(ndx);
        }
        return ndx;
    }

    public void setPlaybackSpeed(@NonNull Double speed) {
        if (speed < 0.25) {
            Timber.w("Invalid playback speed requested: %d", speed);
            return;
        }

        if (nativeMode) {
            mExoPlayer.setPlaybackParameters(new PlaybackParameters(speed.floatValue()));
        } else {
            mVlcPlayer.setRate(speed.floatValue());
        }
    }

    public void setAudioDelay(long value) {
        if (!nativeMode && mVlcPlayer != null) {
            if (!mVlcPlayer.setAudioDelay(value * 1000)) {
                Timber.e("Error setting audio delay");
            } else {
                Timber.i("Audio delay set to %d", value);
            }
        }
    }

    public long getAudioDelay() {
        return mVlcPlayer != null ? mVlcPlayer.getAudioDelay() / 1000 : 0;
    }

    public void setCompatibleAudio() {
        if (!nativeMode) {
            mVlcPlayer.setAudioOutput("opensles_android");
            mVlcPlayer.setAudioOutputDevice("hdmi");
        }
    }

    public void setAudioMode() {
        if (!nativeMode) {
            setVlcAudioOptions();
        }
    }

    private void setVlcAudioOptions() {
        if (!Utils.downMixAudio()) {
            mVlcPlayer.setAudioDigitalOutputEnabled(true);
        } else {
            setCompatibleAudio();
        }
    }

    public void setVideoTrack(MediaSourceInfo mediaSource) {
        if (!nativeMode && mediaSource != null && mediaSource.getMediaStreams() != null) {
            for (MediaStream stream : mediaSource.getMediaStreams()) {
                if (stream.getType() == MediaStreamType.Video && stream.getIndex() >= 0) {
                    Timber.d("Setting video index to: %d", stream.getIndex());
                    mVlcPlayer.setVideoTrack(stream.getIndex());
                    return;
                }
            }
        }
    }

    public org.videolan.libvlc.MediaPlayer.TrackDescription[] getSubtitleTracks() {
        return nativeMode ? null : mVlcPlayer.getSpuTracks();
    }

    public void destroy() {
        stopPlayback();
        releasePlayer();
    }

    private void createPlayer(int buffer, boolean isInterlaced) {
        try {
            // Create a new media player
            ArrayList<String> options = new ArrayList<>(20);
            options.add("--network-caching=" + buffer);
            options.add("--no-audio-time-stretch");
            options.add("--avcodec-skiploopfilter");
            options.add("1");
            options.add("--avcodec-skip-frame");
            options.add("0");
            options.add("--avcodec-skip-idct");
            options.add("0");
            options.add("--android-display-chroma");
            options.add("RV32");
            options.add("--audio-resampler");
            options.add("soxr");
            options.add("--stats");
            if (isInterlaced) {
                options.add("--video-filter=deinterlace");
                options.add("--deinterlace-mode=Bob");
            }
//            options.add("--subsdec-encoding");
//            options.add("Universal (UTF-8)");
            options.add("--audio-desync");
            options.add(String.valueOf(KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getLibVLCAudioDelay())));
            options.add("-v");
            options.add("--vout=android-opaque,android-display");

            mLibVLC = new LibVLC(mActivity, options);
            Timber.i("Network buffer set to %d", buffer);

            mVlcPlayer = new org.videolan.libvlc.MediaPlayer(mLibVLC);
            setVlcAudioOptions();

            mSurfaceHolder.addCallback(mSurfaceCallback);
            mVlcPlayer.setEventListener(mVlcHandler);

            //setup surface
            mVlcPlayer.getVLCVout().detachViews();
            mVlcPlayer.getVLCVout().setVideoView(mSurfaceView);
            mVlcPlayer.getVLCVout().setSubtitlesView(mSubtitlesSurface);
            mVlcPlayer.getVLCVout().attachViews(this);
            Timber.d("Surface attached");
            mSurfaceReady = true;
        } catch (Exception e) {
            Timber.e(e, "Error creating VLC player");
            Utils.showToast(mActivity, mActivity.getString(R.string.msg_video_playback_error));
        }
    }

    private void releasePlayer() {
        if (mVlcPlayer != null) {
            mVlcPlayer.setEventListener(null);
            mVlcPlayer.stop();
            mVlcPlayer.getVLCVout().detachViews();
            mVlcPlayer.release();
            mLibVLC.release();
            mLibVLC = null;
            mVlcPlayer = null;
        }

        if (mExoPlayer != null) {
            mExoPlayerView.setPlayer(null);
            mExoPlayer.release();
            mExoPlayer = null;
        }
        clearPlayerListeners();
    }

    int normalWidth;
    int normalHeight;

    public void contractVideo(int height) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) (nativeMode ? mExoPlayerView.getLayoutParams() : mSurfaceView.getLayoutParams());
        if (isContracted) return;

        int sw = mActivity.getWindow().getDecorView().getWidth();
        int sh = mActivity.getWindow().getDecorView().getHeight();
        float ar = (float) sw / sh;
        lp.height = height;
        lp.width = (int) Math.ceil(height * ar);
        lp.rightMargin = ((lp.width - normalWidth) / 2) - 110;
        lp.bottomMargin = ((lp.height - normalHeight) / 2) - 50;

        if (nativeMode) {
            mExoPlayerView.setLayoutParams(lp);
            mExoPlayerView.invalidate();
        } else mSurfaceView.setLayoutParams(lp);

        isContracted = true;

    }

    public void setVideoFullSize(boolean force) {
        if (normalHeight == 0) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) (nativeMode ? mExoPlayerView.getLayoutParams() : mSurfaceView.getLayoutParams());
        if (force) {
            lp.height = -1;
            lp.width = -1;
        } else {
            lp.height = normalHeight;
            lp.width = normalWidth;
        }
        if (nativeMode) {
            lp.rightMargin = 0;
            lp.bottomMargin = 0;
            mExoPlayerView.setLayoutParams(lp);
            mExoPlayerView.invalidate();
        } else mSurfaceView.setLayoutParams(lp);

        isContracted = false;

    }

    private void changeSurfaceLayout(int videoWidth, int videoHeight, int videoVisibleWidth, int videoVisibleHeight, int sarNum, int sarDen) {
        int sw;
        int sh;

        // get screen size
        if (mActivity == null) return; //called during destroy
        sw = mActivity.getWindow().getDecorView().getWidth();
        sh = mActivity.getWindow().getDecorView().getHeight();

        double dw = sw, dh = sh;
        boolean isPortrait;

        isPortrait = mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        if (sw > sh && isPortrait || sw < sh && !isPortrait) {
            dw = sh;
            dh = sw;
        }

        // sanity check
        if (dw * dh == 0 || videoWidth * videoHeight == 0) {
            Timber.e("Invalid surface size");
            return;
        }

        // compute the aspect ratio
        double ar;
        if (sarDen == sarNum) {
            /* No indication about the density, assuming 1:1 */
            ar = (double) videoVisibleWidth / (double) videoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            double vw = videoVisibleWidth * (double) sarNum / sarDen;
            ar = vw / videoVisibleHeight;
        }

        // compute the display aspect ratio
        double dar = dw / dh;

        if (dar < ar)
            dh = dw / ar;
        else
            dw = dh * ar;

        // set display size
        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = (int) Math.ceil(dw * videoWidth / videoVisibleWidth);
        lp.height = (int) Math.ceil(dh * videoHeight / videoVisibleHeight);
        normalWidth = lp.width;
        normalHeight = lp.height;
        mSurfaceView.setLayoutParams(lp);
        mSubtitlesSurface.setLayoutParams(lp);

        // set frame size (crop if necessary)
        if (mSurfaceFrame != null) {
            lp = mSurfaceFrame.getLayoutParams();
            lp.width = (int) Math.floor(dw);
            lp.height = (int) Math.floor(dh);
            mSurfaceFrame.setLayoutParams(lp);

        }

        Timber.d("Surface sized %d x %d ", lp.width, lp.height);
        mSurfaceView.invalidate();
        mSubtitlesSurface.invalidate();
    }

    public void setOnErrorListener(final PlaybackListener listener) {
        mVlcHandler.setOnErrorListener(listener);
        errorListener = listener;
    }

    public void setOnCompletionListener(final PlaybackListener listener) {
        completionListener = listener;
        mVlcHandler.setOnCompletionListener(listener);
    }

    public void setOnPreparedListener(final PlaybackListener listener) {
        preparedListener = listener;

        mVlcHandler.setOnPreparedListener(listener);
    }

    public void setOnProgressListener(PlaybackListener listener) {
        progressListener = listener;
        mVlcHandler.setOnProgressListener(listener);
    }

    public void clearPlayerListeners() {
        mVlcHandler.setOnErrorListener(null);
        mVlcHandler.setOnCompletionListener(null);
        mVlcHandler.setOnPreparedListener(null);
        mVlcHandler.setOnProgressListener(null);
    }

    private PlaybackListener progressListener;
    private Runnable progressLoop;

    private void startProgressLoop() {
        stopProgressLoop();
        progressLoop = new Runnable() {
            @Override
            public void run() {
                if (progressListener != null) progressListener.onEvent();
                mHandler.postDelayed(this, 500);
            }
        };
        mHandler.post(progressLoop);
    }

    private void stopProgressLoop() {
        if (progressLoop != null) {
            mHandler.removeCallbacks(progressLoop);
        }
    }

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mVlcPlayer != null) mVlcPlayer.getVLCVout().detachViews();
            mSurfaceReady = false;

        }
    };

    @Override
    public void onNewVideoLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0 || isContracted)
            return;

        // store video size
        mVideoHeight = height;
        mVideoWidth = width;
        mVideoVisibleHeight = visibleHeight;
        mVideoVisibleWidth = visibleWidth;
        mSarNum = sarNum;
        mSarDen = sarDen;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                changeSurfaceLayout(mVideoWidth, mVideoHeight, mVideoVisibleWidth, mVideoVisibleHeight, mSarNum, mSarDen);
            }
        });
    }
}
