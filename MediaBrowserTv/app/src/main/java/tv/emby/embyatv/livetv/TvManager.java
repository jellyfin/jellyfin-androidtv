package tv.emby.embyatv.livetv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.results.ChannelInfoDtoResult;
import tv.emby.embyatv.TvApp;

/**
 * Created by Eric on 9/4/2015.
 */
public class TvManager {
    private static List<ChannelInfoDto> allChannels;

    public static String getLastLiveTvChannel() {
        return TvApp.getApplication().getSystemPrefs().getString("sys_pref_last_tv_channel", null);
    }

    public static void setLastLiveTvChannel(String id) {
        TvApp.getApplication().getSystemPrefs().edit().putString("sys_pref_last_tv_channel", id).commit();
    }

    public static List<ChannelInfoDto> getAllChannels() {
        return allChannels;
    }

    public static int getAllChannelsIndex(String id) {
        for (int i = 0; i < allChannels.size(); i++) {
            if (allChannels.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    public static void loadAllChannels(final Response<Integer> outerResponse) {
        //Get channels
        LiveTvChannelQuery query = new LiveTvChannelQuery();
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setEnableFavoriteSorting(true);
        TvApp.getApplication().getLogger().Debug("*** About to load channels");
        TvApp.getApplication().getApiClient().GetLiveTvChannelsAsync(query, new Response<ChannelInfoDtoResult>() {
            @Override
            public void onResponse(ChannelInfoDtoResult response) {
                TvApp.getApplication().getLogger().Debug("*** channel query response");
                allChannels = new ArrayList<>();
                if (response.getTotalRecordCount() > 0) {
                    allChannels.addAll(Arrays.asList(response.getItems()));
                    //fake more channels
//                    mAllChannels.addAll(Arrays.asList(response.getItems()));
//                    mAllChannels.addAll(Arrays.asList(response.getItems()));
//                    mAllChannels.addAll(Arrays.asList(response.getItems()));
//                    mAllChannels.addAll(Arrays.asList(response.getItems()));
                    //

                }
                int ndx = 0;
                String lastTvChannelId = getLastLiveTvChannel();
                if (lastTvChannelId != null) {
                    ndx = getAllChannelsIndex(lastTvChannelId);
                }

                outerResponse.onResponse(ndx);
            }
        });

    }

}
