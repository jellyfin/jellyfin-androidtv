package org.jellyfin.androidtv.playback;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.model.compat.PlaybackException;
import org.jellyfin.androidtv.model.compat.StreamInfo;
import org.jellyfin.androidtv.model.compat.VideoOptions;
import org.jellyfin.androidtv.util.ProfileHelper;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.ReportingHelper;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.session.PlayMethod;

import java.util.List;

import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.BackgroundManager;

public class ExternalPlayerActivity extends FragmentActivity {

    List<BaseItemDto> mItemsToPlay;
    TvApp mApplication = TvApp.getApplication();
    int mCurrentNdx = 0;
    StreamInfo mCurrentStreamInfo;

    Handler mHandler = new Handler();
    Runnable mReportLoop;

    long mLastPlayerStart = 0;
    Long mPosition = 0l;
    boolean isLiveTv;
    boolean noPlayerError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BackgroundManager.getInstance(this).attach(getWindow());

        mItemsToPlay = MediaManager.getCurrentVideoQueue();

        if (mItemsToPlay == null || mItemsToPlay.size() == 0) {
            Utils.showToast(mApplication, mApplication.getString(R.string.msg_no_playable_items));
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
        mApplication.getLogger().Debug("Returned from player... %d", resultCode);
        //MX Player will return position
        int pos = data != null ? data.getIntExtra("position", 0) : 0;
        if (pos > 0) mApplication.getLogger().Info("Player returned position: %d", pos);
        Long reportPos = (long) pos * 10000;

        stopReportLoop();
        ReportingHelper.reportStopped(mItemsToPlay.get(mCurrentNdx), mCurrentStreamInfo, reportPos);

        //Check against a total failure (no apps installed)
        if (playerFinishedTime - mLastPlayerStart < 1000) {
            // less than a second - probably no player explain the option
            mApplication.getLogger().Info("Playback took less than a second - assuming it failed");
            if (!noPlayerError) handlePlayerError();
            return;
        }

        long runtime = mItemsToPlay.get(mCurrentNdx).getRunTimeTicks() != null ? mItemsToPlay.get(mCurrentNdx).getRunTimeTicks() / 10000 : 0;
        if (pos == 0) {
            //If item didn't play as long as its duration - confirm we want to mark watched
            if (!isLiveTv && playerFinishedTime - mLastPlayerStart < runtime * .9) {
                new AlertDialog.Builder(this)
                        .setTitle("Mark Watched")
                        .setMessage("The item didn't appear to play as long as its duration. Mark watched?")
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
                                playNext();
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
        if (!MediaManager.isVideoQueueModified()) MediaManager.clearVideoQueue();

        new AlertDialog.Builder(this)
                .setTitle("No Player")
                .setMessage("It doesn't appear you have a video capable app installed.  This option requires you install a 3rd party application for playing video content.")
                .setPositiveButton(R.string.btn_got_it, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton("Turn this option off", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = mApplication.getPrefs();
                        prefs.edit().putString("pref_video_player", "auto").apply();
                        prefs.edit().putBoolean("pref_live_tv_use_external", false).apply();
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

                mApplication.setLastUserInteraction(System.currentTimeMillis());
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
        mApplication.getApiClient().MarkPlayedAsync(itemId, mApplication.getCurrentUser().getId(), null, new Response<UserItemDataDto>());
    }

    protected void playNext() {
        mItemsToPlay.remove(0);
        if (mItemsToPlay.size() > 0) {
            //Must confirm moving to the next item or there is no way to stop playback of all the items
            new AlertDialog.Builder(this)
                    .setTitle("Next up is "+mItemsToPlay.get(mCurrentNdx).getName())
                    .setPositiveButton(R.string.play, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            launchExternalPlayer(0);
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!MediaManager.isVideoQueueModified()) MediaManager.clearVideoQueue();
                            finish();
                        }
                    })
                    .show();

        } else {
            finish();
        }

    }

    protected void launchExternalPlayer(int ndx) {
        if (ndx >= mItemsToPlay.size()) {
            mApplication.getLogger().Error("Attempt to play index beyond items: %s",ndx);
        } else {
            //Get playback info for current item
            mCurrentNdx = ndx;
            final BaseItemDto item = mItemsToPlay.get(mCurrentNdx);
            isLiveTv = item.getBaseItemType() == BaseItemType.TvChannel;

            if (!isLiveTv && mApplication.getPrefs().getBoolean("pref_send_path_external", false)) {
                // Just pass the path directly
                mCurrentStreamInfo = new StreamInfo();
                mCurrentStreamInfo.setPlayMethod(PlayMethod.DirectPlay);
                startExternalActivity(preparePath(item.getPath()), item.getContainer() != null ? item.getContainer() : "*");
            } else {
                //Build options for player
                VideoOptions options = new VideoOptions();
                options.setDeviceId(mApplication.getApiClient().getDeviceId());
                options.setItemId(item.getId());
                options.setMediaSources(item.getMediaSources());
                options.setMaxBitrate(Utils.getMaxBitrate());
                options.setProfile(ProfileHelper.getExternalProfile());

                // Get playback info for each player and then decide on which one to use
                mApplication.getPlaybackManager().getVideoStreamInfo(mApplication.getApiClient().getServerInfo().getId(), options, item.getResumePositionTicks(), false, mApplication.getApiClient(), new Response<StreamInfo>() {
                    @Override
                    public void onResponse(StreamInfo response) {
                        mCurrentStreamInfo = response;

                        //Construct a static URL to sent to player
                        //String url = mApplication.getApiClient().getApiUrl() + "/videos/" + response.getItemId() + "/stream?static=true&mediaSourceId=" + response.getMediaSourceId();

                        String url = response.getMediaUrl();
                        //And request an activity to play it
                        startExternalActivity(url, response.getMediaSource().getContainer() != null ? response.getMediaSource().getContainer() : "*");
                    }

                    @Override
                    public void onError(Exception exception) {
                        mApplication.getLogger().ErrorException("Error getting playback stream info", exception);
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

        mApplication.getLogger().Info("Starting external playback of path: %s and mime: video/%s",path,container);

        try {
            mLastPlayerStart = System.currentTimeMillis();
            ReportingHelper.reportStart(mItemsToPlay.get(mCurrentNdx), 0);
            startReportLoop();
            startActivityForResult(external, 1);

        } catch (ActivityNotFoundException e) {
            noPlayerError = true;
            mApplication.getLogger().ErrorException("Error launching external player", e);
            handlePlayerError();
        }

    }
}
