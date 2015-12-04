package tv.emby.embyatv.playback;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;

import org.acra.ACRA;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.util.ArrayList;
import java.util.Arrays;
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
import tv.emby.embyatv.util.RemoteControlReceiver;
import tv.emby.embyatv.util.Utils;
import tv.emby.iap.IabValidator;

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

    private static List<IAudioEventListener> mAudioEventListeners = new ArrayList<>();

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
    public static String getCurrentAudioQueueDisplayPosition() { return Integer.toString(mCurrentAudioQueuePosition+1); }
    public static String getCurrentAudioQueueDisplaySize() { return mCurrentAudioQueue != null ? Integer.toString(mCurrentAudioQueue.size()) : "0"; }

    public static BaseItemDto getCurrentAudioItem() { return mCurrentAudioItem; }

    public static boolean toggleRepeat() { mRepeat = !mRepeat; return mRepeat; }
    public static boolean isRepeatMode() { return mRepeat; }

    public static void addAudioEventListener(IAudioEventListener listener) {
        mAudioEventListeners.add(listener);
        TvApp.getApplication().getLogger().Debug("Added event listener.  Total listeners: "+mAudioEventListeners.size());
    }
    public static void removeAudioEventListener(IAudioEventListener listener) {
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
                    for (IAudioEventListener listener : mAudioEventListeners) {
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
                    for (IAudioEventListener listener : mAudioEventListeners) {
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

    public static int queueAudioItem(int pos, BaseItemDto item) {
        if (mCurrentAudioQueue == null) mCurrentAudioQueue = new ArrayList<>();
        mCurrentAudioQueue.add(pos, item);
        TvApp.getApplication().showMessage(TvApp.getApplication().getString(R.string.msg_added_item_to_queue)+(pos+1), Utils.GetFullName(item), 4000, R.drawable.audioicon);
        return pos;
    }

    public static int queueAudioItem(BaseItemDto item) {
        if (mCurrentAudioQueue == null) mCurrentAudioQueue = new ArrayList<>();
        return queueAudioItem(mCurrentAudioQueue.size(), item);
    }

    public static void clearAudioQueue() {
        if (mCurrentAudioQueue == null) mCurrentAudioQueue = new ArrayList<>();
        else mCurrentAudioQueue.clear();
        mCurrentAudioQueuePosition = -1;
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

        if (hasAudioQueueItems()) {
            new AlertDialog.Builder(TvApp.getApplication().getCurrentActivity())
                    .setTitle(TvApp.getApplication().getString(R.string.lbl_play))
                    .setMessage(R.string.msg_how_like_play)
                    .setPositiveButton(R.string.lbl_replace_queue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            clearAudioQueue();
                            mCurrentAudioQueue.addAll(items);
                            mCurrentAudioQueuePosition = -1;
                            nextAudioItem();
                            if (TvApp.getApplication().getCurrentActivity().getClass() != AudioNowPlayingActivity.class) {
                                Intent nowPlaying = new Intent(TvApp.getApplication(), AudioNowPlayingActivity.class);
                                TvApp.getApplication().getCurrentActivity().startActivity(nowPlaying);
                            } else {
                                TvApp.getApplication().showMessage(items.size() + TvApp.getApplication().getString(R.string.msg_items_added), mCurrentAudioQueue.size() + TvApp.getApplication().getString(R.string.msg_total_items_in_queue), 5000, R.drawable.audioicon);

                            }
                        }
                    })
                    .setNeutralButton(R.string.msg_add_to_queue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mCurrentAudioQueue.addAll(mCurrentAudioQueue.size(), items);
                            TvApp.getApplication().showMessage(items.size() + TvApp.getApplication().getString(R.string.msg_items_added), mCurrentAudioQueue.size() + TvApp.getApplication().getString(R.string.msg_total_items_in_queue), 5000, R.drawable.audioicon);
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();

        } else {
            clearAudioQueue();
            mCurrentAudioQueue.addAll(items);
            mCurrentAudioQueuePosition = -1;
            nextAudioItem();
            if (TvApp.getApplication().getCurrentActivity().getClass() != AudioNowPlayingActivity.class) {
                Intent nowPlaying = new Intent(TvApp.getApplication(), AudioNowPlayingActivity.class);
                TvApp.getApplication().getCurrentActivity().startActivity(nowPlaying);
            } else {
                TvApp.getApplication().showMessage(items.size() + TvApp.getApplication().getString(R.string.msg_items_added), mCurrentAudioQueue.size() + TvApp.getApplication().getString(R.string.msg_total_items_in_queue), 5000, R.drawable.audioicon);

            }
        }
    }

    public static void playNow(final BaseItemDto item) {
        if (!ensureInitialized()) return;

        if (isPlayingAudio() && TvApp.getApplication().getCurrentActivity() != null) {
            new AlertDialog.Builder(TvApp.getApplication().getCurrentActivity())
                    .setTitle(TvApp.getApplication().getString(R.string.lbl_play))
                    .setMessage(R.string.msg_how_like_play_single)
                    .setPositiveButton(R.string.lbl_right_now, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            queueAudioItem(mCurrentAudioQueuePosition + 1, item);
                            nextAudioItem();
                        }
                    })
                    .setNeutralButton(R.string.btn_next, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            queueAudioItem(mCurrentAudioQueuePosition + 1, item);
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        } else {
            if (hasAudioQueueItems() && TvApp.getApplication().getCurrentActivity() != null) {
                new AlertDialog.Builder(TvApp.getApplication().getCurrentActivity())
                        .setTitle(TvApp.getApplication().getString(R.string.lbl_play))
                        .setMessage(R.string.msg_audio_queue_has_items)
                        .setPositiveButton(R.string.lbl_replace_queue, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                clearAudioQueue();
                                queueAudioItem(0, item);
                                nextAudioItem();
                                if (TvApp.getApplication().getCurrentActivity().getClass() != AudioNowPlayingActivity.class) {
                                    Intent nowPlaying = new Intent(TvApp.getApplication(), AudioNowPlayingActivity.class);
                                    TvApp.getApplication().getCurrentActivity().startActivity(nowPlaying);
                                }
                            }
                        })
                        .setNeutralButton(R.string.lbl_add_to_queue, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                queueAudioItem(mCurrentAudioQueuePosition + 1, item);
                                nextAudioItem();
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();

            } else {
                clearAudioQueue();
                queueAudioItem(0, item);
                nextAudioItem();
                if (TvApp.getApplication().getCurrentActivity().getClass() != AudioNowPlayingActivity.class) {
                    Intent nowPlaying = new Intent(TvApp.getApplication(), AudioNowPlayingActivity.class);
                    TvApp.getApplication().getCurrentActivity().startActivity(nowPlaying);
                }

            }
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
                TvApp.getApplication().getLogger().Info("Playback attempt via VLC of "+response.ToUrl(apiClient.getApiUrl(), apiClient.getAccessToken()));
                Media media = new Media(mLibVLC, Uri.parse(response.ToUrl(apiClient.getApiUrl(), apiClient.getAccessToken())));
                mCurrentAudioQueuePosition = pos;
                mCurrentAudioPosition = 0;
                media.parse();
                mVlcPlayer.setMedia(media);

                media.release();
                mVlcPlayer.play();

                Utils.ReportStart(item, mCurrentAudioPosition);
                for (IAudioEventListener listener : mAudioEventListeners) {
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
        return mCurrentAudioQueue.get(ndx);
    }

    public static BaseItemDto getPrevAudioItem() {
        if (mCurrentAudioQueue == null || mCurrentAudioQueue.size() == 0 || (!mRepeat && mCurrentAudioQueuePosition == 0)) return null;

        int ndx = mCurrentAudioQueuePosition-1;
        if (ndx < 0) ndx = mCurrentAudioQueue.size() - 1;
        return mCurrentAudioQueue.get(ndx);
    }

    public static boolean hasNextAudioItem() { return mCurrentAudioQueue != null && mCurrentAudioQueue.size() > 0 && (mRepeat || mCurrentAudioQueuePosition < mCurrentAudioQueue.size()-1); }
    public static boolean hasPrevAudioItem() { return mCurrentAudioQueue != null && mCurrentAudioQueue.size() > 0 && (mRepeat || mCurrentAudioQueuePosition > 0); }

    public static int nextAudioItem() {
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
            Utils.ReportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition);
            for (IAudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.IDLE, mCurrentAudioItem);
            }

        }
    }

    public static void pauseAudio() {
        if (mCurrentAudioItem != null && isPlayingAudio()) {
            mVlcPlayer.pause();
            Utils.ReportProgress(mCurrentAudioItem, mCurrentAudioStreamInfo, mVlcPlayer.getTime()*10000, !mVlcPlayer.isPlaying());
            for (IAudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.PAUSED, mCurrentAudioItem);
            }
            lastProgressReport = System.currentTimeMillis();

        }
    }

    public static void resumeAudio() {
        if (mCurrentAudioItem != null && mVlcPlayer != null) {
            mVlcPlayer.play();
            for (IAudioEventListener listener : mAudioEventListeners) {
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
