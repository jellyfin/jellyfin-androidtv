package org.jellyfin.androidtv.ui.playback;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.DynamicsProcessing.Limiter;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.graphics.TypefaceCompat;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ts.TsExtractor;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.ZoomMode;
import org.jellyfin.sdk.api.client.ApiClient;
import org.jellyfin.sdk.model.api.MediaStream;
import org.jellyfin.sdk.model.api.MediaStreamType;
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import timber.log.Timber;

@OptIn(markerClass = UnstableApi.class)
public class VideoManager {
    private ZoomMode mZoomMode;
    private Activity mActivity;
    private Equalizer mEqualizer;
    private DynamicsProcessing mDynamicsProcessing;
    private Limiter mLimiter;
    private PlaybackControllerNotifiable mPlaybackControllerNotifiable;
    private PlaybackOverlayFragmentHelper _helper;
    public ExoPlayer mExoPlayer;
    private PlayerView mExoPlayerView;
    private Handler mHandler = new Handler();

    private long mMetaDuration = -1;
    private long lastExoPlayerPosition = -1;
    private boolean nightModeEnabled;

    public boolean isContracted = false;

    private final UserPreferences userPreferences = KoinJavaComponent.get(UserPreferences.class);
    private final HttpDataSource.Factory exoPlayerHttpDataSourceFactory = KoinJavaComponent.get(HttpDataSource.Factory.class);

    public VideoManager(@NonNull Activity activity, @NonNull View view, @NonNull PlaybackOverlayFragmentHelper helper) {
        mActivity = activity;
        _helper = helper;
        nightModeEnabled = userPreferences.get(UserPreferences.Companion.getAudioNightMode());

        mExoPlayer = configureExoplayerBuilder(activity).build();

        if (userPreferences.get(UserPreferences.Companion.getDebuggingEnabled())) {
            mExoPlayer.addAnalyticsListener(new EventLogger());
        }

        // Volume normalisation (audio night mode).
        if (nightModeEnabled) {
            mExoPlayer.addAnalyticsListener(new AnalyticsListener() {
                @Override
                public void onAudioSessionIdChanged(AnalyticsListener.EventTime eventTime, int audioSessionId) {
                    VideoManagerHelperKt.applyAudioNightmode(audioSessionId);
                }
            });
        }

        mExoPlayerView = view.findViewById(R.id.exoPlayerView);
        mExoPlayerView.setPlayer(mExoPlayer);
        int strokeColor = userPreferences.get(UserPreferences.Companion.getSubtitleTextStrokeColor()).intValue();
        int textWeight = userPreferences.get(UserPreferences.Companion.getSubtitlesTextWeight());
        CaptionStyleCompat subtitleStyle = new CaptionStyleCompat(
                userPreferences.get(UserPreferences.Companion.getSubtitlesTextColor()).intValue(),
                userPreferences.get(UserPreferences.Companion.getSubtitlesBackgroundColor()).intValue(),
                Color.TRANSPARENT,
                Color.alpha(strokeColor) == 0 ? CaptionStyleCompat.EDGE_TYPE_NONE : CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                strokeColor,
                TypefaceCompat.create(activity, Typeface.DEFAULT, textWeight, false)
        );
        mExoPlayerView.getSubtitleView().setFractionalTextSize(0.0533f * userPreferences.get(UserPreferences.Companion.getSubtitlesTextSize()));
        mExoPlayerView.getSubtitleView().setBottomPaddingFraction(userPreferences.get(UserPreferences.Companion.getSubtitlesOffsetPosition()));
        mExoPlayerView.getSubtitleView().setStyle(subtitleStyle);

        mExoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Timber.e("***** Got error from player");
                if (mPlaybackControllerNotifiable != null) mPlaybackControllerNotifiable.onError();
                stopProgressLoop();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    if (mPlaybackControllerNotifiable != null) mPlaybackControllerNotifiable.onPrepared();
                    startProgressLoop();
                    _helper.setScreensaverLock(true);
                } else {
                    stopProgressLoop();
                    _helper.setScreensaverLock(false);
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    Timber.d("Player is buffering");
                }

