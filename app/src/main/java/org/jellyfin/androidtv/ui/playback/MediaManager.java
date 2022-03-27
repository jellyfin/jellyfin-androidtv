package org.jellyfin.androidtv.ui.playback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.compat.AudioOptions;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.di.AppModuleKt;
import org.jellyfin.androidtv.ui.itemhandling.AudioQueueItem;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.util.ContextExtensionsKt;
import org.jellyfin.androidtv.util.DeviceUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.ReportingHelper;
import org.jellyfin.androidtv.util.profile.ExoPlayerProfile;
import org.jellyfin.androidtv.util.profile.LibVlcProfile;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dlna.DeviceProfile;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.playlists.PlaylistCreationRequest;
import org.jellyfin.apiclient.model.playlists.PlaylistCreationResult;
import org.jellyfin.sdk.model.DeviceInfo;
import org.koin.java.KoinJavaComponent;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import timber.log.Timber;

public class MediaManager {
    private ItemRowAdapter mCurrentMediaAdapter;
    private int mCurrentMediaPosition = -1;
    private String currentMediaTitle;

    private ItemRowAdapter mCurrentAudioQueue;
    private ItemRowAdapter mManagedAudioQueue;

    private List<Integer>  mUnShuffledAudioQueueIndexes;

    private int mCurrentAudioQueuePosition = -1;
    private BaseItemDto mCurrentAudioItem;
    private StreamInfo mCurrentAudioStreamInfo;
    private long mCurrentAudioPosition;

    private LibVLC mLibVLC;
    private org.videolan.libvlc.MediaPlayer mVlcPlayer;
    private VlcEventHandler mVlcHandler = new VlcEventHandler();
    private ExoPlayer mExoPlayer;
    private AudioManager mAudioManager;
    private boolean audioInitialized = false;
    private boolean nativeMode = false;
    private boolean videoQueueModified = false;

    private List<AudioEventListener> mAudioEventListeners = new ArrayList<>();

    private long lastProgressReport;
    private long lastProgressEvent;

    private long lastReportedPlaybackPosition = -1;
    private long lastUniqueProgressEvent = -1;

    private boolean mRepeat;

    private List<BaseItemDto> mCurrentVideoQueue;

    public ItemRowAdapter getCurrentMediaAdapter() {
        return mCurrentMediaAdapter;
    }
    public boolean hasAudioQueueItems() { return mCurrentAudioQueue != null && mCurrentAudioQueue.size() > 0; }
    public boolean hasVideoQueueItems() { return mCurrentVideoQueue != null && mCurrentVideoQueue.size() > 0; }

    public void setCurrentMediaAdapter(ItemRowAdapter currentMediaAdapter) {
        this.mCurrentMediaAdapter = currentMediaAdapter;
    }

    public int getCurrentMediaPosition() {
        return mCurrentMediaPosition;
    }

