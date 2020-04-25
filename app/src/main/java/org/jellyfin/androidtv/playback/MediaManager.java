package org.jellyfin.androidtv.playback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.CustomMessage;
import org.jellyfin.androidtv.itemhandling.AudioQueueItem;
import org.jellyfin.androidtv.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.model.compat.AudioOptions;
import org.jellyfin.androidtv.model.compat.StreamInfo;
import org.jellyfin.androidtv.presentation.CardPresenter;
import org.jellyfin.androidtv.querying.QueryType;
import org.jellyfin.androidtv.util.DeviceUtils;
import org.jellyfin.androidtv.util.ProfileHelper;
import org.jellyfin.androidtv.util.RemoteControlReceiver;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.ReportingHelper;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.playlists.PlaylistCreationRequest;
import org.jellyfin.apiclient.model.playlists.PlaylistCreationResult;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
    private static SimpleExoPlayer mExoPlayer;
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
        TvApp.getApplication().getLogger().Debug("Added event listener.  Total listeners: %d", mAudioEventListeners.size());
    }
    public static void removeAudioEventListener(AudioEventListener listener) {
        mAudioEventListeners.remove(listener);
        TvApp.getApplication().getLogger().Debug("Removed event listener.  Total listeners: %d", mAudioEventListeners.size());
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
        return nativeMode ? !mExoPlayer.isPlaying() : !mVlcPlayer.isPlaying();
    }

    private static void reportProgress() {
        //Don't need to be too aggressive with these calls - just be sure every second
        if (System.currentTimeMillis() < lastProgressEvent + 750) return;
        lastProgressEvent = System.currentTimeMillis();

        mCurrentAudioPosition = nativeMode ? mExoPlayer.getCurrentPosition() : mVlcPlayer.getTime();

        //fire external listeners if there
        for (AudioEventListener listener : mAudioEventListeners) {
            listener.onProgress(mCurrentAudioPosition);
        }

        //Report progress to server every 5 secs
        if (System.currentTimeMillis() > lastProgressReport + 5000) {
            ReportingHelper.reportProgress(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition*10000, isPaused());
            lastProgressReport = System.currentTimeMillis();
        }

    }

    private static void onComplete() {
        ReportingHelper.reportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition);
        nextAudioItem();

        //fire external listener if there
        for (AudioEventListener listener : mAudioEventListeners) {
            TvApp.getApplication().getLogger().Info("Firing playback state change listener for item completion. %s", mCurrentAudioItem.getName());
            listener.onPlaybackStateChange(PlaybackController.PlaybackState.IDLE, mCurrentAudioItem);
        }

    }

    private static boolean createPlayer(int buffer) {
        try {

            // Create a new media player based on platform
            if (DeviceUtils.is60()) {
                nativeMode = true;
                mExoPlayer = new SimpleExoPlayer.Builder(TvApp.getApplication()).build();
                mExoPlayer.addListener(new Player.EventListener() {
                    @Override
                    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                        if (playbackState == Player.STATE_READY) {
                            startProgressLoop();
                        } else if (playbackState == Player.STATE_ENDED) {
                            onComplete();
                            stopProgressLoop();
                        }
                    }

                    @Override
                    public void onPlayerError(ExoPlaybackException error) {
                        stopProgressLoop();
                    }
                });
            } else {
                ArrayList<String> options = new ArrayList<>(20);
                options.add("--network-caching=" + buffer);
                options.add("--no-audio-time-stretch");
                options.add("-v");

                mLibVLC = new LibVLC(TvApp.getApplication(), options);

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

    private static Runnable progressLoop;
    private static Handler mHandler = new Handler();
    private static void startProgressLoop() {
        progressLoop = new Runnable() {
            @Override
            public void run() {
                reportProgress();
                mHandler.postDelayed(this, 500);
            }
        };
        mHandler.post(progressLoop);
    }

    private static void stopProgressLoop() {
        if (progressLoop != null) {
            mHandler.removeCallbacks(progressLoop);
        }
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

    private static void fireQueueReplaced(){
        for (AudioEventListener listener : mAudioEventListeners) {
            TvApp.getApplication().getLogger().Info("Firing queue replaced listener. ");
            listener.onQueueReplaced();
        }
    }

    private static void fireQueueStatusChange() {
        for (AudioEventListener listener : mAudioEventListeners) {
            TvApp.getApplication().getLogger().Info("Firing queue state change listener. %b", hasAudioQueueItems());
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
                                Toast.makeText(activity, "Queue saved as new playlist: "+text, Toast.LENGTH_LONG).show();
                                TvApp.getApplication().setLastLibraryChange(Calendar.getInstance());
                            }

                            @Override
                            public void onError(Exception exception) {
                                TvApp.getApplication().getLogger().ErrorException("Exception creating playlist", exception);
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

        Toast.makeText(TvApp.getApplication(), items.size() + (items.size() > 1 ? TvApp.getApplication().getString(R.string.msg_items_added) : TvApp.getApplication().getString(R.string.msg_item_added)), Toast.LENGTH_LONG).show();
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

    public static boolean isPlayingAudio() { return audioInitialized && (nativeMode ? mExoPlayer.isPlaying() : mVlcPlayer.isPlaying()); }

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

        boolean fireQueueReplaceEvent = hasAudioQueueItems();

        playNowInternal(items);

        if (fireQueueReplaceEvent)
            fireQueueReplaced();
    }

    private static void playNowInternal(List<BaseItemDto> items) {
        createAudioQueue(items);
        mCurrentAudioQueuePosition = -1;
        nextAudioItem();
        if (TvApp.getApplication().getCurrentActivity().getClass() != AudioNowPlayingActivity.class) {
            Intent nowPlaying = new Intent(TvApp.getApplication(), AudioNowPlayingActivity.class);
            TvApp.getApplication().getCurrentActivity().startActivity(nowPlaying);
        } else {
            Toast.makeText(TvApp.getApplication(),items.size() + (items.size() > 1 ? TvApp.getApplication().getString(R.string.msg_items_added) : TvApp.getApplication().getString(R.string.msg_item_added)), Toast.LENGTH_LONG).show();
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
        DeviceProfile profile = ProfileHelper.getBaseProfile(false);
        if (DeviceUtils.is60()) {
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
                    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(TvApp.getApplication(), "ATV/ExoPlayer");

                    mExoPlayer.setPlayWhenReady(true);
                    mExoPlayer.prepare(new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(response.ToUrl(apiClient.getApiUrl(), apiClient.getAccessToken()))));
                } else {
                    TvApp.getApplication().getLogger().Info("Playback attempt via VLC of %s", response.getMediaUrl());
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

                ReportingHelper.reportStart(item, mCurrentAudioPosition * 10000);
                for (AudioEventListener listener : mAudioEventListeners) {
                    TvApp.getApplication().getLogger().Info("Firing playback state change listener for item start. %s", mCurrentAudioItem.getName());
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
            if (nativeMode) mExoPlayer.seekTo(0);
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
        if (nativeMode) mExoPlayer.stop(true);
        else mVlcPlayer.stop();
    }

    public static void stopAudio() {
        if (mCurrentAudioItem != null && isPlayingAudio()) {
            stop();
            updateCurrentAudioItemPlaying(false);
            ReportingHelper.reportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition*10000);
            for (AudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.IDLE, mCurrentAudioItem);
            }
            //UnRegister a media button receiver
            mAudioManager.unregisterMediaButtonEventReceiver(new ComponentName(TvApp.getApplication().getPackageName(), RemoteControlReceiver.class.getName()));

        }
    }

    private static void pause() {
        if (nativeMode) mExoPlayer.setPlayWhenReady(false);
        else mVlcPlayer.pause();
    }

    public static void pauseAudio() {
        if (mCurrentAudioItem != null && isPlayingAudio()) {
            updateCurrentAudioItemPlaying(false);
            pause();
            ReportingHelper.reportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition * 10000);
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
            if (nativeMode) mExoPlayer.setPlayWhenReady(true);
            else mVlcPlayer.play();
            updateCurrentAudioItemPlaying(true);
            ReportingHelper.reportStart(mCurrentAudioItem, mCurrentAudioPosition * 10000);
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
