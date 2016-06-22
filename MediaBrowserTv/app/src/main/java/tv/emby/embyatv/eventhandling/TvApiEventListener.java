package tv.emby.embyatv.eventhandling;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import java.util.Arrays;
import java.util.Calendar;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.session.BrowseRequest;
import mediabrowser.model.session.GeneralCommand;
import mediabrowser.model.session.PlayRequest;
import mediabrowser.model.session.PlaystateRequest;
import mediabrowser.model.session.SessionInfoDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.playback.PlaybackOverlayActivity;
import tv.emby.embyatv.querying.StdItemQuery;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 2/14/2015.
 */
public class TvApiEventListener extends ApiEventListener {

    @Override
    public void onPlaybackStopped(ApiClient client, SessionInfoDto info) {
        TvApp app = TvApp.getApplication();
        app.getLogger().Debug("Got Playback stopped message from server");
        if (info.getUserId().equals(app.getCurrentUser().getId())) {
            app.setLastPlayback(Calendar.getInstance());
            if (info.getNowPlayingItem() == null) return;
            switch (info.getNowPlayingItem().getType()) {
                case "Movie":
                    TvApp.getApplication().setLastMoviePlayback(Calendar.getInstance());
                    break;
                case "Episode":
                    TvApp.getApplication().setLastTvPlayback(Calendar.getInstance());
                    break;

            }
        }
    }

    @Override
    public void onGeneralCommand(ApiClient client, GeneralCommand command) {
        TvApp.getApplication().getLogger().Info("General command is: "+command.getName());
        switch (command.getName().toLowerCase()) {
            case "mute":
                TvApp.getApplication().setAudioMuted(true);
                break;
            case "unmute":
                TvApp.getApplication().setAudioMuted(false);
                break;
            case "togglemute":
                TvApp.getApplication().setAudioMuted(!TvApp.getApplication().isAudioMuted());
                break;
        }
    }

    @Override
    public void onPlaystateCommand(ApiClient client, PlaystateRequest command) {
        switch (command.getCommand()) {

            case Stop:
                TvApp.getApplication().stopPlayback();
                break;
            case Pause:
                TvApp.getApplication().pausePlayback();
                break;
            case Unpause:
                TvApp.getApplication().unPausePlayback();
                break;
            case NextTrack:
                TvApp.getApplication().playbackNext();
                break;
            case PreviousTrack:
                TvApp.getApplication().playbackPrev();
                break;
            case Seek:
                Long pos = command.getSeekPositionTicks() / 10000;
                TvApp.getApplication().playbackSeek(pos.intValue());
                break;
            case Rewind:
                TvApp.getApplication().playbackJumpBack();
                break;
            case FastForward:
                TvApp.getApplication().playbackJump();
                break;
        }
    }

    @Override
    public void onBrowseCommand(ApiClient client, BrowseRequest command) {
        TvApp.getApplication().getLogger().Debug("Browse command received");
        if (TvApp.getApplication().getCurrentActivity() == null || videoPlaying()) {
            TvApp.getApplication().getLogger().Info("Command ignored due to no activity or playback in progress");
            return;
        }
        client.GetItemAsync(command.getItemId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
            @Override
            public void onResponse(BaseItemDto response) {
                //Create a rowItem and pass to our handler
                ItemLauncher.launch(new BaseRowItem(0, response), null, -1, TvApp.getApplication().getCurrentActivity(), true);
            }
        });
    }

    private boolean videoPlaying() {
        return (TvApp.getApplication().getPlaybackController() != null && (TvApp.getApplication().getPlaybackController().isPlaying() || TvApp.getApplication().getPlaybackController().isPaused()));
    }

    @Override
    public void onPlayCommand(ApiClient client, PlayRequest command) {
        if (videoPlaying()) {
            TvApp.getApplication().getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.showToast(TvApp.getApplication().getCurrentActivity(), TvApp.getApplication().getString(R.string.msg_remote_already_playing));
                }
            });
            return;
        }

        if (command.getItemIds().length > 1) {
            TvApp.getApplication().getLogger().Info("Playing multiple items by remote request");
            if (TvApp.getApplication().getCurrentActivity() == null) {
                TvApp.getApplication().getLogger().Error("No current activity.  Cannot play");
                return;
            }
            StdItemQuery query = new StdItemQuery(new ItemFields[] {ItemFields.MediaSources});
            query.setIds(command.getItemIds());
            TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    if (response.getItems() != null && response.getItems().length > 0) {
                        //peek at first item to see what type it is
                        switch (response.getItems()[0].getMediaType()) {
                            case "Video":
                                MediaManager.setCurrentVideoQueue(Arrays.asList(response.getItems()));
                                Intent intent = new Intent(TvApp.getApplication().getCurrentActivity(), PlaybackOverlayActivity.class);
                                TvApp.getApplication().getCurrentActivity().startActivity(intent);
                                break;
                            case "Audio":
                                MediaManager.playNow(Arrays.asList(response.getItems()));
                                break;

                        }
                    }
                }
            });

        } else {
            if (command.getItemIds().length > 0) {
                TvApp.getApplication().getLogger().Info("Playing single item by remote request");
                Context context = TvApp.getApplication().getCurrentActivity() != null ? TvApp.getApplication().getCurrentActivity() : TvApp.getApplication();
                Utils.retrieveAndPlay(command.getItemIds()[0], false, command.getStartPositionTicks() != null ? command.getStartPositionTicks() : 0, context);
            }
        }
    }
}
