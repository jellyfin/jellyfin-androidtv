package tv.emby.embyatv.playback;

/**
 * Created by Eric on 4/28/2015.
 */
public interface IPlaybackOverlayFragment {
    public void setCurrentTime(int time);
    public void setFadingEnabled(boolean value);
    public void setPlayPauseActionState(int state);
    public void updatePlaybackControls();
    public void removeQueueItem(int pos);
    public void finish();
}
