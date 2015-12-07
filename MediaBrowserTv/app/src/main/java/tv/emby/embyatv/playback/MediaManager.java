package tv.emby.embyatv.playback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.util.RemoteControlReceiver;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 10/22/2015.
 */
public class MediaManager {

    private static ItemRowAdapter mCurrentMediaAdapter;
    private static int mCurrentMediaPosition = -1;
    private static String currentMediaTitle;

    private static ItemRowAdapter mCurrentAudioQueue;
    private static int mCurrentAudioQueuePosition = -1;
    private static BaseItemDto mCurrentAudioItem;
    private static StreamInfo mCurrentAudioStreamInfo;
    private static long mCurrentAudioPosition;

    private static LibVLC mLibVLC;
    private static org.videolan.libvlc.MediaPlayer mVlcPlayer;
    private static VlcEventHandler mVlcHandler = new VlcEventHandler();
    private static AudioManager mAudioManager;
    private static boolean audioInitialized;

    private static List<AudioEventListener> mAudioEventListeners = new ArrayList<>();

    private static long lastProgressReport;
    private static long lastProgressEvent;

    private static boolean mRepeat;

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

    public static int getCurrentAudioQueueSize() { return mCurrentAudioQueue != null ? mCurrentAudioQueue.size() : 0; }
    public static int getCurrentAudioQueuePosition() { return mCurrentAudioQueuePosition; }
    public static long getCurrentAudioPosition() { return mCurrentAudioPosition; }
    public static String getCurrentAudioQueueDisplayPosition() { return Integer.toString(mCurrentAudioQueuePosition+1); }
    public static String getCurrentAudioQueueDisplaySize() { return mCurrentAudioQueue != null ? Integer.toString(mCurrentAudioQueue.size()) : "0"; }

    public static BaseItemDto getCurrentAudioItem() { return mCurrentAudioItem; }

    public static boolean toggleRepeat() { mRepeat = !mRepeat; return mRepeat; }
    public static boolean isRepeatMode() { return mRepeat; }

    public static ItemRowAdapter getCurrentAudioQueue() { return mCurrentAudioQueue; }

    public static void addAudioEventListener(AudioEventListener listener) {
        mAudioEventListeners.add(listener);
        TvApp.getApplication().getLogger().Debug("Added event listener.  Total listeners: "+mAudioEventListeners.size());
    }
    public static void removeAudioEventListener(AudioEventListener listener) {
        mAudioEventListeners.remove(listener);
        TvApp.getApplication().getLogger().Debug("Removed event listener.  Total listeners: " + mAudioEventListeners.size());
    }

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

            mVlcHandler.setOnProgressListener(new PlaybackListener() {
                @Override
                public void onEvent() {
                    //Don't need to be too aggressive with these calls - just be sure every second
                    if (System.currentTimeMillis() < lastProgressEvent + 750) return;
                    lastProgressEvent = System.currentTimeMillis();

                    mCurrentAudioPosition = mVlcPlayer.getTime();

                    //Report progress to server every 3 secs
                    if (System.currentTimeMillis() > lastProgressReport + 3000) {
                        Utils.ReportProgress(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition*10000, !mVlcPlayer.isPlaying());
                        lastProgressReport = System.currentTimeMillis();
                        TvApp.getApplication().setLastUserInteraction(lastProgressReport);
                    }

                    //fire external listeners if there
                    for (AudioEventListener listener : mAudioEventListeners) {
                        listener.onProgress(mCurrentAudioPosition);
                    }
                }
            });

