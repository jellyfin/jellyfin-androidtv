package org.jellyfin.androidtv.ui.browsing;

import org.jellyfin.androidtv.constant.ChangeTriggerType;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.querying.AlbumArtistsQuery;
import org.jellyfin.androidtv.data.querying.GetSeriesTimersRequest;
import org.jellyfin.androidtv.data.querying.GetSpecialsRequest;
import org.jellyfin.androidtv.data.querying.GetUserViewsRequest;
import org.jellyfin.apiclient.model.querying.ArtistsQuery;
import org.jellyfin.apiclient.model.querying.ItemQuery;
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest;
import org.jellyfin.sdk.model.api.request.GetLiveTvChannelsRequest;
import org.jellyfin.sdk.model.api.request.GetNextUpRequest;
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest;
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest;
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest;
import org.jellyfin.sdk.model.api.request.GetSimilarItemsRequest;

public class BrowseRowDef {
    private String headerText;
    private ItemQuery query;
    private GetNextUpRequest nextUpQuery;
    private GetSimilarItemsRequest similarQuery;
    private GetLatestMediaRequest latestItemsQuery;
    private GetLiveTvChannelsRequest tvChannelQuery;
    private GetRecommendedProgramsRequest programQuery;
    private GetRecordingsRequest recordingQuery;
    private GetSeriesTimersRequest seriesTimerQuery;

    private ArtistsQuery artistsQuery;
    private AlbumArtistsQuery albumArtistsQuery;
    private GetResumeItemsRequest resumeQuery;
    private GetSpecialsRequest specialsQuery;
    private QueryType queryType;

    private int chunkSize = 0;
    private boolean staticHeight = false;
    private boolean preferParentThumb = false;

    private ChangeTriggerType[] changeTriggers;

    public BrowseRowDef(String header, ItemQuery query, int chunkSize) {
        this(header, query, chunkSize, false, false);
    }

    public BrowseRowDef(String header, ItemQuery query, int chunkSize, boolean preferParentThumb, boolean staticHeight) {
        headerText = header;
        this.query = query;
        this.chunkSize = chunkSize;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        this.queryType = QueryType.Items;
    }

    public BrowseRowDef(String header, ItemQuery query, int chunkSize, ChangeTriggerType[] changeTriggers) {
        this(header, query, chunkSize, false, false, changeTriggers);
    }

    public BrowseRowDef(String header, ItemQuery query, int chunkSize, boolean preferParentThumb, boolean staticHeight, ChangeTriggerType[] changeTriggers) {
        this(header,query,chunkSize,preferParentThumb,staticHeight,changeTriggers,QueryType.Items);
    }

    public BrowseRowDef(String header, ItemQuery query, int chunkSize, boolean preferParentThumb, boolean staticHeight, ChangeTriggerType[] changeTriggers, QueryType queryType) {
        headerText = header;
        this.query = query;
        this.chunkSize = chunkSize;
        this.queryType = queryType;
        this.staticHeight = staticHeight;
        this.preferParentThumb = preferParentThumb;
        this.changeTriggers = changeTriggers;
    }

    public BrowseRowDef(String header, ArtistsQuery query, int chunkSize, ChangeTriggerType[] changeTriggers) {
        headerText = header;
        this.artistsQuery = query;
        this.chunkSize = chunkSize;
        this.queryType = QueryType.Artists;
        this.changeTriggers = changeTriggers;
    }

    public BrowseRowDef(String header, AlbumArtistsQuery query, int chunkSize, ChangeTriggerType[] changeTriggers) {
        headerText = header;
        this.albumArtistsQuery = query;
        this.chunkSize = chunkSize;
        this.queryType = QueryType.AlbumArtists;
        this.changeTriggers = changeTriggers;
    }

    public BrowseRowDef(String header, GetSeriesTimersRequest query) {
        headerText = header;
        this.seriesTimerQuery = query;
        this.staticHeight = true;
        this.queryType = QueryType.SeriesTimer;
    }

