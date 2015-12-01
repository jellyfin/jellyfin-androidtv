package tv.emby.embyatv.playback;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

    private static ItemRowAdapter mCurrentMediaAdapter;
    private static int mCurrentMediaPosition = -1;
    private static String currentMediaTitle;

    private static List<BaseItemDto> mCurrentAudioQueue;
    private static int mCurrentAudioQueuePosition = -1;
    private static BaseItemDto mCurrentAudioItem;
    private static StreamInfo mCurrentAudioStreamInfo;
    private static long mCurrentAudioPosition;

    private static LibVLC mLibVLC;
    private static org.videolan.libvlc.MediaPlayer mVlcPlayer;
    private static VlcEventHandler mVlcHandler = new VlcEventHandler();
    private static AudioManager mAudioManager;
    private static boolean audioInitialized;


    public static ItemRowAdapter getCurrentMediaAdapter() {
        return mCurrentMediaAdapter;
    }
    public static boolean hasAudioQueueItems() { return mCurrentAudioQueue != null && mCurrentAudioQueue.size() > 0; }

    public static void setCurrentMediaAdapter(ItemRowAdapter currentMediaAdapter) {
        MediaManager.mCurrentMediaAdapter = currentMediaAdapter;
    }

    public static int getCurrentMediaPosition() {
        return mCurrentMediaPosition;
    }
    public static int getCurrentAudioQueuePosition() { return mCurrentAudioQueuePosition; }

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

    public static int queueAudioItem(int pos, BaseItemDto item) {
        if (mCurrentAudioQueue == null) mCurrentAudioQueue = new ArrayList<>();
        mCurrentAudioQueue.add(pos, item);
        return pos;
    }

    public static int queueAudioItem(BaseItemDto item) {
        if (mCurrentAudioQueue == null) mCurrentAudioQueue = new ArrayList<>();
        return queueAudioItem(mCurrentAudioQueue.size(), item);
    }

    public static boolean isPlayingAudio() { return audioInitialized && mVlcPlayer.isPlaying(); }

    public static void playNow(final BaseItemDto item) {
        if (!audioInitialized) {
            audioInitialized = initAudio();
        }

        if (!audioInitialized) {
            Utils.showToast(TvApp.getApplication(), "Unable to play audio");
            return;
        }

        if (isPlayingAudio() && TvApp.getApplication().getCurrentActivity() != null) {
            new AlertDialog.Builder(TvApp.getApplication().getCurrentActivity())
                    .setTitle(TvApp.getApplication().getString(R.string.lbl_play))
                    .setMessage("How do you wish to play this item?")
                    .setPositiveButton("Right now", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            queueAudioItem(mCurrentAudioQueuePosition +1, item);
                            nextAudioItem();
                        }
                    })
                    .setNeutralButton("Next", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            queueAudioItem(mCurrentAudioQueuePosition +1, item);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            queueAudioItem(0, item);
            nextAudioItem();
        }
    }

    private static void playInternal(final BaseItemDto item, final int pos) {
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
                mCurrentAudioItem = item;
                mCurrentAudioStreamInfo = response;
                Media media = new Media(mLibVLC, Uri.parse(response.ToUrl(apiClient.getApiUrl(), apiClient.getAccessToken())));
                mCurrentAudioQueuePosition = pos;
                mCurrentAudioPosition = 0;
                media.parse();
                mVlcPlayer.setMedia(media);

                media.release();
                mVlcPlayer.play();

                Utils.ReportStart(item, mCurrentAudioPosition);
            }

            @Override
            public void onError(Exception exception) {
                Utils.showToast(TvApp.getApplication(), "Unable to play audio " + exception.getLocalizedMessage());
            }
        });

    }

    public static BaseItemDto getNextAudioItem() {
        if (mCurrentAudioQueue == null || mCurrentAudioQueue.size() == 0 || mCurrentAudioQueuePosition == mCurrentAudioQueue.size() - 1) return null;

        return mCurrentAudioQueue.get(mCurrentAudioQueuePosition +1);
    }

    public static int nextAudioItem() {
        if (mCurrentAudioQueue == null || mCurrentAudioQueue.size() == 0 || mCurrentAudioQueuePosition == mCurrentAudioQueue.size() - 1) return -1;
        stopAudio();
        playInternal(getNextAudioItem(), mCurrentAudioQueuePosition +1);
        return mCurrentAudioQueuePosition +1;
    }

    public static void stopAudio() {
        if (mCurrentAudioItem != null && isPlayingAudio()) {
            mVlcPlayer.stop();
            Utils.ReportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition);
        }
    }

    public static void setCurrentMediaPosition(int currentMediaPosition) {
        MediaManager.mCurrentMediaPosition = currentMediaPosition;
    }

    public static BaseRowItem getMediaItem(int pos) {
        return mCurrentMediaAdapter != null && mCurrentMediaAdapter.size() > pos ? (BaseRowItem) mCurrentMediaAdapter.get(pos) : null;
    }

    public static BaseRowItem getCurrentMediaItem() { return getMediaItem(mCurrentMediaPosition); }
    public static BaseRowItem nextMedia() {
        if (hasNextMediaItem()) {
            mCurrentMediaPosition++;
            mCurrentMediaAdapter.loadMoreItemsIfNeeded(mCurrentMediaPosition);
        }

        return getCurrentMediaItem();
    }

    public static BaseRowItem prevMedia() {
        if (hasPrevMediaItem()) {
            mCurrentMediaPosition--;
        }

        return getCurrentMediaItem();
    }

    public static BaseRowItem peekNextMediaItem() {
        return hasNextMediaItem() ? getMediaItem(mCurrentMediaPosition +1) : null;
    }

    public static BaseRowItem peekPrevMediaItem() {
        return hasPrevMediaItem() ? getMediaItem(mCurrentMediaPosition -1) : null;
    }

    public static boolean hasNextMediaItem() { return mCurrentMediaAdapter.size() > mCurrentMediaPosition +1; }
    public static boolean hasPrevMediaItem() { return mCurrentMediaPosition > 0; }

    public static String getCurrentMediaTitle() {
        return currentMediaTitle;
    }

    public static void setCurrentMediaTitle(String currentMediaTitle) {
        MediaManager.currentMediaTitle = currentMediaTitle;
    }
}
