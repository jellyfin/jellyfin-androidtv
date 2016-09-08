package tv.emby.embyatv.browsing;

import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.livetv.RecommendedProgramQuery;
import mediabrowser.model.livetv.RecordingGroupQuery;
import mediabrowser.model.livetv.RecordingQuery;
import mediabrowser.model.querying.ArtistsQuery;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.LatestItemsQuery;
import mediabrowser.model.querying.NextUpQuery;
import mediabrowser.model.querying.PersonsQuery;
import mediabrowser.model.querying.SeasonQuery;
import mediabrowser.model.querying.SimilarItemsQuery;
import mediabrowser.model.querying.UpcomingEpisodesQuery;
import tv.emby.embyatv.model.ChangeTriggerType;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.ViewQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class BrowseRowDef {
    private String headerText;
    private ItemQuery query;
    private NextUpQuery nextUpQuery;
    private UpcomingEpisodesQuery upcomingQuery;
    private SimilarItemsQuery similarQuery;
    private LatestItemsQuery latestItemsQuery;

    private PersonsQuery personsQuery;

    private LiveTvChannelQuery tvChannelQuery;
    private RecommendedProgramQuery programQuery;
    private RecordingQuery recordingQuery;
    private RecordingGroupQuery recordingGroupQuery;

    private ArtistsQuery artistsQuery;
    private SeasonQuery seasonQuery;
    private QueryType queryType;

    private int chunkSize = 0;
    private boolean staticHeight = false;
    private boolean preferParentThumb = false;

    private ChangeTriggerType[] changeTriggers;

    public BrowseRowDef(String header, ItemQuery query, int chunkSize) {
        this(header, query, chunkSize, false);
    }
    public BrowseRowDef(String header, ItemQuery query, int chunkSize, boolean preferParentThumb) {
        this(header, query, chunkSize, preferParentThumb, false);
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
        this.queryType = QueryType.AlbumArtists;
        this.changeTriggers = changeTriggers;

    }

    public BrowseRowDef(String header, NextUpQuery query) {
        headerText = header;
        this.nextUpQuery = query;
        this.queryType = QueryType.NextUp;
    }

    public BrowseRowDef(String header, NextUpQuery query, ChangeTriggerType[] changeTriggers) {
        headerText = header;
        this.nextUpQuery = query;
        this.queryType = QueryType.NextUp;
        this.changeTriggers = changeTriggers;
    }

    public BrowseRowDef(String header, LatestItemsQuery query, ChangeTriggerType[] changeTriggers) {
        headerText = header;
        this.latestItemsQuery = query;
        this.queryType = QueryType.LatestItems;
        this.changeTriggers = changeTriggers;
    }

    public BrowseRowDef(String header, SimilarItemsQuery query) {
        headerText = header;
        this.similarQuery = query;
        this.queryType = QueryType.SimilarSeries;
    }

    public BrowseRowDef(String header, LiveTvChannelQuery query) {
        headerText = header;
        this.tvChannelQuery = query;
        this.queryType = QueryType.LiveTvChannel;
    }

    public BrowseRowDef(String header, RecommendedProgramQuery query) {
        headerText = header;
        this.programQuery = query;
        this.queryType = QueryType.LiveTvProgram;
        this.changeTriggers = new ChangeTriggerType[] {ChangeTriggerType.GuideNeedsLoad};
    }

    public BrowseRowDef(String header, RecordingQuery query) {
        this(header, query, 0);
    }

    public BrowseRowDef(String header, RecordingQuery query, int chunkSize) {
        headerText = header;
        this.recordingQuery = query;
        this.chunkSize = chunkSize;
        this.queryType = QueryType.LiveTvRecording;
    }

    public BrowseRowDef(String header, RecordingGroupQuery query) {
        headerText = header;
        this.recordingGroupQuery = query;
        this.queryType = QueryType.LiveTvRecordingGroup;
    }

    public BrowseRowDef(String header, PersonsQuery query, int chunkSize) {
        headerText = header;
        this.personsQuery = query;
        this.queryType = QueryType.Persons;
        this.chunkSize = chunkSize;
    }

    public BrowseRowDef(String header, SimilarItemsQuery query, QueryType type) {
        headerText = header;
        this.similarQuery = query;
        this.queryType = type;
    }

    public BrowseRowDef(String header, SeasonQuery query) {
        headerText = header;
        this.seasonQuery = query;
        this.queryType = QueryType.Season;
    }

    public BrowseRowDef(String header, UpcomingEpisodesQuery query) {
        headerText = header;
        this.upcomingQuery = query;
        this.queryType = QueryType.Upcoming;
    }

    public BrowseRowDef(String header, ViewQuery query) {
        headerText = header;
        this.staticHeight = true;
        this.queryType = QueryType.Views;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public boolean isStaticHeight() { return staticHeight; }

    public String getHeaderText() {
        return headerText;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public ItemQuery getQuery() {
        return query;
    }

    public NextUpQuery getNextUpQuery() {
        return nextUpQuery;
    }

    public LatestItemsQuery getLatestItemsQuery() { return latestItemsQuery; }

    public SimilarItemsQuery getSimilarQuery() { return similarQuery; }

    public QueryType getQueryType() {
        return queryType;
    }

    public SeasonQuery getSeasonQuery() { return seasonQuery; }

    public UpcomingEpisodesQuery getUpcomingQuery() {
        return upcomingQuery;
    }

    public LiveTvChannelQuery getTvChannelQuery() {
        return tvChannelQuery;
    }

    public RecommendedProgramQuery getProgramQuery() {
        return programQuery;
    }

    public RecordingQuery getRecordingQuery() { return recordingQuery; }

    public boolean getPreferParentThumb() { return preferParentThumb; }

    public PersonsQuery getPersonsQuery() {
        return personsQuery;
    }

    public ArtistsQuery getArtistsQuery() { return artistsQuery; }

    public ChangeTriggerType[] getChangeTriggers() {
        return changeTriggers;
    }

    public RecordingGroupQuery getRecordingGroupQuery() {
        return recordingGroupQuery;
    }
}

