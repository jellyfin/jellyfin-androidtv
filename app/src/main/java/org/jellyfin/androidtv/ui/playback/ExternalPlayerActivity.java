package org.jellyfin.androidtv.ui.playback;

import static org.koin.java.KoinJavaComponent.inject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.FragmentActivity;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.data.compat.PlaybackException;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.data.compat.VideoOptions;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.NextUpBehavior;
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer;
import org.jellyfin.androidtv.ui.playback.nextup.NextUpActivity;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.androidtv.util.apiclient.ReportingHelper;
import org.jellyfin.androidtv.util.profile.ExternalPlayerProfile;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.session.PlayMethod;
import org.koin.java.KoinJavaComponent;

import java.io.File;
import java.util.List;
import java.util.Objects;

import kotlin.Lazy;
import timber.log.Timber;

public class ExternalPlayerActivity extends FragmentActivity {

    List<BaseItemDto> mItemsToPlay;
    int mCurrentNdx = 0;
    StreamInfo mCurrentStreamInfo;

    Handler mHandler = new Handler();
    Runnable mReportLoop;

    long mLastPlayerStart = 0;
    Long mPosition = 0l;
    boolean isLiveTv;
    boolean noPlayerError;

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private Lazy<UserPreferences> userPreferences = inject(UserPreferences.class);
    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private Lazy<org.jellyfin.sdk.api.client.ApiClient> api = inject(org.jellyfin.sdk.api.client.ApiClient.class);
    private Lazy<PlaybackControllerContainer> playbackControllerContainer = inject(PlaybackControllerContainer.class);

    static final int RUNTIME_TICKS_TO_MS = 10000;

    // https://sites.google.com/site/mxvpen/api
    static final String API_MX_TITLE = "title";
    static final String API_MX_SEEK_POSITION = "position";
    static final String API_MX_FILENAME = "filename";
    static final String API_MX_RETURN_RESULT = "return_result";
    static final String API_MX_RESULT_ID = "com.mxtech.intent.result.VIEW";
    static final String API_MX_RESULT_POSITION = "position";
    static final String API_MX_RESULT_END_BY = "end_by";
    static final String API_MX_RESULT_END_BY_USER = "user";
    static final String API_MX_RESULT_END_BY_PLAYBACK_COMPLETION = "playback_completion";

    // https://wiki.videolan.org/Android_Player_Intents/
    static final String API_VLC_TITLE = "title";
    static final String API_VLC_SEEK_POSITION = "position";
    static final String API_VLC_FROM_START = "from_start";
    static final String API_VLC_RESULT_ID = "org.videolan.vlc.player.result";
    static final String API_VLC_RESULT_POSITION = "extra_position";

    // https://www.vimu.tv/player-api
    static final String API_VIMU_TITLE = "forcename";
    static final String API_VIMU_SEEK_POSITION = "startfrom";
    static final String API_VIMU_RESUME = "forceresume";
    static final String API_VIMU_RESULT_POSITION = "position";
    static final int API_VIMU_RESULT_PLAYBACK_COMPLETED = 1;
    static final int API_VIMU_RESULT_PLAYBACK_INTERRUPTED = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backgroundService.getValue().attach(this);

        mItemsToPlay = mediaManager.getValue().getCurrentVideoQueue();

        if (mItemsToPlay == null || mItemsToPlay.size() == 0) {
            Utils.showToast(this, getString(R.string.msg_no_playable_items));
            finish();
            return;
        }

        mPosition = (long) getIntent().getIntExtra("Position", 0);

        launchExternalPlayer(0);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        long playerFinishedTime = System.currentTimeMillis();
        Timber.d("Returned from player, result <%d>, extra data <%s>", resultCode, data);
        BaseItemDto item = mItemsToPlay.get(mCurrentNdx);
        long runtime = item.getRunTimeTicks() != null ? item.getRunTimeTicks() / RUNTIME_TICKS_TO_MS : 0;
        int pos = 0;
        // look for result position in API's
         if (data != null) {
             if (data.hasExtra(API_MX_RESULT_POSITION)) {
                 pos = data.getIntExtra(API_MX_RESULT_POSITION, 0);
             } else if (data.hasExtra(API_VLC_RESULT_POSITION)) {
                 pos = data.getIntExtra(API_VLC_RESULT_POSITION, 0);
             } else if (data.hasExtra(API_VIMU_RESULT_POSITION)) {
                 pos = data.getIntExtra(API_VIMU_RESULT_POSITION, 0);
             }
         }
        // check for playback completion in API's
        if (pos == 0 && data != null) {
            if (Objects.equals(data.getAction(), API_MX_RESULT_ID)) {
                if (resultCode == Activity.RESULT_OK && data.getStringExtra(API_MX_RESULT_END_BY).equals(API_MX_RESULT_END_BY_PLAYBACK_COMPLETION)) {
                    pos = (int) runtime;
                    Timber.i("Detected playback completion for MX player.");
                }
            }
            else if (Objects.equals(data.getAction(), API_VLC_RESULT_ID)) {
                if (resultCode == Activity.RESULT_OK) {
                    pos = (int) runtime;
                    Timber.i("Detected playback completion for VLC player.");
                }
            }
            else if (resultCode == API_VIMU_RESULT_PLAYBACK_COMPLETED) {
                pos = (int) runtime;
                Timber.i("Detected playback completion for Vimu player.");
            }
        }