    public BrowseRowDef(String header, GetNextUpRequest query, ChangeTriggerType[] changeTriggers) {
        headerText = header;
        this.nextUpQuery = query;
        this.queryType = QueryType.NextUp;
        this.staticHeight = true;
        this.changeTriggers = changeTriggers;
    }

    public BrowseRowDef(String header, GetLatestMediaRequest query, ChangeTriggerType[] changeTriggers) {
        headerText = header;
        this.latestItemsQuery = query;
        this.queryType = QueryType.LatestItems;
        this.staticHeight = true;
        this.changeTriggers = changeTriggers;
    }

    public BrowseRowDef(String header, GetLiveTvChannelsRequest query) {
        headerText = header;
        this.tvChannelQuery = query;
        this.queryType = QueryType.LiveTvChannel;
    }

    public BrowseRowDef(String header, GetRecommendedProgramsRequest query) {
        headerText = header;
        this.programQuery = query;
        this.queryType = QueryType.LiveTvProgram;
        this.changeTriggers = new ChangeTriggerType[] {ChangeTriggerType.GuideNeedsLoad};
    }

    public BrowseRowDef(String header, GetRecordingsRequest query) {
        this(header, query, 0);
    }

    public BrowseRowDef(String header, GetRecordingsRequest query, int chunkSize) {
        headerText = header;
        this.recordingQuery = query;
        this.chunkSize = chunkSize;
        this.queryType = QueryType.LiveTvRecording;
    }

    public BrowseRowDef(String header, GetSimilarItemsRequest query, QueryType type) {
        headerText = header;
        this.similarQuery = query;
        this.queryType = type;
    }

    public BrowseRowDef(String header, GetUserViewsRequest query) {
        headerText = header;
        this.staticHeight = true;
        this.queryType = QueryType.Views;
    }

    public BrowseRowDef(String header, GetResumeItemsRequest query, int chunkSize, boolean preferParentThumb, boolean staticHeight, ChangeTriggerType[] changeTriggers) {
        headerText = header;
        this.resumeQuery = query;
        this.chunkSize = chunkSize;
        this.queryType = QueryType.Resume;
        this.staticHeight = staticHeight;
        this.preferParentThumb = preferParentThumb;
        this.changeTriggers = changeTriggers;
    }

    public BrowseRowDef(String header, GetSpecialsRequest query) {
        headerText = header;
        this.specialsQuery = query;
        this.queryType = QueryType.Specials;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public boolean isStaticHeight() { return staticHeight; }

    public String getHeaderText() {
        return headerText;
    }

    public ItemQuery getQuery() {
        return query;
    }

    public GetNextUpRequest getNextUpQuery() {
        return nextUpQuery;
    }

    public GetLatestMediaRequest getLatestItemsQuery() { return latestItemsQuery; }

    public GetSimilarItemsRequest getSimilarQuery() { return similarQuery; }

    public QueryType getQueryType() {
        return queryType;
    }

    public GetLiveTvChannelsRequest getTvChannelQuery() {
        return tvChannelQuery;
    }

    public GetRecommendedProgramsRequest getProgramQuery() {
        return programQuery;
    }

    public GetRecordingsRequest getRecordingQuery() { return recordingQuery; }

    public boolean getPreferParentThumb() { return preferParentThumb; }

    public ArtistsQuery getArtistsQuery() { return artistsQuery; }
    public AlbumArtistsQuery getAlbumArtistsQuery() { return albumArtistsQuery; }

    public GetSeriesTimersRequest getSeriesTimerQuery() { return seriesTimerQuery; }

    public GetResumeItemsRequest getResumeQuery() { return resumeQuery; }

    public GetSpecialsRequest getSpecialsQuery() { return specialsQuery; }

    public ChangeTriggerType[] getChangeTriggers() {
        return changeTriggers;
    }
}
