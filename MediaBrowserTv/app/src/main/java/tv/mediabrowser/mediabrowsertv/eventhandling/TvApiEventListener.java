package tv.mediabrowser.mediabrowsertv.eventhandling;

import java.util.Calendar;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.model.session.SessionInfoDto;
import tv.mediabrowser.mediabrowsertv.TvApp;

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
}