            mVlcHandler.setOnCompletionListener(new PlaybackListener() {
                @Override
                public void onEvent() {
                    Utils.ReportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition);
                    nextAudioItem();

                    //fire external listener if there
                    for (AudioEventListener listener : mAudioEventListeners) {
                        TvApp.getApplication().getLogger().Info("Firing playback state change listener for item completion. "+ mCurrentAudioItem.getName());
                        listener.onPlaybackStateChange(PlaybackController.PlaybackState.IDLE, mCurrentAudioItem);
                    }
                }
            });

            mVlcPlayer.setEventListener(mVlcHandler);

        } catch (Exception e) {
            TvApp.getApplication().getLogger().ErrorException("Error creating VLC player", e);
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_video_playback_error));
            return false;
        }

        return true;
    }

    private static AudioManager.OnAudioFocusChangeListener mAudioFocusChanged = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    pauseAudio();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    stopAudio();
                    mAudioManager.unregisterMediaButtonEventReceiver(new ComponentName(TvApp.getApplication().getPackageName(), RemoteControlReceiver.class.getName()));
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    resumeAudio();
                    break;
            }
        }
    };

    private static void createAudioQueue(List<BaseItemDto> items) {
        mCurrentAudioQueue = new ItemRowAdapter(items, new CardPresenter(true, Utils.convertDpToPixel(TvApp.getApplication(), 200)), null, true);
        mCurrentAudioQueue.Retrieve();
    }

    public static int queueAudioItem(int pos, BaseItemDto item) {
        if (mCurrentAudioQueue == null) createAudioQueue(new ArrayList<BaseItemDto>());
        mCurrentAudioQueue.add(new BaseRowItem(pos, item));
        TvApp.getApplication().showMessage(TvApp.getApplication().getString(R.string.msg_added_item_to_queue) + (pos + 1), Utils.GetFullName(item), 4000, R.drawable.audioicon);
        return pos;
    }

    public static int queueAudioItem(BaseItemDto item) {
        if (mCurrentAudioQueue == null) createAudioQueue(new ArrayList<BaseItemDto>());
        mCurrentAudioQueue.add(new BaseRowItem(mCurrentAudioQueue.size(), item));
        return mCurrentAudioQueue.size()-1;
    }

    public static void clearAudioQueue() {
        if (mCurrentAudioQueue == null) createAudioQueue(new ArrayList<BaseItemDto>());
        else mCurrentAudioQueue.clear();
        mCurrentAudioQueuePosition = -1;
    }

    public static void addToAudioQueue(List<BaseItemDto> items) {
        if (mCurrentAudioQueue == null) createAudioQueue(items);
        else {
            int ndx = mCurrentAudioQueue.size();
            for (BaseItemDto item : items) {
                mCurrentAudioQueue.add(new BaseRowItem(ndx++, item));
            }
        }
        TvApp.getApplication().showMessage(items.size() + TvApp.getApplication().getString(R.string.msg_items_added), mCurrentAudioQueue.size() + TvApp.getApplication().getString(R.string.msg_total_items_in_queue), 5000, R.drawable.audioicon);
    }

    public static boolean isPlayingAudio() { return audioInitialized && mVlcPlayer.isPlaying(); }

    private static boolean ensureInitialized() {
        if (!audioInitialized) {
            audioInitialized = initAudio();
        }

        if (!audioInitialized) {
            Utils.showToast(TvApp.getApplication(), "Unable to play audio");
        }

        return audioInitialized;
    }

    public static void playNow(final List<BaseItemDto> items) {
        if (!ensureInitialized()) return;

        createAudioQueue(items);
        mCurrentAudioQueuePosition = -1;
        nextAudioItem();
        if (TvApp.getApplication().getCurrentActivity().getClass() != AudioNowPlayingActivity.class) {
            Intent nowPlaying = new Intent(TvApp.getApplication(), AudioNowPlayingActivity.class);
            TvApp.getApplication().getCurrentActivity().startActivity(nowPlaying);
        } else {
            TvApp.getApplication().showMessage(items.size() + TvApp.getApplication().getString(R.string.msg_items_added), mCurrentAudioQueue.size() + TvApp.getApplication().getString(R.string.msg_total_items_in_queue), 5000, R.drawable.audioicon);

        }
    }

    public static void playNow(final BaseItemDto item) {
        if (!ensureInitialized()) return;

        createAudioQueue(new ArrayList<BaseItemDto>());
        queueAudioItem(item);
        nextAudioItem();
        if (TvApp.getApplication().getCurrentActivity().getClass() != AudioNowPlayingActivity.class) {
            Intent nowPlaying = new Intent(TvApp.getApplication(), AudioNowPlayingActivity.class);
            TvApp.getApplication().getCurrentActivity().startActivity(nowPlaying);
        }
    }

    private static boolean ensureAudioFocus() {
        if (mAudioManager.requestAudioFocus(mAudioFocusChanged, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            TvApp.getApplication().getLogger().Error("Unable to get audio focus");
            Utils.showToast(TvApp.getApplication(), R.string.msg_cannot_play_time);
            return false;
        }

        //Register a media button receiver so that all media button presses will come to us and not another app
        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(TvApp.getApplication().getPackageName(), RemoteControlReceiver.class.getName()));
        //TODO implement conditional logic for api 21+
        return true;
    }

    private static void playInternal(final BaseItemDto item, final int pos) {
        ensureAudioFocus();
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
                mCurrentAudioQueuePosition = pos;
                mCurrentAudioPosition = 0;
                TvApp.getApplication().getLogger().Info("Playback attempt via VLC of "+response.ToUrl(apiClient.getApiUrl(), apiClient.getAccessToken()));
                Media media = new Media(mLibVLC, Uri.parse(response.ToUrl(apiClient.getApiUrl(), apiClient.getAccessToken())));
                media.parse();
                mVlcPlayer.setMedia(media);

                media.release();
                mVlcPlayer.play();
                updateCurrentAudioItemPlaying(true);

                Utils.ReportStart(item, mCurrentAudioPosition*10000);
                for (AudioEventListener listener : mAudioEventListeners) {
                    TvApp.getApplication().getLogger().Info("Firing playback state change listener for item start. "+mCurrentAudioItem.getName());
                    listener.onPlaybackStateChange(PlaybackController.PlaybackState.PLAYING, mCurrentAudioItem);
                }
            }

            @Override
            public void onError(Exception exception) {
                Utils.showToast(TvApp.getApplication(), "Unable to play audio " + exception.getLocalizedMessage());
            }
        });

    }

    public static BaseItemDto getNextAudioItem() {
        if (mCurrentAudioQueue == null || mCurrentAudioQueue.size() == 0 || (!mRepeat && mCurrentAudioQueuePosition == mCurrentAudioQueue.size() - 1)) return null;

        int ndx = mCurrentAudioQueuePosition+1;
        if (ndx >= mCurrentAudioQueue.size()) ndx = 0;
        return ((BaseRowItem)mCurrentAudioQueue.get(ndx)).getBaseItem();
    }

    public static BaseItemDto getPrevAudioItem() {
        if (mCurrentAudioQueue == null || mCurrentAudioQueue.size() == 0 || (!mRepeat && mCurrentAudioQueuePosition == 0)) return null;

        int ndx = mCurrentAudioQueuePosition-1;
        if (ndx < 0) ndx = mCurrentAudioQueue.size() - 1;
        return ((BaseRowItem)mCurrentAudioQueue.get(ndx)).getBaseItem();
    }

    public static boolean hasNextAudioItem() { return mCurrentAudioQueue != null && mCurrentAudioQueue.size() > 0 && (mRepeat || mCurrentAudioQueuePosition < mCurrentAudioQueue.size()-1); }
    public static boolean hasPrevAudioItem() { return mCurrentAudioQueue != null && mCurrentAudioQueue.size() > 0 && (mRepeat || mCurrentAudioQueuePosition > 0); }

    public static void updateCurrentAudioItemPlaying(boolean playing) {
        BaseRowItem rowItem = (BaseRowItem) mCurrentAudioQueue.get(mCurrentAudioQueuePosition);
        if (rowItem != null) {
            rowItem.setIsPlaying(playing);
            mCurrentAudioQueue.notifyArrayItemRangeChanged(mCurrentAudioQueuePosition, 1);
        }
    }

    public static int nextAudioItem() {
        //turn off indicator for current item
        if (mCurrentAudioQueuePosition >= 0) {
            updateCurrentAudioItemPlaying(false);
        }

        if (mCurrentAudioQueue == null || mCurrentAudioQueue.size() == 0 || (!mRepeat && mCurrentAudioQueuePosition == mCurrentAudioQueue.size() - 1)) return -1;
        stopAudio();
        int ndx = mCurrentAudioQueuePosition +1;
        if (ndx >= mCurrentAudioQueue.size()) ndx = 0;
        playInternal(getNextAudioItem(), ndx);
        return ndx;
    }

    public static int prevAudioItem() {
        if (mCurrentAudioQueue == null || (!mRepeat && mCurrentAudioQueue.size() == 0)) return -1;
        if (isPlayingAudio() && mCurrentAudioPosition > 10000) {
            //just back up to the beginning of current item
            mVlcPlayer.setTime(0);
            return mCurrentAudioQueuePosition;
        }

        if ( !mRepeat && mCurrentAudioQueuePosition < 1) {
            //nowhere to go
            return mCurrentAudioQueuePosition;
        }


        stopAudio();
        int ndx = mCurrentAudioQueuePosition - 1;
        if (ndx < 0) ndx = mCurrentAudioQueue.size() - 1;
        playInternal(getPrevAudioItem(), ndx);
        return ndx;
    }

    public static void stopAudio() {
        if (mCurrentAudioItem != null && isPlayingAudio()) {
            mVlcPlayer.stop();
            Utils.ReportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition*10000);
            for (AudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.IDLE, mCurrentAudioItem);
            }

        }
    }

    public static void pauseAudio() {
        if (mCurrentAudioItem != null && isPlayingAudio()) {
            updateCurrentAudioItemPlaying(false);
            mVlcPlayer.pause();
            Utils.ReportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition*10000);
            for (AudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.PAUSED, mCurrentAudioItem);
            }
            lastProgressReport = System.currentTimeMillis();

        }
    }

    public static void resumeAudio() {
        if (mCurrentAudioItem != null && mVlcPlayer != null) {
            mVlcPlayer.play();
            updateCurrentAudioItemPlaying(true);
            Utils.ReportStart(mCurrentAudioItem, mCurrentAudioPosition * 10000);
            for (AudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.PLAYING, mCurrentAudioItem);
            }
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