    public void setCurrentVideoQueue(List<BaseItemDto> items) {
        if (items == null) {
            clearVideoQueue();
            return;
        }

        // protect against items secretly being an Arrays.asList(), which are fixed-size
        List<BaseItemDto> newMutableItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++){
            newMutableItems.add(items.get(i));
        }
        mCurrentVideoQueue = newMutableItems;
    }

    public List<BaseItemDto> getCurrentVideoQueue() { return mCurrentVideoQueue; }

    public int getCurrentAudioQueueSize() { return mCurrentAudioQueue != null ? mCurrentAudioQueue.size() : 0; }
    public int getCurrentAudioQueuePosition() { return hasAudioQueueItems() && mCurrentAudioQueuePosition >= 0 ? mCurrentAudioQueuePosition : 0; }
    public long getCurrentAudioPosition() { return mCurrentAudioPosition; }
    public String getCurrentAudioQueueDisplayPosition() { return Integer.toString(getCurrentAudioQueuePosition() + 1); }
    public String getCurrentAudioQueueDisplaySize() { return mCurrentAudioQueue != null ? Integer.toString(mCurrentAudioQueue.size()) : "0"; }

    public BaseItemDto getCurrentAudioItem() { return mCurrentAudioItem != null ? mCurrentAudioItem : hasAudioQueueItems() ? ((BaseRowItem)mCurrentAudioQueue.get(0)).getBaseItem() : null; }

    public boolean toggleRepeat() { mRepeat = !mRepeat; return mRepeat; }
    public boolean isRepeatMode() { return mRepeat; }

    public boolean getIsAudioPlayerInitialized() {
        return audioInitialized && (nativeMode ? mExoPlayer != null : mVlcPlayer != null);
    }

    public boolean isShuffleMode() {
        if (mUnShuffledAudioQueueIndexes != null) {
            return true;
        }
        return false;
    }

    private void clearUnShuffledQueue() {
        mUnShuffledAudioQueueIndexes = null;
    }

    public ItemRowAdapter getCurrentAudioQueue() { return mCurrentAudioQueue; }
    public ItemRowAdapter getManagedAudioQueue() {
        createManagedAudioQueue();
        return mManagedAudioQueue;
    }

    public void createManagedAudioQueue() {
        if (mCurrentAudioQueue != null) {
            if (mManagedAudioQueue != null) {
                //re-create existing one
                mManagedAudioQueue.clear();
                for (int i = getCurrentAudioQueuePosition(); i < mCurrentAudioQueue.size(); i++) {
                    mManagedAudioQueue.add(mCurrentAudioQueue.get(i));
                }
            } else {
                List<BaseItemDto> managedItems = new ArrayList<>();
                for (int i = getCurrentAudioQueuePosition(); i < mCurrentAudioQueue.size(); i++) {
                    managedItems.add(((BaseRowItem)mCurrentAudioQueue.get(i)).getBaseItem());
                }
                mManagedAudioQueue = new ItemRowAdapter(managedItems, new CardPresenter(true, Utils.convertDpToPixel(TvApp.getApplication(), 150)), null, QueryType.StaticAudioQueueItems);
                mManagedAudioQueue.Retrieve();
            }
            if (mManagedAudioQueue.size() > 0 && isPlayingAudio()) {
                ((BaseRowItem)mManagedAudioQueue.get(0)).setIsPlaying(true);
            } else if (mManagedAudioQueue.size() < 1) {
                Timber.d("error creating managed audio queue from size of: %s", mCurrentAudioQueue.size());
            }
        }
    }

    public void addAudioEventListener(AudioEventListener listener) {
        mAudioEventListeners.add(listener);
        Timber.d("Added event listener.  Total listeners: %d", mAudioEventListeners.size());
    }
    public void removeAudioEventListener(AudioEventListener listener) {
        mAudioEventListeners.remove(listener);
        Timber.d("Removed event listener.  Total listeners: %d", mAudioEventListeners.size());
    }

    public boolean initAudio() {
        Timber.d("initializing audio");
        if (mAudioManager == null) mAudioManager = (AudioManager) TvApp.getApplication().getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager == null) {
            Timber.e("Unable to get audio manager");
            Utils.showToast(TvApp.getApplication(), R.string.msg_cannot_play_time);
            return false;
        }

        return createPlayer(600);
    }

    private boolean isPaused() {
        // report true if player is null
        // allows remote tvApiEventListener to call playPauseAudio() and start playback if playback is stopped
        return nativeMode ? (mExoPlayer != null ? !mExoPlayer.isPlaying() : true) : (mVlcPlayer != null ? !mVlcPlayer.isPlaying() : true);
    }

    private void reportProgress() {
        if (mCurrentAudioItem == null || !getIsAudioPlayerInitialized()) {
            stopProgressLoop();
            return;
        }
        //Don't need to be too aggressive with these calls - just be sure every second
        if (System.currentTimeMillis() < lastProgressEvent + 750) return;
        lastProgressEvent = System.currentTimeMillis();

        mCurrentAudioPosition = nativeMode ? mExoPlayer.getCurrentPosition() : mVlcPlayer.getTime();

        // until MediaSessions are used to handle playback interruptions that won't be caught by the player, catch them with a timeout
        if (mCurrentAudioPosition != lastReportedPlaybackPosition || lastUniqueProgressEvent == -1) {
            lastUniqueProgressEvent = lastProgressEvent;
        } else if (!isPaused() && lastProgressEvent - lastUniqueProgressEvent > 15000) {
            Timber.d("playback stalled due to uncaught error - pausing");
            pauseAudio();
            return;
        }

        lastReportedPlaybackPosition = mCurrentAudioPosition;

        //fire external listeners if there
        for (AudioEventListener listener : mAudioEventListeners) {
            listener.onProgress(mCurrentAudioPosition);
        }

        //Report progress to server every 5 secs if playing, 15 if paused
        if (System.currentTimeMillis() > lastProgressReport + (isPaused() ? 15000 : 5000)) {
            ReportingHelper.reportProgress(null, mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition*10000, isPaused());
            lastProgressReport = System.currentTimeMillis();
        }

    }

    private void onComplete() {
        Timber.d("item complete");
        stopAudio(hasNextAudioItem());

        if (hasNextAudioItem()) {
            nextAudioItem();
        } else if (hasAudioQueueItems()) {
            clearAudioQueue();
        }
    }

    private void releasePlayer() {
        Timber.d("releasing audio player");
        if (mVlcPlayer != null) {
            mVlcPlayer.setEventListener(null);
            mVlcPlayer.release();
            mLibVLC.release();
            mLibVLC = null;
            mVlcPlayer = null;
        }
        if (mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }

    private boolean createPlayer(int buffer) {
        try {

            // Create a new media player based on platform
            if (DeviceUtils.is60()) {
                Timber.i("creating audio player using: exoplayer");
                nativeMode = true;
                mExoPlayer = new ExoPlayer.Builder(TvApp.getApplication()).build();
                mExoPlayer.addListener(new Player.EventListener() {
                    @Override
                    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                        if (playbackState == Player.STATE_READY) {
                            startProgressLoop();
                        } else if (playbackState == Player.STATE_ENDED) {
                            onComplete();
                            stopProgressLoop();
                        } else if (playbackState == Player.STATE_IDLE) {
                            stopProgressLoop();
                        }
                    }
                    @Override
                    public void onPlayerError(PlaybackException error) {
                        Timber.d("player error!");
                        stopAudio(true);
                    }
                });
            } else {
                Timber.i("creating audio player using: libVLC");
                ArrayList<String> options = new ArrayList<>(20);
                options.add("--network-caching=" + buffer);
                options.add("--no-audio-time-stretch");
                options.add("-v");

                mLibVLC = new LibVLC(TvApp.getApplication(), options);

                mVlcPlayer = new org.videolan.libvlc.MediaPlayer(mLibVLC);
                if(!Utils.downMixAudio()) {
                    mVlcPlayer.setAudioDigitalOutputEnabled(true);
                } else {
                    mVlcPlayer.setAudioOutput("opensles_android");
                    mVlcPlayer.setAudioOutputDevice("hdmi");
                }

                mVlcHandler.setOnPreparedListener(new PlaybackListener() {
                    @Override
                    public void onEvent() {
                        Timber.i("libVLC onPrepared - starting progress loop");
                        startProgressLoop();
                    }
                });

                mVlcHandler.setOnProgressListener(new PlaybackListener() {
                    @Override
                    public void onEvent() {
                        if (!isPlayingAudio()) {
                            stopProgressLoop();
                        }
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
            Timber.e(e, "Error creating VLC player");
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_video_playback_error));
            return false;
        }

        return true;
    }

    private Runnable progressLoop;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private void startProgressLoop() {
        stopProgressLoop();
        Timber.i("starting progress loop");
        for (AudioEventListener listener : mAudioEventListeners) {
            Timber.i("Firing playback state change listener for item: %s", mCurrentAudioItem.getName());
            listener.onPlaybackStateChange(isPlayingAudio() ? PlaybackController.PlaybackState.PLAYING : PlaybackController.PlaybackState.PAUSED, mCurrentAudioItem);
        }
        progressLoop = new Runnable() {
            @Override
            public void run() {
                reportProgress();
                mHandler.postDelayed(this, 500);
            }
        };
        mHandler.post(progressLoop);
    }

    private void stopProgressLoop() {
        lastUniqueProgressEvent = -1;
        lastReportedPlaybackPosition = -1;
        if (progressLoop != null) {
            Timber.i("stopping progress loop");
            mHandler.removeCallbacks(progressLoop);
            progressLoop = null;
        }
    }

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChanged = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    pauseAudio();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    Timber.d("stopping audio player and releasing due to audio focus loss");
                    stopAudio(true);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    //resumeAudio();
                    break;
            }
        }
    };

    private void fireQueueReplaced(){
        for (AudioEventListener listener : mAudioEventListeners) {
            Timber.i("Firing queue replaced listener. ");
            listener.onQueueReplaced();
        }
    }

    private void fireQueueStatusChange() {
        for (AudioEventListener listener : mAudioEventListeners) {
            Timber.i("Firing queue state change listener. %b", hasAudioQueueItems());
            listener.onQueueStatusChanged(hasAudioQueueItems());
        }

    }

    private void createAudioQueue(List<BaseItemDto> items) {
        mCurrentAudioQueue = new ItemRowAdapter(items, new CardPresenter(true, Utils.convertDpToPixel(TvApp.getApplication(), 140)), null, QueryType.StaticAudioQueueItems);
        mCurrentAudioQueue.Retrieve();
        mManagedAudioQueue = null;
        fireQueueStatusChange();
    }

    private static final int TYPE_AUDIO = 0;
    private static final int TYPE_VIDEO = 1;

    public void saveAudioQueue(Activity activity) {
        saveQueue(activity, TYPE_AUDIO);
    }

    public void saveVideoQueue(Activity activity) {
        saveQueue(activity, TYPE_VIDEO);
    }

    public void saveQueue(Activity activity, final int type) {
        //Get a name and save as playlist
        final EditText name = new EditText(activity);
        name.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.lbl_save_as_playlist)
                .setMessage(R.string.lbl_new_playlist_name)
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
                        KoinJavaComponent.<ApiClient>get(ApiClient.class).CreatePlaylist(request, new Response<PlaylistCreationResult>() {
                            @Override
                            public void onResponse(PlaylistCreationResult response) {
                                Toast.makeText(activity, activity.getString(R.string.msg_queue_saved, text), Toast.LENGTH_LONG).show();
                                DataRefreshService dataRefreshService = KoinJavaComponent.<DataRefreshService>get(DataRefreshService.class);
                                dataRefreshService.setLastLibraryChange(System.currentTimeMillis());
                            }

                            @Override
                            public void onError(Exception exception) {
                                Timber.e(exception, "Exception creating playlist");
                            }
                        });
                    }
                })
                .show();

    }

    private ArrayList<String> getCurrentAudioQueueItemIds() {
        ArrayList<String> result = new ArrayList<>();

        if (mCurrentAudioQueue != null) {
            for (int i = 0; i < mCurrentAudioQueue.size(); i++) {
                AudioQueueItem item = (AudioQueueItem) mCurrentAudioQueue.get(i);
                result.add(item.getItemId());
            }
        }

        return result;
    }

    private ArrayList<String> getCurrentVideoQueueItemIds() {
        ArrayList<String> result = new ArrayList<>();

        if (mCurrentVideoQueue != null) {
            for (int i = 0; i < mCurrentVideoQueue.size(); i++) {
                result.add(mCurrentVideoQueue.get(i).getId());
            }
        }

        return result;
    }

    public int queueAudioItem(BaseItemDto item) {
        if (mCurrentAudioQueue == null) {
            createAudioQueue(new ArrayList<BaseItemDto>());
            clearUnShuffledQueue();
        }
        pushToUnShuffledQueue(item);
        mCurrentAudioQueue.add(new AudioQueueItem(mCurrentAudioQueue.size(), item));
        fireQueueStatusChange();
        return mCurrentAudioQueue.size()-1;
    }

    public int addToVideoQueue(BaseItemDto item) {
        if (mCurrentVideoQueue == null) mCurrentVideoQueue = new ArrayList<>();
        mCurrentVideoQueue.add(item);
        videoQueueModified = true;
        DataRefreshService dataRefreshService = KoinJavaComponent.<DataRefreshService>get(DataRefreshService.class);
        dataRefreshService.setLastVideoQueueChange(System.currentTimeMillis());

        long total = System.currentTimeMillis();
        for (BaseItemDto video :
                mCurrentVideoQueue) {
            total += video.getRunTimeTicks() / 10000;
        }

        Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_added_to_video, item.getName(), android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(new Date(total))));
        return mCurrentVideoQueue.size()-1;
    }

    public void clearAudioQueue() {
        clearAudioQueue(false);
    }

    public void clearAudioQueue(boolean releasePlayer) {
        Timber.d("clearing the audio queue");
        stopAudio(releasePlayer);
        clearUnShuffledQueue();
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

    public void addToAudioQueue(List<BaseItemDto> items) {
        if (mCurrentAudioQueue == null) {
            createAudioQueue(items);
            clearUnShuffledQueue();
        } else {
            int ndx = mCurrentAudioQueue.size();
            for (BaseItemDto item : items) {
                AudioQueueItem queueItem = new AudioQueueItem(ndx++, item);
                mCurrentAudioQueue.add(queueItem);
                if (mManagedAudioQueue != null) mManagedAudioQueue.add(queueItem);
                pushToUnShuffledQueue(item);
            }
            fireQueueStatusChange();
        }

        Toast.makeText(TvApp.getApplication(), items.size() + (items.size() > 1 ? TvApp.getApplication().getString(R.string.msg_items_added) : TvApp.getApplication().getString(R.string.msg_item_added)), Toast.LENGTH_LONG).show();
    }

    public void removeFromAudioQueue(int ndx) {
        if (!hasAudioQueueItems() || ndx > getCurrentAudioQueueSize()) return;

        removeFromUnShuffledQueue(ndx);
        if (mManagedAudioQueue != null) mManagedAudioQueue.remove(mCurrentAudioQueue.get(ndx));

        if (mCurrentAudioQueuePosition == ndx) {
            // current item - stop audio, remove and re-start
            stopAudio(false);
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
            }
        } else {
            //just remove it
            mCurrentAudioQueue.removeItems(ndx, 1);
            if (mCurrentAudioQueuePosition > ndx) mCurrentAudioQueuePosition--;
        }

        // now need to update indexes for subsequent items
        if (hasAudioQueueItems()) {
            for (int i = ndx; i < mCurrentAudioQueue.size(); i++){
                ((BaseRowItem)mCurrentAudioQueue.get(i)).setIndex(i);
            }
        } else {
            clearUnShuffledQueue();
        }
        fireQueueStatusChange();
    }

    public boolean isPlayingAudio() { return getIsAudioPlayerInitialized() && (nativeMode ? mExoPlayer.isPlaying() : mVlcPlayer.isPlaying()); }

    private boolean ensureInitialized() {
        if (!audioInitialized || !getIsAudioPlayerInitialized()) {
            audioInitialized = initAudio();
        }

        if (!audioInitialized) {
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.audio_error));
        }

        return audioInitialized;
    }

    public void playNow(Context context, final List<BaseItemDto> items, int position, boolean shuffle) {
        if (!ensureInitialized()) return;

        boolean fireQueueReplaceEvent = hasAudioQueueItems();

        List<BaseItemDto> list = new ArrayList<BaseItemDto>();
        for (int i = 0; i < items.size(); i++){
            if (items.get(i).getBaseItemType() == BaseItemType.Audio) {
                list.add(items.get(i));
            } else if (i < position) {
                position--;
            }
        }
        if (position < 0)
            position = 0;

        playNowInternal(context, list, position, shuffle);

        if (fireQueueReplaceEvent)
            fireQueueReplaced();
    }

    public void playNow(Context context, final List<BaseItemDto> items, boolean shuffle) {
        playNow(context, items, 0, shuffle);
    }

    public void playNow(Context context, final BaseItemDto item) {
        if (!ensureInitialized()) return;

        List<BaseItemDto> list = new ArrayList<BaseItemDto>();
        list.add(item);
        playNow(context, list, false);
    }

    private void playNowInternal(Context context, List<BaseItemDto> items, int position, boolean shuffle) {
        if (items == null || items.size() == 0) return;
        if (position < 0 || position >= items.size()) position = 0;
        // stop current item before queue is cleared so it can still be referenced
        if (isPlayingAudio()) {
            stopAudio(false);
        }
        clearUnShuffledQueue();
        createAudioQueue(items);

        mCurrentAudioQueuePosition = shuffle ? new Random().nextInt(items.size()) : position;

        if (shuffle) shuffleAudioQueue();
        playFrom(position);

        Activity activity = ContextExtensionsKt.getActivity(context);
        if (activity != null && activity.getClass() != AudioNowPlayingActivity.class) {
            Intent nowPlaying = new Intent(context, AudioNowPlayingActivity.class);
            context.startActivity(nowPlaying);
        } else {
            Toast.makeText(context,items.size() + (items.size() > 1 ? context.getString(R.string.msg_items_added) : context.getString(R.string.msg_item_added)), Toast.LENGTH_LONG).show();
        }
    }

    public boolean playFrom(int ndx) {
        if (mCurrentAudioQueue == null || ndx >= mCurrentAudioQueue.size()) return false;

        if (!ensureInitialized()) return false;

        if (isPlayingAudio()) stopAudio(false);

        Timber.d("playing audio queue from pos %s", ndx);

        mCurrentAudioQueuePosition = ndx < 0 || ndx >= mCurrentAudioQueue.size() ? -1 : ndx - 1;
        createManagedAudioQueue();
        nextAudioItem();
        return true;
    }

    private boolean ensureAudioFocus() {
        if (mAudioManager.requestAudioFocus(mAudioFocusChanged, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Timber.e("Unable to get audio focus");
            Utils.showToast(TvApp.getApplication(), R.string.msg_cannot_play_time);
            return false;
        }

        return true;
    }

    private void playInternal(final BaseItemDto item, final int pos) {
        if (!ensureInitialized()) return;

        ensureAudioFocus();
        final ApiClient apiClient = KoinJavaComponent.<ApiClient>get(ApiClient.class);
        AudioOptions options = new AudioOptions();
        options.setItemId(item.getId());
        Integer maxBitrate = Utils.getMaxBitrate();
        if (maxBitrate != null) options.setMaxBitrate(maxBitrate);
        options.setMediaSources(item.getMediaSources());
        DeviceProfile profile;
        if (DeviceUtils.is60()) {
            profile = new ExoPlayerProfile();
        } else {
            profile = new LibVlcProfile();
        }
        options.setProfile(profile);

        DeviceInfo deviceInfo = KoinJavaComponent.<org.jellyfin.sdk.api.client.ApiClient>get(org.jellyfin.sdk.api.client.ApiClient.class, AppModuleKt.getUserApiClient()).getDeviceInfo();
        KoinJavaComponent.<PlaybackManager>get(PlaybackManager.class).getAudioStreamInfo(deviceInfo, options, item.getResumePositionTicks(), apiClient, new Response<StreamInfo>() {
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
                    Timber.i("Playback attempt via VLC of %s", response.getMediaUrl());
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
                DataRefreshService dataRefreshService = KoinJavaComponent.<DataRefreshService>get(DataRefreshService.class);
                dataRefreshService.setLastMusicPlayback(System.currentTimeMillis());

                ReportingHelper.reportStart(item, mCurrentAudioPosition * 10000);
            }

            @Override
            public void onError(Exception exception) {
                Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.audio_error, exception.getLocalizedMessage()));
            }
        });

    }

    private void pushToUnShuffledQueue(BaseItemDto newItem) {
        if (isShuffleMode()) {
            mUnShuffledAudioQueueIndexes.add(mUnShuffledAudioQueueIndexes.size());
        }
    }

    private void removeFromUnShuffledQueue(int ndx) {
        if (hasAudioQueueItems() && isShuffleMode() && ndx < mUnShuffledAudioQueueIndexes.size()) {
            int OriginalNdx = mUnShuffledAudioQueueIndexes.get(ndx);
            for(int i = 0; i < mUnShuffledAudioQueueIndexes.size(); i++) {
                int oldNdx = mUnShuffledAudioQueueIndexes.get(i);
                if (oldNdx > OriginalNdx) mUnShuffledAudioQueueIndexes.set(i, --oldNdx);
            }
            mUnShuffledAudioQueueIndexes.remove(ndx);
        }
    }

    public void shuffleAudioQueue() {
        if (!hasAudioQueueItems()) return;

        /*
            # Shuffle feature

            # Dependencies
                * mUnShuffledAudioQueueIndexes - List<Integer> shuffled list of the original queue item indexes

            # Methods
                * isShuffleMode()                    - true/false for checking if shuffled
                * clearUnShuffledQueue()             - set the saved queue to null
                * pushToUnShuffledQueue()            - push a new items index to the shuffled queue of original indexes
                * removeFromUnShuffledQueue(int ndx) - updates the original indexes to reflect the removal, and removes the item from the saved queue

            # Implementation
                1) create a fixed size BaseItemDto[] of queue size to be populated with shuffled or unshuffled items
                2)
                    A) if not shuffled
                        1A) create new queue for saving the original queue state
                        2A) push all but the currently playing item's indexes to the list
                        3A) shuffle the list of indexes then insert the currently playing item's index at pos 0 in the list
                        4A) loop through the shuffled list of indexes and insert the corresponding items into the array

                    B) if shuffled
                        1B) inserts each queue item into the array using its original index

                3) create a new list and push all items from the array into the list, and set the current queue position when its found
                4) create a new queue from the list

         */
        BaseItemDto[] items = new BaseItemDto[isShuffleMode() ? mUnShuffledAudioQueueIndexes.size() : mCurrentAudioQueue.size()];

        if (isShuffleMode()) {
            Timber.d("queue is already shuffled, restoring original order");

            for(int i = 0; i < mUnShuffledAudioQueueIndexes.size(); i++) {
                items[mUnShuffledAudioQueueIndexes.get(i)] = ((BaseRowItem) mCurrentAudioQueue.get(i)).getBaseItem();
            }
            mUnShuffledAudioQueueIndexes = null;
        } else {
            Timber.d("Queue is not shuffled, shuffling");
            mUnShuffledAudioQueueIndexes = new ArrayList<>();

            for(int i = 0; i < mCurrentAudioQueue.size(); i++) {
                if (i != getCurrentAudioQueuePosition()) {
                    mUnShuffledAudioQueueIndexes.add(i);
                }
            }
            Collections.shuffle(mUnShuffledAudioQueueIndexes);
            mUnShuffledAudioQueueIndexes.add(0, getCurrentAudioQueuePosition());
            for(int i = 0; i < mUnShuffledAudioQueueIndexes.size(); i++) {
                items[i] = ((BaseRowItem) mCurrentAudioQueue.get(mUnShuffledAudioQueueIndexes.get(i))).getBaseItem();
            }
        }

        List<BaseItemDto> itemsList = new ArrayList<>();
        for(int i = 0; i < items.length; i++) {
            if (items[i] == getCurrentAudioItem()) {
                mCurrentAudioQueuePosition = i;
            }
            itemsList.add(items[i]);
        }
        createAudioQueue(itemsList);
        updateCurrentAudioItemPlaying(isPlayingAudio());
        fireQueueReplaced();
    }

    public BaseItemDto getNextAudioItem() {
        if (mCurrentAudioQueue == null || mCurrentAudioQueue.size() == 0 || (!mRepeat && mCurrentAudioQueuePosition == mCurrentAudioQueue.size() - 1)) return null;

        int ndx = mCurrentAudioQueuePosition+1;
        if (ndx >= mCurrentAudioQueue.size()) ndx = 0;
        return ((BaseRowItem)mCurrentAudioQueue.get(ndx)).getBaseItem();
    }

    public BaseItemDto getPrevAudioItem() {
        if (mCurrentAudioQueue == null || mCurrentAudioQueue.size() == 0 || (!mRepeat && mCurrentAudioQueuePosition == 0)) return null;

        int ndx = mCurrentAudioQueuePosition-1;
        if (ndx < 0) ndx = mCurrentAudioQueue.size() - 1;
        return ((BaseRowItem)mCurrentAudioQueue.get(ndx)).getBaseItem();
    }

    public boolean hasNextAudioItem() { return mCurrentAudioQueue != null && mCurrentAudioQueue.size() > 0 && (mRepeat || mCurrentAudioQueuePosition < mCurrentAudioQueue.size()-1); }
    public boolean hasPrevAudioItem() { return mCurrentAudioQueue != null && mCurrentAudioQueue.size() > 0 && (mRepeat || mCurrentAudioQueuePosition > 0); }

    public void updateCurrentAudioItemPlaying(boolean playing) {
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

    public int nextAudioItem() {
        //turn off indicator for current item
        if (mCurrentAudioQueuePosition >= 0) {
            updateCurrentAudioItemPlaying(false);
        }

        if (mCurrentAudioQueue == null || mCurrentAudioQueue.size() == 0 || (!mRepeat && mCurrentAudioQueuePosition == mCurrentAudioQueue.size() - 1)) return -1;
        stopAudio(false);
        if (mManagedAudioQueue != null && mManagedAudioQueue.size() > 1) {
            //don't remove last item as it causes framework crashes
            mManagedAudioQueue.removeItems(0, 1);
        }
        int ndx = mCurrentAudioQueuePosition +1;
        if (ndx >= mCurrentAudioQueue.size()) ndx = 0;
        playInternal(getNextAudioItem(), ndx);
        return ndx;
    }

    public int prevAudioItem() {
        if (mCurrentAudioQueue == null || (!mRepeat && mCurrentAudioQueue.size() == 0)) return -1;
        if (isPlayingAudio() && mCurrentAudioPosition > 10000) {
            //just back up to the beginning of current item
            if (nativeMode) mExoPlayer.seekTo(0);
            else mVlcPlayer.setTime(0);
            return mCurrentAudioQueuePosition;
        }

        if (!mRepeat && mCurrentAudioQueuePosition < 1) {
            //nowhere to go
            return mCurrentAudioQueuePosition;
        }

        stopAudio(false);
        int ndx = mCurrentAudioQueuePosition == 0 ? mCurrentAudioQueue.size() - 1 : mCurrentAudioQueuePosition - 1;
        if (mManagedAudioQueue != null) {
            mManagedAudioQueue.add(0, mCurrentAudioQueue.get(ndx));
        }
        playInternal(getPrevAudioItem(), ndx);
        return ndx;
    }

    private void stop() {
        if (!getIsAudioPlayerInitialized()) return ;
        if (nativeMode) mExoPlayer.stop();
        else mVlcPlayer.stop();
    }

    public void stopAudio(boolean releasePlayer) {
        if (mCurrentAudioItem != null) {
            Timber.d("Stopping audio");
            stop();
            updateCurrentAudioItemPlaying(false);
            stopProgressLoop();
            ReportingHelper.reportStopped(mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition * 10000);
            mCurrentAudioPosition = 0;
            for (AudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.IDLE, mCurrentAudioItem);
            }
            if (releasePlayer) {
                releasePlayer();
                if (mAudioManager != null) mAudioManager.abandonAudioFocus(mAudioFocusChanged);
            }
        }
    }

    private void pause() {
        if (!getIsAudioPlayerInitialized()) return;
        if (nativeMode) mExoPlayer.setPlayWhenReady(false);
        else mVlcPlayer.pause();
    }

    public void pauseAudio() {
        if (mCurrentAudioItem != null && isPlayingAudio()) {
            updateCurrentAudioItemPlaying(false);
            pause();
            lastProgressReport = System.currentTimeMillis();
            ReportingHelper.reportProgress(null, mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition * 10000, true);
            for (AudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.PAUSED, mCurrentAudioItem);
            }
        }
    }

    public void playPauseAudio() {
        if (isPaused()) {
            resumeAudio();
        } else {
            pauseAudio();
        }
    }


    public void resumeAudio() {
        if (mCurrentAudioItem != null && getIsAudioPlayerInitialized()) {
            ensureAudioFocus();
            if (nativeMode) mExoPlayer.setPlayWhenReady(true);
            else mVlcPlayer.play();
            updateCurrentAudioItemPlaying(true);
            lastProgressReport = System.currentTimeMillis();
            ReportingHelper.reportProgress(null, mCurrentAudioItem, mCurrentAudioStreamInfo, mCurrentAudioPosition * 10000, false);
            for (AudioEventListener listener : mAudioEventListeners) {
                listener.onPlaybackStateChange(PlaybackController.PlaybackState.PLAYING, mCurrentAudioItem);
            }
        } else if (hasAudioQueueItems()) {
            //play from start
            playInternal(mCurrentAudioItem != null ? mCurrentAudioItem : ((BaseRowItem)mCurrentAudioQueue.get(0)).getBaseItem(), mCurrentAudioItem != null ? getCurrentAudioQueuePosition() : 0);
        }
    }

    public void setCurrentMediaPosition(int currentMediaPosition) {
        this.mCurrentMediaPosition = currentMediaPosition;
    }

    public BaseRowItem getMediaItem(int pos) {
        return mCurrentMediaAdapter != null && mCurrentMediaAdapter.size() > pos ? (BaseRowItem) mCurrentMediaAdapter.get(pos) : null;
    }

    public BaseRowItem getCurrentMediaItem() { return getMediaItem(mCurrentMediaPosition); }

    public BaseRowItem nextMedia() {
        if (hasNextMediaItem()) {
            mCurrentMediaPosition++;
            mCurrentMediaAdapter.loadMoreItemsIfNeeded(mCurrentMediaPosition);
        }

        return getCurrentMediaItem();
    }

    public BaseRowItem prevMedia() {
        if (hasPrevMediaItem()) {
            mCurrentMediaPosition--;
        }

        return getCurrentMediaItem();
    }

    public BaseRowItem peekNextMediaItem() {
        return hasNextMediaItem() ? getMediaItem(mCurrentMediaPosition +1) : null;
    }

    public BaseRowItem peekPrevMediaItem() {
        return hasPrevMediaItem() ? getMediaItem(mCurrentMediaPosition -1) : null;
    }

    public boolean hasNextMediaItem() { return mCurrentMediaAdapter.size() > mCurrentMediaPosition +1; }
    public boolean hasPrevMediaItem() { return mCurrentMediaPosition > 0; }

    public String getCurrentMediaTitle() {
        return currentMediaTitle;
    }

    public void setCurrentMediaTitle(String currentMediaTitle) {
        this.currentMediaTitle = currentMediaTitle;
    }

    public boolean isVideoQueueModified() {
        return videoQueueModified;
    }

    public void setVideoQueueModified(boolean videoQueueModified) {
        this.videoQueueModified = videoQueueModified;
    }

    public void clearVideoQueue() {
        mCurrentVideoQueue = new ArrayList<>();
        videoQueueModified = false;
    }
}
