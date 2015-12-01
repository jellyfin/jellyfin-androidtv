package tv.emby.embyatv.playback;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;

import org.acra.ACRA;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.profiles.AndroidProfile;
import mediabrowser.model.dlna.AudioOptions;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 10/22/2015.
 */
public class MediaManager {

    private static ItemRowAdapter currentMediaAdapter;
    private static int currentMediaPosition = -1;
    private static String currentMediaTitle;

    private static List<BaseItemDto> currentAudioQueue;
    private static int currentAudioQueuePosition = -1;

    private static LibVLC mLibVLC;
    private static org.videolan.libvlc.MediaPlayer mVlcPlayer;
    private static Media mCurrentMedia;
    private static VlcEventHandler mVlcHandler = new VlcEventHandler();
    private static AudioManager mAudioManager;
    private static boolean audioInitialized;


    public static ItemRowAdapter getCurrentMediaAdapter() {
        return currentMediaAdapter;
    }
    public static boolean hasAudioQueueItems() { return currentAudioQueue != null && currentAudioQueue.size() > 0; }

    public static void setCurrentMediaAdapter(ItemRowAdapter currentMediaAdapter) {
        MediaManager.currentMediaAdapter = currentMediaAdapter;
    }

    public static int getCurrentMediaPosition() {
        return currentMediaPosition;
    }
    public static int getCurrentAudioQueuePosition() { return currentAudioQueuePosition; }

    public static boolean initAudio() {
        if (mAudioManager == null) mAudioManager = (AudioManager) TvApp.getApplication().getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager == null) {
            TvApp.getApplication().getLogger().Error("Unable to get audio manager");
            Utils.showToast(TvApp.getApplication(), R.string.msg_cannot_play_time);
            return false;
        }

        return createPlayer(600);
    }

    private static boolean createPlayer(int buffer) {
        try {

            // Create a new media player
            ArrayList<String> options = new ArrayList<>(20);
            options.add("--network-caching=" + buffer);
            options.add("--no-audio-time-stretch");
            options.add("-v");

            mLibVLC = new LibVLC(options);
            LibVLC.setOnNativeCrashListener(new LibVLC.OnNativeCrashListener() {
                @Override
                public void onNativeCrash() {
                    new Exception().printStackTrace();
                    Utils.PutCustomAcraData();
                    ACRA.getErrorReporter().handleException(new Exception("Error in LibVLC"), false);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);

                }
            });

            mVlcPlayer = new org.videolan.libvlc.MediaPlayer(mLibVLC);
            SharedPreferences prefs = TvApp.getApplication().getPrefs();
            String audioOption = Utils.isFireTv() && !Utils.is50() ? "1" : prefs.getString("pref_audio_option","0"); // force compatible audio on Fire 4.2
            mVlcPlayer.setAudioOutput("0".equals(audioOption) ? "android_audiotrack" : "opensles_android");
            mVlcPlayer.setAudioOutputDevice("hdmi");


            mVlcPlayer.setEventListener(mVlcHandler);

        } catch (Exception e) {
            TvApp.getApplication().getLogger().ErrorException("Error creating VLC player", e);
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_video_playback_error));
            return false;
        }

        return true;
    }

    public static void testPlay(BaseItemDto item) {
        if (!audioInitialized) {
            audioInitialized = initAudio();
        }

        if (!audioInitialized) {
            Utils.showToast(TvApp.getApplication(), "Unable to play audio");
            return;
        }

        final ApiClient apiClient = TvApp.getApplication().getApiClient();
        AudioOptions options = new AudioOptions();
        options.setDeviceId(apiClient.getDeviceId());
        options.setItemId(item.getId());
        options.setMaxBitrate(TvApp.getApplication().getAutoBitrate());
        options.setMediaSources(item.getMediaSources());
        options.setProfile(new AndroidProfile("vlc"));
        TvApp.getApplication().getPlaybackManager().getAudioStreamInfo(apiClient.getServerInfo().getId(), options, false, apiClient, new Response<StreamInfo>() {
            @Override
            public void onResponse(StreamInfo response) {
                mCurrentMedia = new Media(mLibVLC, Uri.parse(response.ToUrl(apiClient.getApiUrl(), apiClient.getAccessToken())));
                mCurrentMedia.parse();
                mVlcPlayer.setMedia(mCurrentMedia);

                mCurrentMedia.release();
                mVlcPlayer.play();

            }

            @Override
            public void onError(Exception exception) {
                Utils.showToast(TvApp.getApplication(), "Unable to play audio " + exception.getLocalizedMessage());
            }
        });

    }

    public static void setCurrentMediaPosition(int currentMediaPosition) {
        MediaManager.currentMediaPosition = currentMediaPosition;
    }

    public static BaseRowItem getMediaItem(int pos) {
        return currentMediaAdapter != null && currentMediaAdapter.size() > pos ? (BaseRowItem) currentMediaAdapter.get(pos) : null;
    }

    public static BaseRowItem getCurrentMediaItem() { return getMediaItem(currentMediaPosition); }
    public static BaseRowItem nextMedia() {
        if (hasNextMediaItem()) {
            currentMediaPosition++;
            currentMediaAdapter.loadMoreItemsIfNeeded(currentMediaPosition);
        }

        return getCurrentMediaItem();
    }

    public static BaseRowItem prevMedia() {
        if (hasPrevMediaItem()) {
            currentMediaPosition--;
        }

        return getCurrentMediaItem();
    }

    public static BaseRowItem peekNextMediaItem() {
        return hasNextMediaItem() ? getMediaItem(currentMediaPosition+1) : null;
    }

    public static BaseRowItem peekPrevMediaItem() {
        return hasPrevMediaItem() ? getMediaItem(currentMediaPosition-1) : null;
    }

    public static boolean hasNextMediaItem() { return currentMediaAdapter.size() > currentMediaPosition+1; }
    public static boolean hasPrevMediaItem() { return currentMediaPosition > 0; }

    public static String getCurrentMediaTitle() {
        return currentMediaTitle;
    }

    public static void setCurrentMediaTitle(String currentMediaTitle) {
        MediaManager.currentMediaTitle = currentMediaTitle;
    }
}
