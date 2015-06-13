package tv.emby.embyatv.playback;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcUtil;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaList;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 6/13/2015.
 */
public class VlcVideoView extends SurfaceView implements IVideoView, SurfaceHolder.Callback, IVideoPlayer {

    private SurfaceHolder holder;
    private LibVLC libvlc;
    private String currentVideoPath;
    private VlcEventHandler mHandler = new VlcEventHandler();

    public VlcVideoView(Context context) {
        super(context);
        init();
    }

    public VlcVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VlcVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        holder = getHolder();
        holder.addCallback(this);
    }

    @Override
    public int getDuration() {
        return libvlc != null ? ((Long) libvlc.getLength()).intValue() : -1;
    }

    @Override
    public int getCurrentPosition() {
        return libvlc != null ? ((Float) libvlc.getPosition()).intValue() : -1;
    }

    @Override
    public boolean isPlaying() {
        return libvlc != null && libvlc.isPlaying();
    }

    @Override
    public void start() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stopPlayback() {

    }

    @Override
    public void seekTo(int pos) {

    }

    @Override
    public void setVideoPath(String path) {
        currentVideoPath = path;
        createPlayer(path);

    }

    private void createPlayer(String path) {
        releasePlayer();
        try {

            // Create a new media player
            libvlc = new LibVLC();
            libvlc.init(getContext());
            libvlc.attachSurface(holder.getSurface(), this);
            
//            libvlc.setHardwareAcceleration(LibVLC.HW_ACCELERATION_FULL);
//            libvlc.setSubtitlesEncoding("");
//            libvlc.setAout(LibVLC.AOUT_OPENSLES);
//            libvlc.setTimeStretching(true);
//            libvlc.setVerboseMode(true);
//            libvlc.setVout(LibVLC.VOUT_ANDROID_WINDOW);
            EventHandler.getInstance().addHandler(mHandler);
            holder.setKeepScreenOn(true);

            Media media = new Media(libvlc, path);
            libvlc.playMRL(media.getMrl());
        } catch (Exception e) {
            TvApp.getApplication().getLogger().ErrorException("Error creating VLC player", e);
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_video_playback_error));
        }
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        EventHandler.getInstance().removeHandler(mHandler);
        libvlc.stop();
        libvlc.detachSurface();
        libvlc = null;

    }

    @Override
    public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
        mHandler.setOnErrorListener(listener);
    }

    @Override
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        mHandler.setOnCompletionListener(listener);
    }

    @Override
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
        mHandler.setOnPreparedListener(listener);
    }

    @Override
    public void setOnSeekCompleteListener(MediaPlayer mp, MediaPlayer.OnSeekCompleteListener listener) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if (libvlc != null)
            libvlc.attachSurface(holder.getSurface(), this);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void setSurfaceLayout(int i, int i1, int i2, int i3, int i4, int i5) {

    }

    @Override
    public int configureSurface(Surface surface, int i, int i1, int i2) {
        return -1;
    }

    @Override
    public void eventHardwareAccelerationError() {
        TvApp.getApplication().getLogger().Error("Hardware Acceleration Error");
        Utils.showToast(getContext(), "Hardware Acceleration Error");
    }
}