                if (playbackState == Player.STATE_ENDED) {
                    if (mPlaybackControllerNotifiable != null) mPlaybackControllerNotifiable.onCompletion();
                    stopProgressLoop();
                }
            }

            @Override
            public void onPlaybackParametersChanged(@NonNull PlaybackParameters playbackParameters) {
                if (mPlaybackControllerNotifiable != null) {
                    mPlaybackControllerNotifiable.onPlaybackSpeedChange(playbackParameters.speed);
                }
            }

            @Override
            public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, int reason) {
                // discontinuity for reason internal usually indicates an error, and that the player will reset to its default timestamp
                if (reason == Player.DISCONTINUITY_REASON_INTERNAL) {
                    Timber.i("Caught player discontinuity (reason internal) - oldPos: %s newPos: %s", oldPosition.positionMs, newPosition.positionMs);
                }
            }

            @Override
            public void onTimelineChanged(@NonNull Timeline timeline, int reason) {
                Timber.d("Caught player timeline change - reason: %s", reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED ? "PLAYLIST_CHANGED" : "SOURCE_UPDATE");
            }

            @Override
            public void onTracksChanged(Tracks tracks) {
                Timber.d("Tracks changed");
            }
        });
    }

    public void subscribe(@NonNull PlaybackControllerNotifiable notifier) {
        mPlaybackControllerNotifiable = notifier;
    }

    private int determineExoPlayerExtensionRendererMode() {
        if (userPreferences.get(UserPreferences.Companion.getPreferExoPlayerFfmpeg())) {
            return DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;
        }

        return DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
    }

    /**
     * Configures Exoplayer for video playback. Initially we try with core decoders, but allow
     * ExoPlayer to silently fallback to software renderers.
     *
     * @param context The associated context
     * @return A configured builder for Exoplayer
     */
    private ExoPlayer.Builder configureExoplayerBuilder(Context context) {
        ExoPlayer.Builder exoPlayerBuilder = new ExoPlayer.Builder(context);
        DefaultRenderersFactory defaultRendererFactory = new DefaultRenderersFactory(context);
        defaultRendererFactory.setEnableDecoderFallback(true);
        defaultRendererFactory.setExtensionRendererMode(determineExoPlayerExtensionRendererMode());

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setAudioOffloadPreferences(new TrackSelectionParameters.AudioOffloadPreferences.Builder()
                        .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                        .build()
                )
                .setAllowInvalidateSelectionsOnRendererCapabilitiesChange(true)
                .build()
        );
        exoPlayerBuilder.setTrackSelector(trackSelector);

        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory().setTsExtractorTimestampSearchBytes(TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES * 3);
        extractorsFactory.setConstantBitrateSeekingEnabled(true);
        extractorsFactory.setConstantBitrateSeekingAlwaysEnabled(true);
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context, exoPlayerHttpDataSourceFactory);
        exoPlayerBuilder.setRenderersFactory(defaultRendererFactory);
        exoPlayerBuilder.setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory));

        return exoPlayerBuilder;
    }

    public boolean isInitialized() {
        return mExoPlayer != null;
    }

    public @NonNull ZoomMode getZoomMode() {
        return mZoomMode;
    }

    public void setZoom(@NonNull ZoomMode mode) {
        mZoomMode = mode;
        switch (mode) {
            case FIT:
                mExoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                break;
            case AUTO_CROP:
                mExoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                break;
            case STRETCH:
                mExoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
                break;
        }
    }

    public void setMetaDuration(long duration) {
        mMetaDuration = duration;
    }

    public long getDuration() {
        return isInitialized() && mExoPlayer.getDuration() > 0 ? mExoPlayer.getDuration() : mMetaDuration;
    }

    public long getBufferedPosition() {
        if (!isInitialized())
            return -1;

        long bufferedPosition = mExoPlayer.getBufferedPosition();

        if (bufferedPosition > -1 && bufferedPosition < getDuration()) {
            return bufferedPosition;
        }
        return -1;
    }

    public long getCurrentPosition() {
        if (mExoPlayer == null || !isPlaying()) {
            return lastExoPlayerPosition == -1 ? 0 : lastExoPlayerPosition;
        } else {
            long mExoPlayerCurrentPosition = mExoPlayer.getCurrentPosition();
            lastExoPlayerPosition = mExoPlayerCurrentPosition;
            return mExoPlayerCurrentPosition;
        }
    }

    public boolean isPlaying() {
        return mExoPlayer.isPlaying();
    }

    public void start() {
        if (mExoPlayer == null) {
            Timber.e("mExoPlayer should not be null!!");
            _helper.getFragment().closePlayer();
            return;
        }
        mExoPlayer.setPlayWhenReady(true);
        normalWidth = mExoPlayerView.getLayoutParams().width;
        normalHeight = mExoPlayerView.getLayoutParams().height;
    }

    public void play() {
        mExoPlayer.setPlayWhenReady(true);
    }

    public void pause() {
        mExoPlayer.setPlayWhenReady(false);
    }

    public void stopPlayback() {
        if (mExoPlayer != null) {
            mExoPlayer.stop();

            mExoPlayer.setTrackSelectionParameters(mExoPlayer.getTrackSelectionParameters()
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .build());
        }

        stopProgressLoop();
    }

    public boolean isSeekable() {
        if (!isInitialized())
            return false;

        boolean canSeek = mExoPlayer.isCurrentMediaItemSeekable();
        Timber.d("current media item is%s seekable", canSeek ? "" : " not");
        return canSeek;
    }

    public long seekTo(long pos) {
        if (!isInitialized())
            return -1;

        Timber.i("Exo length in seek is: %d", getDuration());
        mExoPlayer.seekTo(pos);
        return pos;
    }

    private int getSubtitleSelectionFlags(MediaStream mediaStream) {
        int flags = 0;
        if (mediaStream.isDefault()) flags &= C.SELECTION_FLAG_DEFAULT;
        if (mediaStream.isForced()) flags &= C.SELECTION_FLAG_FORCED;
        return flags;
    }

    public void setMediaStreamInfo(ApiClient api, StreamInfo streamInfo) {
        String path = streamInfo.getMediaUrl();
        if (path == null) {
            Timber.w("Video path is null cannot continue");
            return;
        }
        Timber.i("Video path set to: %s", path);

        try {
            // Add external subtitles
            List<MediaItem.SubtitleConfiguration> subtitleConfigurations = new ArrayList<>();
            for (MediaStream mediaStream : streamInfo.getMediaSource().getMediaStreams()) {
                if (mediaStream.getType() != MediaStreamType.SUBTITLE) continue;

                if (mediaStream.getDeliveryMethod() == SubtitleDeliveryMethod.EXTERNAL) {
                    Uri subtitleUri = Uri.parse(api.createUrl(mediaStream.getDeliveryUrl(), Collections.emptyMap(), Collections.emptyMap(), true));
                    MediaItem.SubtitleConfiguration subtitleConfiguration = new MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                            .setId("JF_EXTERNAL:" + String.valueOf(mediaStream.getIndex()))
                            .setMimeType(VideoManagerHelperKt.getSubtitleMediaStreamCodec(mediaStream))
                            .setLanguage(mediaStream.getLanguage())
                            .setLabel(mediaStream.getDisplayTitle())
                            .setSelectionFlags(getSubtitleSelectionFlags(mediaStream))
                            .build();
                    Timber.i("Adding subtitle track %s of type %s", subtitleConfiguration.uri, subtitleConfiguration.mimeType);
                    subtitleConfigurations.add(subtitleConfiguration);
                }
            }

            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(Uri.parse(path))
                    .setSubtitleConfigurations(subtitleConfigurations)
                    .build();

            mExoPlayer.setMediaItem(mediaItem);
            mExoPlayer.prepare();
        } catch (IllegalStateException e) {
            Timber.e(e, "Unable to set video path.  Probably backing out.");
        }
    }

    private int offsetStreamIndex(int index, boolean adjustByAdding, boolean indexStartsAtOne, @Nullable List<org.jellyfin.sdk.model.api.MediaStream> allStreams) {
        if (index < 0 || allStreams == null)
            return -1;

        // translate player track index to/from Jellyfin MediaStream index to account for external tracks
        // being in the MediaStream tracks list but not in a player's track list
        //
        // use adjustByAdding=true to translate player-id -> MediaStream-id, false for the other direction
        //
        // use indexStartsAtOne=true when the player's tracks list uses indexes/IDs starting at 1
        // MediaStream indexes/IDs start at 0

        for (org.jellyfin.sdk.model.api.MediaStream stream : allStreams) {
            if (!stream.isExternal())
                break;
            index += adjustByAdding ? 1 : -1;
        }
        index += indexStartsAtOne ? (adjustByAdding ? -1 : 1) : 0;

        return index < 0 || index > allStreams.size() ? -1 : index;
    }

    public int getExoPlayerTrack(@Nullable org.jellyfin.sdk.model.api.MediaStreamType streamType, @Nullable List<org.jellyfin.sdk.model.api.MediaStream> allStreams) {
        if (!isInitialized() || streamType == null || allStreams == null)
            return -1;
        if (streamType != org.jellyfin.sdk.model.api.MediaStreamType.SUBTITLE && streamType != org.jellyfin.sdk.model.api.MediaStreamType.AUDIO)
            return -1;

        int chosenTrackType = streamType == org.jellyfin.sdk.model.api.MediaStreamType.SUBTITLE ? C.TRACK_TYPE_TEXT : C.TRACK_TYPE_AUDIO;

        int matchedIndex = -2;
        Tracks exoTracks = mExoPlayer.getCurrentTracks();
        for (Tracks.Group groupInfo : exoTracks.getGroups()) {
            if (matchedIndex > -2)
                break;
            // Group level information.
            @C.TrackType int trackType = groupInfo.getType();
            TrackGroup group = groupInfo.getMediaTrackGroup();
            for (int i = 0; i < group.length; i++) {
                // Individual track information.
                Format trackFormat = group.getFormat(i);
                if (trackType == chosenTrackType) {
                    if (groupInfo.isTrackSelected(i)) {
                        // we found the track, set to -1 first to handle failed int parsing
                        matchedIndex = -1;
                        if (trackFormat.id != null) {
                            int id;
                            try {
                                if (trackFormat.id.contains(":")) {
                                    id = Integer.parseInt(trackFormat.id.split(":")[1]);
                                } else {
                                    id = Integer.parseInt(trackFormat.id);
                                }
                            } catch (NumberFormatException e) {
                                Timber.w("failed to parse track ID [%s]", trackFormat.id);
                                break;
                            }
                            matchedIndex = id;
                        }
                        break;
                    }
                }
            }
        }

        // offset the stream index to account for external streams
        int exoTrackID = offsetStreamIndex(matchedIndex, true, true, allStreams);
        if (exoTrackID < 0)
            return -1;

        Timber.d("re-retrieved exoplayer track index %s", exoTrackID);
        return exoTrackID;
    }

    public boolean setExoPlayerTrack(int index, @Nullable org.jellyfin.sdk.model.api.MediaStreamType streamType, @Nullable List<org.jellyfin.sdk.model.api.MediaStream> allStreams) {
        if (!isInitialized() || allStreams == null || allStreams.isEmpty() || streamType != org.jellyfin.sdk.model.api.MediaStreamType.SUBTITLE && streamType != org.jellyfin.sdk.model.api.MediaStreamType.AUDIO)
            return false;

        int chosenTrackType = streamType == org.jellyfin.sdk.model.api.MediaStreamType.SUBTITLE ? C.TRACK_TYPE_TEXT : C.TRACK_TYPE_AUDIO;

        // Make sure the index is present
        Optional<MediaStream> candidateOptional = allStreams.stream().filter(stream -> stream.getIndex() == index && !stream.isExternal() && stream.getType() == streamType).findFirst();
        if (!candidateOptional.isPresent()) return false;

        int exoTrackID = offsetStreamIndex(index, false, true, allStreams);
        if (exoTrackID < 0)
            return false;

        // print the streams for debugging
        for (MediaStream stream : allStreams) {
            Timber.d("MediaStream track %s type %s label %s codec %s isExternal %s", stream.getIndex(), stream.getType(), stream.getTitle(), stream.getCodec(), stream.isExternal());
        }

        // design choices for exoplayer track selection overrides:
        // * build upon the prior parameters so we can mix overrides of different track types without erasing priors
        //
        // * for subtitles (not currently used) - use setDisabledTrackTypes to disable or enable exoplayer handing subtitles
        //   if we want most formats to be handled by the external subtitle handler (which has adjustable size, background), we leave sub track selection disabled
        //   if we decide to use exoplayer to render a specific subtitle format, allow subtitle track selection and restrict selection to the chosen group

        Tracks exoTracks = mExoPlayer.getCurrentTracks();
        TrackGroup matchedGroup = null;
        for (Tracks.Group groupInfo : exoTracks.getGroups()) {
            // Group level information.
            @C.TrackType int trackType = groupInfo.getType();
            TrackGroup group = groupInfo.getMediaTrackGroup();
            for (int i = 0; i < group.length; i++) {
                // Individual track information.
                boolean isSupported = groupInfo.isTrackSupported(i);
                boolean isSelected = groupInfo.isTrackSelected(i);
                Format trackFormat = group.getFormat(i);

                Timber.i("track %s group %s/%s trackType %s label %s mime %s isSelected %s isSupported %s",
                        trackFormat.id, i + 1, group.length, trackType, trackFormat.label, trackFormat.sampleMimeType, isSelected, isSupported);

                if (trackType != chosenTrackType || trackFormat.id == null)
                    continue;

                int id;
                try {
                    if (trackFormat.id.contains(":")) {
                        id = Integer.parseInt(trackFormat.id.split(":")[1]);
                    } else {
                        id = Integer.parseInt(trackFormat.id);
                    }
                    if (id != exoTrackID)
                        continue;
                } catch (NumberFormatException e) {
                    Timber.w("failed to parse track ID [%s]", trackFormat.id);
                    continue;
                }

                if (!groupInfo.isTrackSupported(i)) {
                    Timber.d("track is not compatible");
                    return false;
                }

                if (groupInfo.isTrackSelected(i)) {
                    Timber.d("track is already selected");
                    return true;
                }

                Timber.i("matched exoplayer track %s to mediaStream track %s", trackFormat.id, index);
                matchedGroup = group;
            }
        }

        if (matchedGroup == null)
            return false;

        try {
            TrackSelectionParameters.Builder mExoPlayerSelectionParams = mExoPlayer.getTrackSelectionParameters().buildUpon();
            mExoPlayerSelectionParams.setOverrideForType(new TrackSelectionOverride(matchedGroup, 0));
            mExoPlayer.setTrackSelectionParameters(mExoPlayerSelectionParams.build());
        } catch (Exception e) {
            Timber.w("Error setting track selection");
            return false;
        }
        return true;
    }

    public float getPlaybackSpeed() {
        if (!isInitialized()) {
            return 1.0f;
        } else {
            return mExoPlayer.getPlaybackParameters().speed;
        }
    }

    public void setPlaybackSpeed(float speed) {
        if (speed < 0.25) {
            Timber.w("Invalid playback speed requested: %f", speed);
            return;
        }
        Timber.d("Setting playback speed: %f", speed);

        mExoPlayer.setPlaybackParameters(new PlaybackParameters(speed));
    }

    public void destroy() {
        mPlaybackControllerNotifiable = null;
        stopPlayback();
        releasePlayer();
    }

    private void releasePlayer() {
        if (mExoPlayer != null) {
            mExoPlayerView.setPlayer(null);
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }

    int normalWidth;
    int normalHeight;

    public void contractVideo(int height) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mExoPlayerView.getLayoutParams();
        if (isContracted) return;

        int sw = mActivity.getWindow().getDecorView().getWidth();
        int sh = mActivity.getWindow().getDecorView().getHeight();
        float ar = (float) sw / sh;
        lp.height = height;
        lp.width = (int) Math.ceil(height * ar);
        lp.rightMargin = ((lp.width - normalWidth) / 2) - 110;
        lp.bottomMargin = ((lp.height - normalHeight) / 2) - 50;

        mExoPlayerView.setLayoutParams(lp);
        mExoPlayerView.invalidate();

        isContracted = true;
    }

    public void setVideoFullSize(boolean force) {
        if (normalHeight == 0) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mExoPlayerView.getLayoutParams();
        if (force) {
            lp.height = -1;
            lp.width = -1;
        } else {
            lp.height = normalHeight;
            lp.width = normalWidth;
        }

        lp.rightMargin = 0;
        lp.bottomMargin = 0;
        mExoPlayerView.setLayoutParams(lp);
        mExoPlayerView.invalidate();

        isContracted = false;
    }

    private Runnable progressLoop;

    private void startProgressLoop() {
        stopProgressLoop();
        progressLoop = new Runnable() {
            @Override
            public void run() {
                if (mPlaybackControllerNotifiable != null) mPlaybackControllerNotifiable.onProgress();
                mHandler.postDelayed(this, 500);
            }
        };
        mHandler.post(progressLoop);
    }

    private void stopProgressLoop() {
        if (progressLoop != null) {
            mHandler.removeCallbacks(progressLoop);
        }
    }
}
