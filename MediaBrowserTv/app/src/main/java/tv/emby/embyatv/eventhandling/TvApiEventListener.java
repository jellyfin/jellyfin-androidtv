package tv.emby.embyatv.eventhandling;

import android.content.Context;

import java.util.Calendar;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.session.BrowseRequest;
import mediabrowser.model.session.GeneralCommand;
import mediabrowser.model.session.PlayRequest;
import mediabrowser.model.session.SessionInfoDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
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
        switch (command.getName().toLowerCase()) {

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
                ItemLauncher.launch(new BaseRowItem(0, response), TvApp.getApplication(), TvApp.getApplication().getCurrentActivity(), true);
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

        } else {
            if (command.getItemIds().length > 0) {
                TvApp.getApplication().getLogger().Info("Playing single item by remote request");
                Context context = TvApp.getApplication().getCurrentActivity() != null ? TvApp.getApplication().getCurrentActivity() : TvApp.getApplication();
                Utils.retrieveAndPlay(command.getItemIds()[0], false, context);
            }
        }
    }
}
