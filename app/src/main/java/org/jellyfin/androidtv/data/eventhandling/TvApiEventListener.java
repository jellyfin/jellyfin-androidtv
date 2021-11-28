package org.jellyfin.androidtv.data.eventhandling;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;
import android.os.Looper;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher;
import org.jellyfin.androidtv.ui.playback.PlaybackOverlayActivity;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.ApiEventListener;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.entities.LibraryUpdateInfo;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.session.BrowseRequest;
import org.jellyfin.apiclient.model.session.MessageCommand;
import org.jellyfin.apiclient.model.session.PlayRequest;
import org.jellyfin.apiclient.model.session.PlaystateRequest;
import org.jellyfin.apiclient.model.session.SessionInfoDto;
import org.koin.java.KoinJavaComponent;

import java.util.Arrays;

import timber.log.Timber;

public class TvApiEventListener extends ApiEventListener {
    private final DataRefreshService dataRefreshService;
    private final MediaManager mediaManager;

    public TvApiEventListener(DataRefreshService dataRefreshService, MediaManager mediaManager) {
        this.dataRefreshService = dataRefreshService;
        this.mediaManager = mediaManager;
    }

    @Override
    public void onPlaybackStopped(ApiClient client, SessionInfoDto info) {
        TvApp app = TvApp.getApplication();
        Timber.d("Got Playback stopped message from server");
        if (info.getUserId().equals(app.getCurrentUser().getId())) {
            dataRefreshService.setLastPlayback(System.currentTimeMillis());
            if (info.getNowPlayingItem() == null) return;
            switch (info.getNowPlayingItem().getType()) {
                case "Movie":
                    dataRefreshService.setLastMoviePlayback(System.currentTimeMillis());
                    break;
                case "Episode":
                    dataRefreshService.setLastTvPlayback(System.currentTimeMillis());
                    break;

            }
        }
    }

    @Override
    public void onLibraryChanged(ApiClient client, LibraryUpdateInfo info) {
        Timber.d("Library Changed. Added %o items. Removed %o items. Changed %o items.", info.getItemsAdded().size(), info.getItemsRemoved().size(), info.getItemsUpdated().size());
        if (info.getItemsAdded().size() > 0 || info.getItemsRemoved().size() > 0)
            dataRefreshService.setLastLibraryChange(System.currentTimeMillis());
    }

    @Override
    public void onPlaystateCommand(ApiClient client, PlaystateRequest command) {
        PlaybackController playbackController = TvApp.getApplication().getPlaybackController();
        Timber.d("caught playstate command: %s", command.getCommand());

        //handler to access the players on the main thread
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        switch (command.getCommand()) {
            case Stop:
                if (mediaManager.getIsAudioPlayerInitialized())
                    mainThreadHandler.post(() -> mediaManager.stopAudio(true));
                else {
                    Activity currentActivity = TvApp.getApplication().getCurrentActivity();

                    if(currentActivity instanceof PlaybackOverlayActivity)
                        currentActivity.finish();
                }
                break;
            case Pause:
            case Unpause:
            case PlayPause:
                if (mediaManager.getIsAudioPlayerInitialized())
                    mainThreadHandler.post(() -> mediaManager.playPauseAudio());
                else if(playbackController != null && mediaManager.hasVideoQueueItems() && playbackController.hasInitializedVideoManager())
                    mainThreadHandler.post(() -> playbackController.playPause());
                break;
            case NextTrack:
                if (mediaManager.getIsAudioPlayerInitialized() && mediaManager.hasAudioQueueItems())
                    mainThreadHandler.post(() -> mediaManager.nextAudioItem());
                else if(playbackController != null && mediaManager.hasVideoQueueItems() && playbackController.hasInitializedVideoManager())
                    mainThreadHandler.post(() -> playbackController.next());
                break;
            case PreviousTrack:
                if (mediaManager.getIsAudioPlayerInitialized() && mediaManager.hasAudioQueueItems())
                    mainThreadHandler.post(() -> mediaManager.prevAudioItem());
                else if(playbackController != null && mediaManager.hasVideoQueueItems() && playbackController.hasInitializedVideoManager())
                    mainThreadHandler.post(() -> playbackController.prev());
                break;
            case Seek:
                if(playbackController != null && mediaManager.hasVideoQueueItems() && playbackController.hasInitializedVideoManager()) {
                    long pos = command.getSeekPositionTicks() / 10000;
                    mainThreadHandler.post(() -> playbackController.seek(pos));
                }
                break;
            case Rewind:
                if (playbackController != null && mediaManager.hasVideoQueueItems() && playbackController.hasInitializedVideoManager())
                    mainThreadHandler.post(() -> playbackController.skip(-11000));
                break;
            case FastForward:
                if (playbackController != null && mediaManager.hasVideoQueueItems() && playbackController.hasInitializedVideoManager())
                    mainThreadHandler.post(() -> playbackController.skip(30000));
                break;
        }
    }