        if (pos > 0) Timber.i("Player returned position: %d", pos);
        Long reportPos = (long) pos * RUNTIME_TICKS_TO_MS;

        stopReportLoop();
        ReportingHelper.reportStopped(item, mCurrentStreamInfo, reportPos);

        //Check against a total failure (no apps installed)
        if (playerFinishedTime - mLastPlayerStart < 1000) {
            // less than a second - probably no player explain the option
            Timber.i("Playback took less than a second - assuming it failed");
            if (!noPlayerError) handlePlayerError();
            return;
        }

        if (pos == 0) {
            //If item didn't play as long as its duration - confirm we want to mark watched
            if (!isLiveTv && playerFinishedTime - mLastPlayerStart < runtime * .9) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.mark_watched)
                        .setMessage(R.string.mark_watched_message)
                        .setPositiveButton(R.string.lbl_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                markPlayed(mItemsToPlay.get(mCurrentNdx).getId());
                                playNext();
                            }
                        })
                        .setNegativeButton(R.string.lbl_no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!mediaManager.getValue().isVideoQueueModified()) {
                                    mediaManager.getValue().clearVideoQueue();
                                } else {
                                    mItemsToPlay.remove(0);
                                }
                                finish();
                            }
                        })
                        .show();
            } else {
                markPlayed(mItemsToPlay.get(mCurrentNdx).getId());
                playNext();
            }

        } else {
            if (!isLiveTv && pos > (runtime * .9)) {
                playNext();
            } else {
                mItemsToPlay.remove(0);
                finish();
            }
        }
    }

    private void handlePlayerError() {
        if (!mediaManager.getValue().isVideoQueueModified()) mediaManager.getValue().clearVideoQueue();

        new AlertDialog.Builder(this)
                .setTitle(R.string.no_player)
                .setMessage(R.string.no_player_message)
                .setPositiveButton(R.string.btn_got_it, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton(R.string.turn_off, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        userPreferences.getValue().set(UserPreferences.Companion.getVideoPlayer(), PreferredVideoPlayer.AUTO);
                        userPreferences.getValue().set(UserPreferences.Companion.getLiveTvVideoPlayer(), PreferredVideoPlayer.AUTO);
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();

    }

    private void startReportLoop() {
        // FIXME: Don't use the getApplication method..
        PlaybackController playbackController = playbackControllerContainer.getValue().getPlaybackController();
        ReportingHelper.reportProgress(playbackController, mItemsToPlay.get(mCurrentNdx), mCurrentStreamInfo, null, false);
        mReportLoop = new Runnable() {
            @Override
            public void run() {
                ReportingHelper.reportProgress(playbackController, mItemsToPlay.get(mCurrentNdx), mCurrentStreamInfo, mPosition * RUNTIME_TICKS_TO_MS, false);
                mHandler.postDelayed(this, 15000);
            }
        };
        mHandler.postDelayed(mReportLoop, 15000);
    }

    private void stopReportLoop() {
        if (mHandler != null && mReportLoop != null) {
            mHandler.removeCallbacks(mReportLoop);
        }
    }

    protected void markPlayed(String itemId) {
        apiClient.getValue().MarkPlayedAsync(itemId, KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), null, new Response<UserItemDataDto>());
    }

    protected void playNext() {
        mItemsToPlay.remove(0);
        if (mItemsToPlay.size() > 0) {
            if (userPreferences.getValue().get(UserPreferences.Companion.getNextUpBehavior()) != NextUpBehavior.DISABLED) {
                // Set to "modified" so the queue won't be cleared
                mediaManager.getValue().setVideoQueueModified(true);

                Intent intent = new Intent(this, NextUpActivity.class);
                intent.putExtra(NextUpActivity.EXTRA_ID, mItemsToPlay.get(mCurrentNdx).getId());
                intent.putExtra(NextUpActivity.EXTRA_USE_EXTERNAL_PLAYER, true);
                startActivity(intent);
                finishAfterTransition();
            } else {
                mPosition = 0L; // reset for next item
                launchExternalPlayer(0);
            }
        } else {
            finish();
        }
    }

    protected void launchExternalPlayer(int ndx) {
        if (ndx >= mItemsToPlay.size()) {
            Timber.e("Attempt to play index beyond items: %s", ndx);
            finish();
            return;
        }

        //Get playback info for current item
        mCurrentNdx = ndx;
        BaseItemDto item = mItemsToPlay.get(mCurrentNdx);
        isLiveTv = item.getBaseItemType() == BaseItemType.TvChannel;

        if (!isLiveTv && userPreferences.getValue().get(UserPreferences.Companion.getExternalVideoPlayerSendPath())) {
            // Just pass the path directly
            mCurrentStreamInfo = new StreamInfo();
            mCurrentStreamInfo.setPlayMethod(PlayMethod.DirectPlay);
            startExternalActivity(preparePath(item.getPath()), item.getContainer() != null ? item.getContainer() : "*");
        } else {
            //Build options for player
            VideoOptions options = new VideoOptions();
            options.setItemId(item.getId());
            options.setMediaSources(item.getMediaSources());
            options.setMaxBitrate(Utils.getMaxBitrate());
            options.setProfile(new ExternalPlayerProfile());

            // Get playback info for each player and then decide on which one to use
            KoinJavaComponent.<PlaybackManager>get(PlaybackManager.class).getVideoStreamInfo(api.getValue().getDeviceInfo(), options, item.getResumePositionTicks(), apiClient.getValue(), new Response<StreamInfo>() {
                @Override
                public void onResponse(StreamInfo response) {
                    mCurrentStreamInfo = response;

                    //Construct a static URL to sent to player
                    //String url = KoinJavaComponent.<ApiClient>get(ApiClient.class).getApiUrl() + "/videos/" + response.getItemId() + "/stream?static=true&mediaSourceId=" + response.getMediaSourceId();

                    String url = response.getMediaUrl();
                    //And request an activity to play it
                    startExternalActivity(url, response.getMediaSource().getContainer() != null ? response.getMediaSource().getContainer() : "*");
                }

                @Override
                public void onError(Exception exception) {
                    Timber.e(exception, "Error getting playback stream info");
                    if (exception instanceof PlaybackException) {
                        PlaybackException ex = (PlaybackException) exception;
                        switch (ex.getErrorCode()) {
                            case NotAllowed:
                                Utils.showToast(ExternalPlayerActivity.this, getString(R.string.msg_playback_not_allowed));
                                break;
                            case NoCompatibleStream:
                                Utils.showToast(ExternalPlayerActivity.this, getString(R.string.msg_playback_incompatible));
                                break;
                            case RateLimitExceeded:
                                Utils.showToast(ExternalPlayerActivity.this, getString(R.string.msg_playback_restricted));
                                break;
                        }
                    }
                }

            });
        }
    }


    protected String preparePath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty() || rawPath.trim().isEmpty()) return "";
        if (!rawPath.contains("://")) {
            rawPath = rawPath.replace("\\\\",""); // remove UNC prefix if there
            //prefix with smb
            rawPath = "smb://"+rawPath;
        }

        return rawPath.replaceAll("\\\\","/");
    }

    protected void startExternalActivity(String path, String container) {
        if (path == null || path.isEmpty() || path.trim().isEmpty()) {
            Timber.e("Error playback path is null/empty.");
            finish();
            return;
        }
        BaseItemDto item = mItemsToPlay.get(mCurrentNdx);
        if (item == null) {
            Timber.e("Error getting item to play for Ndx: <%d>.", mCurrentNdx);
            finish();
            return;
        }

        Intent external = new Intent(Intent.ACTION_VIEW);
        external.setDataAndType(Uri.parse(path), "video/"+container);

        // build full title string
        String full_title = "";
        Context context = getBaseContext();
        if (context != null) {
            full_title = BaseItemUtils.getDisplayName(item, context);
        }
        if (full_title.isEmpty()) {
            full_title = item.getName();
        }
        if (item.getProductionYear() != null && item.getProductionYear() > 0) {
            full_title += " - ("+ item.getProductionYear().toString() +")";
        }

        //Start player API params
        int pos = mPosition.intValue();
        external.putExtra(API_MX_SEEK_POSITION, pos);
        external.putExtra(API_VIMU_SEEK_POSITION, pos);
        if (pos == 0) {
            external.putExtra(API_VLC_FROM_START, true);
        }
        external.putExtra(API_VIMU_RESUME, false);
        external.putExtra(API_MX_RETURN_RESULT, true);
        if (!full_title.isEmpty()) {
            external.putExtra(API_MX_TITLE, full_title);
            external.putExtra(API_VIMU_TITLE, full_title);
        }
        String filepath = item.getPath();
        if (filepath != null && !filepath.isEmpty()) {
            File file = new File(filepath);
            if (!file.getName().isEmpty()) {
                external.putExtra(API_MX_FILENAME, file.getName());
            }
        }
        //End player API params

        Timber.i("Starting external playback of path: %s and mime: video/%s at position/ms: %s",path,container,mPosition);

        try {
            mLastPlayerStart = System.currentTimeMillis();
            ReportingHelper.reportStart(item, mPosition * RUNTIME_TICKS_TO_MS);
            startReportLoop();
            startActivityForResult(external, 1);
        } catch (ActivityNotFoundException e) {
            noPlayerError = true;
            Timber.e(e, "Error launching external player");
            handlePlayerError();
        }
    }
}
