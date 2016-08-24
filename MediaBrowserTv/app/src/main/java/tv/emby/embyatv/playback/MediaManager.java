package tv.emby.embyatv.playback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.InputType;
import android.widget.EditText;

import com.devbrackets.android.exomedia.EMAudioPlayer;
import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;
import com.devbrackets.android.exomedia.listener.EMProgressCallback;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.profiles.AndroidProfile;
import mediabrowser.model.dlna.AudioOptions;
import mediabrowser.model.dlna.DeviceProfile;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.playlists.PlaylistCreationRequest;
import mediabrowser.model.playlists.PlaylistCreationResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.CustomMessage;
import tv.emby.embyatv.itemhandling.AudioQueueItem;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.util.ProfileHelper;
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
    private static ItemRowAdapter mManagedAudioQueue;
    private static int mCurrentAudioQueuePosition = -1;
    private static BaseItemDto mCurrentAudioItem;
    private static StreamInfo mCurrentAudioStreamInfo;
    private static long mCurrentAudioPosition;

    private static LibVLC mLibVLC;
    private static org.videolan.libvlc.MediaPlayer mVlcPlayer;
    private static VlcEventHandler mVlcHandler = new VlcEventHandler();
    private static EMAudioPlayer mExoplayer;
    private static AudioManager mAudioManager;
    private static boolean audioInitialized;
    private static boolean nativeMode = false;
    private static boolean videoQueueModified = false;

    private static List<AudioEventListener> mAudioEventListeners = new ArrayList<>();

    private static long lastProgressReport;
    private static long lastProgressEvent;

    private static boolean mRepeat;

    private static List<BaseItemDto> mCurrentVideoQueue;

    public static ItemRowAdapter getCurrentMediaAdapter() {
        return mCurrentMediaAdapter;
    }
    public static boolean hasAudioQueueItems() { return mCurrentAudioQueue != null && mCurrentAudioQueue.size() > 0; }
    public static boolean hasVideoQueueItems() { return mCurrentVideoQueue != null && mCurrentVideoQueue.size() > 0; }

    public static void setCurrentMediaAdapter(ItemRowAdapter currentMediaAdapter) {
        MediaManager.mCurrentMediaAdapter = currentMediaAdapter;
    }

    public static int getCurrentMediaPosition() {
        return mCurrentMediaPosition;
    }

    public static void setCurrentVideoQueue(List<BaseItemDto> items) { mCurrentVideoQueue = items; }
    public static List<BaseItemDto> getCurrentVideoQueue() { return mCurrentVideoQueue; }

    public static int getCurrentAudioQueueSize() { return mCurrentAudioQueue != null ? mCurrentAudioQueue.size() : 0; }
    public static int getCurrentAudioQueuePosition() { return mCurrentAudioQueuePosition; }
    public static long getCurrentAudioPosition() { return mCurrentAudioPosition; }
    public static String getCurrentAudioQueueDisplayPosition() { return Integer.toString(mCurrentAudioQueuePosition >=0 ? mCurrentAudioQueuePosition+1 : 1); }
    public static String getCurrentAudioQueueDisplaySize() { return mCurrentAudioQueue != null ? Integer.toString(mCurrentAudioQueue.size()) : "0"; }

    public static BaseItemDto getCurrentAudioItem() { return mCurrentAudioItem != null ? mCurrentAudioItem : hasAudioQueueItems() ? ((BaseRowItem)mCurrentAudioQueue.get(0)).getBaseItem() : null; }

    public static boolean toggleRepeat() { mRepeat = !mRepeat; return mRepeat; }
    public static boolean isRepeatMode() { return mRepeat; }

    public static ItemRowAdapter getCurrentAudioQueue() { return mCurrentAudioQueue; }
    public static ItemRowAdapter getManagedAudioQueue() {
        createManagedAudioQueue();
        return mManagedAudioQueue;
    }

    public static void createManagedAudioQueue() {
        if (mCurrentAudioQueue != null) {
            if (mManagedAudioQueue != null) {
                //re-create existing one
                mManagedAudioQueue.clear();
                for (int i = mCurrentAudioQueuePosition >= 0 ? mCurrentAudioQueuePosition : 0; i < mCurrentAudioQueue.size(); i++) {
                    mManagedAudioQueue.add(mCurrentAudioQueue.get(i));
                }
            } else {
                List<BaseItemDto> managedItems = new ArrayList<>();
                for (int i = mCurrentAudioQueuePosition >= 0 ? mCurrentAudioQueuePosition : 0; i < mCurrentAudioQueue.size(); i++) {
                    managedItems.add(((BaseRowItem)mCurrentAudioQueue.get(i)).getBaseItem());
                }
                mManagedAudioQueue = new ItemRowAdapter(managedItems, new CardPresenter(true, Utils.convertDpToPixel(TvApp.getApplication(), 150)), null, QueryType.StaticAudioQueueItems);
                mManagedAudioQueue.Retrieve();
            }
            if (isPlayingAudio()) {
                ((BaseRowItem)mManagedAudioQueue.get(0)).setIsPlaying(true);
            }
        }

    }

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

    private static boolean isPaused() {
        return nativeMode ? !mExoplayer.isPlaying() : !mVlcPlayer.isPlaying();
    }

    private static void reportProgress() {
        //Don't need to be too aggressive with these calls - just be sure every second
        if (System.currentTimeMillis() < lastProgressEvent + 750) return;
        lastProgressEvent = System.currentTimeMillis();

        mCurrentAudioPosition = nativeMode ? mExoplayer.getCurrentPosition() : mVlcPlayer.getTime();

        //fire external listeners if there
        for (AudioEventListener listener : mAudioEventListeners) {
            listener.onProgress(mCurrentAudioPosition);
        }

        //Report progress to server every 5 secs
        if (System.currentTimeMillis() > lastProgressReport + 5000) {
            Utils.ReportProgress(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition*10000, isPaused());
            lastProgressReport = System.currentTimeMillis();
            TvApp.getApplication().setLastUserInteraction(lastProgressReport);
        }

    }

    private static void onComplete() {
        Utils.ReportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition);
        nextAudioItem();

        //fire external listener if there
        for (AudioEventListener listener : mAudioEventListeners) {
            TvApp.getApplication().getLogger().Info("Firing playback state change listener for item completion. "+ mCurrentAudioItem.getName());
            listener.onPlaybackStateChange(PlaybackController.PlaybackState.IDLE, mCurrentAudioItem);
        }

    }

    private static boolean createPlayer(int buffer) {
        try {

            // Create a new media player based on platform
            if (Utils.is60()) {
                nativeMode = true;
                mExoplayer = new EMAudioPlayer(TvApp.getApplication());
                mExoplayer.setProgressCallback(new EMProgressCallback() {
                    @Override
                    public boolean onProgressUpdated(EMMediaProgressEvent progressEvent) {
                        reportProgress();
                        return false;
                    }
                });

                mExoplayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        onComplete();
                    }
                });
            } else {
                ArrayList<String> options = new ArrayList<>(20);
                options.add("--network-caching=" + buffer);
                options.add("--no-audio-time-stretch");
                options.add("-v");

                mLibVLC = new LibVLC(TvApp.getApplication(), options);
                LibVLC.setOnNativeCrashListener(new LibVLC.OnNativeCrashListener() {
                    @Override
                    public void onNativeCrash() {
                        new Exception().printStackTrace();
                        //todo put our custom log reporter here...
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(10);

                    }
                });

                mVlcPlayer = new org.videolan.libvlc.MediaPlayer(mLibVLC);
                mVlcPlayer.setAudioOutput(Utils.downMixAudio() ? "opensles_android" : "android_audiotrack");
                mVlcPlayer.setAudioOutputDevice("hdmi");

                mVlcHandler.setOnProgressListener(new PlaybackListener() {
                    @Override
                    public void onEvent() {
                        reportProgress();
                    }
                });

                mVlcHandler.setOnCompletionListener(new PlaybackListener() {
                    @Override
                    public void onEvent() {
                        onComplete();
                    }
                });

                mVlcPlayer.setEventListener(mVlcHandler);

            }

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
                    //resumeAudio();
                    break;
            }
        }
    };

    private static void fireQueueStatusChange() {
        for (AudioEventListener listener : mAudioEventListeners) {
            TvApp.getApplication().getLogger().Info("Firing queue state change listener. "+ hasAudioQueueItems());
            listener.onQueueStatusChanged(hasAudioQueueItems());
        }

    }

    private static void createAudioQueue(List<BaseItemDto> items) {
        mCurrentAudioQueue = new ItemRowAdapter(items, new CardPresenter(true, Utils.convertDpToPixel(TvApp.getApplication(), 140)), null, QueryType.StaticAudioQueueItems);
        mCurrentAudioQueue.Retrieve();
        mManagedAudioQueue = null;
        fireQueueStatusChange();
    }

    private static int TYPE_AUDIO = 0;
    private static int TYPE_VIDEO = 1;

    public static void saveAudioQueue(Activity activity) {
        saveQueue(activity, TYPE_AUDIO);
    }

    public static void saveVideoQueue(Activity activity) {
        saveQueue(activity, TYPE_VIDEO);
    }

    public static void saveQueue(Activity activity, final int type) {
        //Get a name and save as playlist
        final EditText name = new EditText(activity);
        name.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.lbl_save_as_playlist)
                .setMessage("Enter a name for the new playlist")
                .setView(name)
                .setPositiveButton(R.string.btn_done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String text = name.getText().toString();
                        PlaylistCreationRequest request = new PlaylistCreationRequest();
                        request.setUserId(TvApp.getApplication().getCurrentUser().getId());
                        request.setMediaType(type == TYPE_AUDIO ? "Audio" : "Video");
                        request.setName(text);
                        request.setItemIdList(type == TYPE_AUDIO ? getCurrentAudioQueueItemIds() : getCurrentVideoQueueItemIds());
                        TvApp.getApplication().getApiClient().CreatePlaylist(request, new Response<PlaylistCreationResult>() {
                            @Override
                            public void onResponse(PlaylistCreationResult response) {
                                TvApp.getApplication().showMessage("Playlist Saved", "Queue saved as new playlist: "+text);
                                TvApp.getApplication().setLastLibraryChange(Calendar.getInstance());
                            }

                            @Override
                            public void onError(Exception exception) {
                                TvApp.getApplication().getLogger().Debug(exception.toString());
                            }
                        });
                    }
                })
                .show();

    }

    private static ArrayList<String> getCurrentAudioQueueItemIds() {
        ArrayList<String> result = new ArrayList<>();

        if (mCurrentAudioQueue != null) {
            for (int i = 0; i < mCurrentAudioQueue.size(); i++) {
                AudioQueueItem item = (AudioQueueItem) mCurrentAudioQueue.get(i);
                result.add(item.getItemId());
            }
        }

        return result;
    }

    private static ArrayList<String> getCurrentVideoQueueItemIds() {
        ArrayList<String> result = new ArrayList<>();

        if (mCurrentVideoQueue != null) {
            for (int i = 0; i < mCurrentVideoQueue.size(); i++) {
                result.add(mCurrentVideoQueue.get(i).getId());
            }
        }

        return result;
    }

    public static int queueAudioItem(int pos, BaseItemDto item) {
        if (mCurrentAudioQueue == null) createAudioQueue(new ArrayList<BaseItemDto>());
        mCurrentAudioQueue.add(new BaseRowItem(pos, item));
        TvApp.getApplication().showMessage(TvApp.getApplication().getString(R.string.msg_added_item_to_queue) + (pos + 1), Utils.GetFullName(item), 4000, R.drawable.audioicon);
        return pos;
    }

    public static int queueAudioItem(BaseItemDto item) {
        if (mCurrentAudioQueue == null) createAudioQueue(new ArrayList<BaseItemDto>());
        mCurrentAudioQueue.add(new AudioQueueItem(mCurrentAudioQueue.size(), item));
        return mCurrentAudioQueue.size()-1;
    }

    public static int addToVideoQueue(BaseItemDto item) {
        if (mCurrentVideoQueue == null) mCurrentVideoQueue = new ArrayList<>();
        mCurrentVideoQueue.add(item);
        videoQueueModified = true;
        TvApp.getApplication().setLastVideoQueueChange(System.currentTimeMillis());
        if (mCurrentVideoQueue.size() == 1 && TvApp.getApplication().getCurrentActivity() != null) {
            TvApp.getApplication().getCurrentActivity().sendMessage(CustomMessage.RefreshRows);
        }
        long total = System.currentTimeMillis();
        for (BaseItemDto video :
                mCurrentVideoQueue) {
            total += video.getRunTimeTicks() / 10000;
        }

        Utils.showToast(TvApp.getApplication(), item.getName() + " added to video queue. Ends: "+android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(new Date(total)));
        return mCurrentVideoQueue.size()-1;
    }

    public static void clearAudioQueue() {
        if (mCurrentAudioQueue == null) {
            createAudioQueue(new ArrayList<BaseItemDto>());
        }
        else {
            mCurrentAudioQueue.clear();
            fireQueueStatusChange();
        }
        mCurrentAudioQueuePosition = -1;
        if (mManagedAudioQueue != null) mManagedAudioQueue.clear();
    }

    public static void addToAudioQueue(List<BaseItemDto> items) {
        if (mCurrentAudioQueue == null) createAudioQueue(items);
        else {
            int ndx = mCurrentAudioQueue.size();
            for (BaseItemDto item : items) {
                AudioQueueItem queueItem = new AudioQueueItem(ndx++, item);
                mCurrentAudioQueue.add(queueItem);
                if (mManagedAudioQueue != null) mManagedAudioQueue.add(queueItem);
            }
            fireQueueStatusChange();
        }
        TvApp.getApplication().showMessage(items.size() + (items.size() > 1 ? TvApp.getApplication().getString(R.string.msg_items_added) : TvApp.getApplication().getString(R.string.msg_item_added)), mCurrentAudioQueue.size() + TvApp.getApplication().getString(R.string.msg_total_items_in_queue), 5000, R.drawable.audioicon);
    }

    public static void removeFromAudioQueue(int ndx) {
        if (mCurrentAudioQueuePosition == ndx) {
            // current item - stop audio, remove and re-start
            stopAudio();
            if (mManagedAudioQueue != null) {
                mManagedAudioQueue.remove(mCurrentAudioQueue.get(ndx));
            }
            mCurrentAudioQueue.removeItems(ndx, 1);
            mCurrentAudioQueuePosition--;
            mCurrentAudioPosition = 0;
            if (ndx >= 0 && ndx < mCurrentAudioQueue.size()) {
                nextAudioItem();
            } else {
                if (mCurrentAudioQueuePosition >= 0) mCurrentAudioItem = ((BaseRowItem)mCurrentAudioQueue.get(mCurrentAudioQueuePosition)).getBaseItem();
                // fire a change to update current item
                fireQueueStatusChange();
            }
        } else {
            //just remove it
            mCurrentAudioQueue.removeItems(ndx, 1);
            if (mManagedAudioQueue != null) {
                mManagedAudioQueue.remove(mManagedAudioQueue.findByIndex(ndx));
            }
        }

        // now need to update indexes for subsequent items
        if (hasAudioQueueItems()) {
            for (int i = ndx; i < mCurrentAudioQueue.size(); i++){
                ((BaseRowItem)mCurrentAudioQueue.get(i)).setIndex(i);
            }
        }
    }

    public static boolean isPlayingAudio() { return audioInitialized && (nativeMode ? mExoplayer.isPlaying() : mVlcPlayer.isPlaying()); }

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
                    .setTitle(R.string.lbl_items_in_queue)
                    .setMessage(R.string.msg_replace_or_add_queue_q)
                    .setPositiveButton(R.string.btn_replace_queue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            playNowInternal(items);
                        }
                    })
                    .setNeutralButton(R.string.lbl_add_to_queue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            addToAudioQueue(items);
                        }
                    })
                    .setNegativeButton(R.string.lbl_cancel, null)
                    .show();
        } else {
            playNowInternal(items);
        }

    }

    private static void playNowInternal(List<BaseItemDto> items) {
        createAudioQueue(items);
        mCurrentAudioQueuePosition = -1;
        nextAudioItem();
        if (TvApp.getApplication().getCurrentActivity().getClass() != AudioNowPlayingActivity.class) {
            Intent nowPlaying = new Intent(TvApp.getApplication(), AudioNowPlayingActivity.class);
            TvApp.getApplication().getCurrentActivity().startActivity(nowPlaying);
        } else {
            TvApp.getApplication().showMessage(items.size() + (items.size() > 1 ? TvApp.getApplication().getString(R.string.msg_items_added) : TvApp.getApplication().getString(R.string.msg_item_added)), mCurrentAudioQueue.size() + TvApp.getApplication().getString(R.string.msg_total_items_in_queue), 5000, R.drawable.audioicon);
        }

    }

    public static void playNow(final BaseItemDto item) {
        if (!ensureInitialized()) return;

        List<BaseItemDto> list = new ArrayList<BaseItemDto>();
        list.add(item);
        playNow(list);
    }

    public static boolean playFrom(int ndx) {
        if (ndx >= mCurrentAudioQueue.size()) return false;

        if (isPlayingAudio()) stopAudio();

        mCurrentAudioQueuePosition = ndx-1;
        createManagedAudioQueue();
        nextAudioItem();
        return true;
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
        if (!ensureInitialized()) return;
        ensureAudioFocus();
        final ApiClient apiClient = TvApp.getApplication().getApiClient();
        AudioOptions options = new AudioOptions();
        options.setDeviceId(apiClient.getDeviceId());
        options.setItemId(item.getId());
        options.setMaxBitrate(TvApp.getApplication().getAutoBitrate());
        options.setMediaSources(item.getMediaSources());
        DeviceProfile profile = ProfileHelper.getBaseProfile();
        if (Utils.is60()) {
            ProfileHelper.setExoOptions(profile, false, true);
        } else {
            ProfileHelper.setVlcOptions(profile, false);
        }
        options.setProfile(profile);
        TvApp.getApplication().getPlaybackManager().getAudioStreamInfo(apiClient.getServerInfo().getId(), options, item.getResumePositionTicks(), false, apiClient, new Response<StreamInfo>() {
            @Override
            public void onResponse(StreamInfo response) {
                mCurrentAudioItem = item;
                mCurrentAudioStreamInfo = response;
                mCurrentAudioQueuePosition = pos;
                mCurrentAudioPosition = 0;
                if (nativeMode) {
                    mExoplayer.setDataSource(TvApp.getApplication(), Uri.parse(response.ToUrl(apiClient.getApiUrl(), apiClient.getAccessToken())));
                    mExoplayer.start();
                } else {
                    TvApp.getApplication().getLogger().Info("Playback attempt via VLC of " + response.getMediaUrl());
                    Media media = new Media(mLibVLC, Uri.parse(response.getMediaUrl()));
                    media.parse();
                    mVlcPlayer.setMedia(media);

                    media.release();
                    mVlcPlayer.play();

                }
                if (mCurrentAudioQueuePosition == 0) {
                    //we just started or repeated - re-create managed queue
                    createManagedAudioQueue();
                }

                updateCurrentAudioItemPlaying(true);
                TvApp.getApplication().setLastMusicPlayback(System.currentTimeMillis());

                Utils.ReportStart(item, mCurrentAudioPosition * 10000);
                for (AudioEventListener listener : mAudioEventListeners) {
                    TvApp.getApplication().getLogger().Info("Firing playback state change listener for item start. " + mCurrentAudioItem.getName());
                    listener.onPlaybackStateChange(PlaybackController.PlaybackState.PLAYING, mCurrentAudioItem);
                }
            }

            @Override
            public void onError(Exception exception) {
                Utils.showToast(TvApp.getApplication(), "Unable to play audio " + exception.getLocalizedMessage());
            }
        });

    }

    public static void shuffleAudioQueue() {
        if (!hasAudioQueueItems()) return;

        List<BaseItemDto> items = new ArrayList<>();
        for(int i = 0; i < mCurrentAudioQueue.size(); i++) {
            items.add(((BaseRowItem) mCurrentAudioQueue.get(i)).getBaseItem());
        }

        Collections.shuffle(items);
        playNow(items);

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
        if (mCurrentAudioQueuePosition < 0) return;
        BaseRowItem rowItem = (BaseRowItem) mCurrentAudioQueue.get(mCurrentAudioQueuePosition);
        if (rowItem != null) {
            rowItem.setIsPlaying(playing);
            mCurrentAudioQueue.notifyArrayItemRangeChanged(mCurrentAudioQueuePosition, 1);
            if (mManagedAudioQueue != null && mManagedAudioQueue.size() > 0) {
                BaseRowItem managedItem = (BaseRowItem) mManagedAudioQueue.get(0);
                managedItem.setIsPlaying(playing);
                mManagedAudioQueue.notifyArrayItemRangeChanged(0, 1);
            }
        }
    }

    public static int nextAudioItem() {
        //turn off indicator for current item
        if (mCurrentAudioQueuePosition >= 0) {
            updateCurrentAudioItemPlaying(false);
        }

        if (mCurrentAudioQueue == null || mCurrentAudioQueue.size() == 0 || (!mRepeat && mCurrentAudioQueuePosition == mCurrentAudioQueue.size() - 1)) return -1;
        stopAudio();
        if (mManagedAudioQueue != null && mManagedAudioQueue.size() > 1) {
            //don't remove last item as it causes framework crashes
            mManagedAudioQueue.removeItems(0, 1);
        }
        int ndx = mCurrentAudioQueuePosition +1;
        if (ndx >= mCurrentAudioQueue.size()) ndx = 0;
        playInternal(getNextAudioItem(), ndx);
        return ndx;
    }

    public static int prevAudioItem() {
        if (mCurrentAudioQueue == null || (!mRepeat && mCurrentAudioQueue.size() == 0)) return -1;
        if (isPlayingAudio() && mCurrentAudioPosition > 10000) {
            //just back up to the beginning of current item
            if (nativeMode) mExoplayer.seekTo(0);
            else mVlcPlayer.setTime(0);
            return mCurrentAudioQueuePosition;
        }

        if ( !mRepeat && mCurrentAudioQueuePosition < 1) {
            //nowhere to go
            return mCurrentAudioQueuePosition;
        }


        stopAudio();
        int ndx = mCurrentAudioQueuePosition - 1;
        if (mManagedAudioQueue != null) {
            mManagedAudioQueue.add(0, mCurrentAudioQueue.get(ndx));
        }
        if (ndx < 0) ndx = mCurrentAudioQueue.size() - 1;
        playInternal(getPrevAudioItem(), ndx);
        return ndx;
    }

    private static void stop() {
        if (nativeMode) mExoplayer.stopPlayback();
        else mVlcPlayer.stop();
    }

    public static void stopAudio() {
        if (mCurrentAudioItem != null && isPlayingAudio()) {
            stop();
            updateCurrentAudioItemPlaying(false);
            Utils.ReportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition*10000);
            for (AudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.IDLE, mCurrentAudioItem);
            }
            //UnRegister a media button receiver
            mAudioManager.unregisterMediaButtonEventReceiver(new ComponentName(TvApp.getApplication().getPackageName(), RemoteControlReceiver.class.getName()));

        }
    }

    private static void pause() {
        if (nativeMode) mExoplayer.pause();
        else mVlcPlayer.pause();
    }

    public static void pauseAudio() {
        if (mCurrentAudioItem != null && isPlayingAudio()) {
            updateCurrentAudioItemPlaying(false);
            pause();
            Utils.ReportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition * 10000);
            for (AudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.PAUSED, mCurrentAudioItem);
            }
            //UnRegister a media button receiver
            mAudioManager.unregisterMediaButtonEventReceiver(new ComponentName(TvApp.getApplication().getPackageName(), RemoteControlReceiver.class.getName()));
            lastProgressReport = System.currentTimeMillis();

        }
    }

    public static void resumeAudio() {
        if (mCurrentAudioItem != null) {
            ensureAudioFocus();
            if (nativeMode) mExoplayer.start();
            else mVlcPlayer.play();
            updateCurrentAudioItemPlaying(true);
            Utils.ReportStart(mCurrentAudioItem, mCurrentAudioPosition * 10000);
            for (AudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.PLAYING, mCurrentAudioItem);
            }
        } else if (hasAudioQueueItems()) {
            //play from start
            playInternal(((BaseRowItem)mCurrentAudioQueue.get(0)).getBaseItem(), 0);
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

    public static boolean isVideoQueueModified() {
        return videoQueueModified;
    }

    public static void setVideoQueueModified(boolean videoQueueModified) {
        MediaManager.videoQueueModified = videoQueueModified;
    }

    public static void clearVideoQueue() {
        mCurrentVideoQueue = new ArrayList<>();
        videoQueueModified = false;
    }
}