    @Override
    public void onBrowseCommand(ApiClient client, BrowseRequest command) {
        Timber.d("Browse command received");

        //handler to access the players on the main thread
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        mainThreadHandler.post(() -> {
            if (TvApp.getApplication().getCurrentActivity() == null ||
                    (TvApp.getApplication().getPlaybackController() != null && (TvApp.getApplication().getPlaybackController().isPlaying() || TvApp.getApplication().getPlaybackController().isPaused()))) {
                Timber.i("Command ignored due to no activity or playback in progress");
                return;
            }
            client.GetItemAsync(command.getItemId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                @Override
                public void onResponse(BaseItemDto response) {
                    //Create a rowItem and pass to our handler
                    ItemLauncher.launch(new BaseRowItem(0, response), null, -1, TvApp.getApplication().getCurrentActivity(), true);
                }
            });
        });
    }

    @Override
    public void onPlayCommand(ApiClient client, PlayRequest command) {
        //handler to access the players on the main thread
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        mainThreadHandler.post(() -> {
            if (TvApp.getApplication().getPlaybackController() != null && (TvApp.getApplication().getPlaybackController().isPlaying() || TvApp.getApplication().getPlaybackController().isPaused())) {
                TvApp.getApplication().getCurrentActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.showToast(TvApp.getApplication().getCurrentActivity(), TvApp.getApplication().getString(R.string.msg_remote_already_playing));
                    }
                });
                return;
            }
            if (command.getItemIds().length > 1) {
                Timber.i("Playing multiple items by remote request");
                if (TvApp.getApplication().getCurrentActivity() == null) {
                    Timber.e("No current activity.  Cannot play");
                    return;
                }
                Timber.d("got queue start index: %S", command.getStartIndex());
                Integer startIndex = command.getStartIndex();
                StdItemQuery query = new StdItemQuery(new ItemFields[]{
                        ItemFields.MediaSources,
                        ItemFields.ChildCount
                });
                query.setIds(command.getItemIds());
                KoinJavaComponent.<ApiClient>get(ApiClient.class).GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        if (response.getItems() != null && response.getItems().length > 0) {
                            PlaybackLauncher playbackLauncher = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class);
                            if (playbackLauncher.interceptPlayRequest(TvApp.getApplication(), response.getItems()[0])) return;

                            //peek at first item to see what type it is
                            switch (response.getItems()[0].getMediaType()) {
                                case "Video":
                                    Class activity = KoinJavaComponent.<PlaybackLauncher>get(PlaybackLauncher.class).getPlaybackActivityClass(response.getItems()[0].getBaseItemType());
                                    mediaManager.setCurrentVideoQueue(Arrays.asList(response.getItems()));
                                    Intent intent = new Intent(TvApp.getApplication().getCurrentActivity(), activity);
                                    TvApp.getApplication().getCurrentActivity().startActivity(intent);
                                    break;
                                case "Audio":
                                    mediaManager.playNow(Arrays.asList(response.getItems()), startIndex != null ? startIndex.intValue() : 0);
                                    break;

                            }
                        }
                    }
                });

            } else {
                if (command.getItemIds().length > 0) {
                    Timber.i("Playing single item by remote request");
                    Context context = TvApp.getApplication().getCurrentActivity() != null ? TvApp.getApplication().getCurrentActivity() : TvApp.getApplication();
                    PlaybackHelper.retrieveAndPlay(command.getItemIds()[0], false, command.getStartPositionTicks() != null ? command.getStartPositionTicks() : 0, context);
                }
            }
        });
    }

    @Override
    public void onMessageCommand(ApiClient client, MessageCommand command) {
        new Handler(TvApp.getApplication().getMainLooper()).post(() -> Toast.makeText(TvApp.getApplication(), command.getText(), Toast.LENGTH_LONG).show());
    }
}
