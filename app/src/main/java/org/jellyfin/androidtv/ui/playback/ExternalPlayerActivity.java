package org.jellyfin.androidtv.ui.playback;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.FragmentActivity;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.data.compat.PlaybackException;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.data.compat.VideoOptions;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer;
import org.jellyfin.androidtv.ui.playback.nextup.NextUpActivity;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.ReportingHelper;
import org.jellyfin.androidtv.util.profile.ExternalPlayerProfile;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.session.PlayMethod;

import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.get;
import static org.koin.java.KoinJavaComponent.inject;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backgroundService.getValue().attach(this);

        mItemsToPlay = mediaManager.getValue().getCurrentVideoQueue();

        if (mItemsToPlay == null || mItemsToPlay.size() == 0) {
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_no_playable_items));
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
        Timber.d("Returned from player... %d", resultCode);
        //MX Player will return position
        int pos = data != null ? data.getIntExtra("position", 0) : 0;
        if (pos > 0) Timber.i("Player returned position: %d", pos);
        Long reportPos = (long) pos * 10000;

        stopReportLoop();
        ReportingHelper.reportStopped(mItemsToPlay.get(mCurrentNdx), mCurrentStreamInfo, reportPos);

        //Check against a total failure (no apps installed)
        if (playerFinishedTime - mLastPlayerStart < 1000) {
            // less than a second - probably no player explain the option
            Timber.i("Playback took less than a second - assuming it failed");
            if (!noPlayerError) handlePlayerError();
            return;
        }

        long runtime = mItemsToPlay.get(mCurrentNdx).getRunTimeTicks() != null ? mItemsToPlay.get(mCurrentNdx).getRunTimeTicks() / 10000 : 0;
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
        ReportingHelper.reportProgress(mItemsToPlay.get(mCurrentNdx), mCurrentStreamInfo, null, false);
        mReportLoop = new Runnable() {
            @Override
            public void run() {
                ReportingHelper.reportProgress(mItemsToPlay.get(mCurrentNdx), mCurrentStreamInfo, mPosition, false);
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
        apiClient.getValue().MarkPlayedAsync(itemId, TvApp.getApplication().getCurrentUser().getId(), null, new Response<UserItemDataDto>());
    }

    protected void playNext() {
        mItemsToPlay.remove(0);
        if (mItemsToPlay.size() > 0) {
            if (userPreferences.getValue().get(UserPreferences.Companion.getNextUpEnabled())) {
                // Set to "modified" so the queue won't be cleared
                mediaManager.getValue().setVideoQueueModified(true);

                Intent intent = new Intent(this, NextUpActivity.class);
                intent.putExtra("id", mItemsToPlay.get(mCurrentNdx).getId());
                startActivity(intent);
                finish();
            } else {
                launchExternalPlayer(0);
            }
        } else {
            finish();
        }
    }

    protected void launchExternalPlayer(int ndx) {
        if (ndx >= mItemsToPlay.size()) {
            Timber.e("Attempt to play index beyond items: %s",ndx);
        } else {
            //Get playback info for current item
            mCurrentNdx = ndx;
            final BaseItemDto item = mItemsToPlay.get(mCurrentNdx);
            isLiveTv = item.getBaseItemType() == BaseItemType.TvChannel;

            if (!isLiveTv && userPreferences.getValue().get(UserPreferences.Companion.getExternalVideoPlayerSendPath())) {
                // Just pass the path directly
                mCurrentStreamInfo = new StreamInfo();
                mCurrentStreamInfo.setPlayMethod(PlayMethod.DirectPlay);
                startExternalActivity(preparePath(item.getPath()), item.getContainer() != null ? item.getContainer() : "*");
            } else {
                //Build options for player
                VideoOptions options = new VideoOptions();
                options.setDeviceId(apiClient.getValue().getDeviceId());
                options.setItemId(item.getId());
                options.setMediaSources(item.getMediaSources());
                options.setMaxBitrate(Utils.getMaxBitrate());
                options.setProfile(new ExternalPlayerProfile());

                // Get playback info for each player and then decide on which one to use
                get(PlaybackManager.class).getVideoStreamInfo(apiClient.getValue().getServerInfo().getId(), options, item.getResumePositionTicks(), false, apiClient.getValue(), new Response<StreamInfo>() {
                    @Override
                    public void onResponse(StreamInfo response) {
                        mCurrentStreamInfo = response;

                        //Construct a static URL to sent to player
                        //String url = get(ApiClient.class).getApiUrl() + "/videos/" + response.getItemId() + "/stream?static=true&mediaSourceId=" + response.getMediaSourceId();

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
                                    Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_playback_not_allowed));
                                    break;
                                case NoCompatibleStream:
                                    Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_playback_incompatible));
                                    break;
                                case RateLimitExceeded:
                                    Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_playback_restricted));
                                    break;
                            }
                        }
                    }

                });
            }
        }
    }

    protected String preparePath(String rawPath) {
        if (rawPath == null) return "";
        String lower = rawPath.toLowerCase();
        if (!rawPath.contains("://")) {
            rawPath = rawPath.replace("\\\\",""); // remove UNC prefix if there
            //prefix with smb
            rawPath = "smb://"+rawPath;
        }

        return rawPath.replaceAll("\\\\","/");
    }

    protected void startExternalActivity(String path, String container) {
        Intent external = new Intent(Intent.ACTION_VIEW);
        external.setDataAndType(Uri.parse(path), "video/"+container);

        BaseItemDto item = mItemsToPlay.get(mCurrentNdx);

        //These parms are for MX Player
        if (mPosition > 0) external.putExtra("position", mPosition);
        external.putExtra("return_result", true);
        if (item != null) {
            external.putExtra("title", item.getName());
        }
        //End MX Player

        Timber.i("Starting external playback of path: %s and mime: video/%s",path,container);

        try {
            mLastPlayerStart = System.currentTimeMillis();
            ReportingHelper.reportStart(mItemsToPlay.get(mCurrentNdx), 0);
            startReportLoop();
            startActivityForResult(external, 1);

        } catch (ActivityNotFoundException e) {
            noPlayerError = true;
            Timber.e(e, "Error launching external player");
            handlePlayerError();
        }
    }
}
