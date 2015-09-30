package tv.emby.embyatv.playback;

import android.os.Environment;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.entities.MediaStream;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 7/19/2015.
 */
public class SubtitleHelper {

    private ApiClient apiClient;
    private BaseActivity activity;

    public SubtitleHelper(BaseActivity activity) {
        this.activity = activity;
        apiClient = TvApp.getApplication().getApiClient();
    }

    public void downloadExternalSubtitleTrack(final MediaStream stream, Response<File> response) {

        final File file = new File(getSubtitleDownloadPath(stream));

        if (file.exists()){
            TvApp.getApplication().getLogger().Info("Re-using downloaded subtitle file");
            response.onResponse(file);
            return;
        }

        downloadSubtitles(stream, file, response);
    }

    private void downloadSubtitles(final MediaStream stream, final File file, final Response<File> response) {

        String url = (stream.getIsExternalUrl() != null && !stream.getIsExternalUrl()) ? apiClient.GetApiUrl(stream.getDeliveryUrl()) : stream.getDeliveryUrl();

        TvApp.getApplication().getLogger().Info("Subtitle url: "+url);

        apiClient.getResponseStream(url, new Response<InputStream>(response){

            @Override
            public void onResponse(InputStream initialStream) {

                try {
                    Files.createParentDirs(file);

                    OutputStream outStream = new FileOutputStream(file);

                    try {
                        byte[] buffer = new byte[8 * 1024];
                        int bytesRead;
                        while ((bytesRead = initialStream.read(buffer)) != -1) {
                            outStream.write(buffer, 0, bytesRead);
                        }
                    }
                    finally {
                        outStream.close();
                    }
                }
                catch (Exception ex){
                    response.onError(ex);
                    return;
                }
                finally {
                    try {
                        initialStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                response.onResponse(file);
            }
        });

    }

    private String getSubtitleDownloadPath(MediaStream stream) {

        String filename = Utils.MD5(stream.getDeliveryUrl());

        if (stream.getCodec() != null){
            filename += "." + stream.getCodec().toLowerCase();
        }

        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if (mExternalStorageAvailable && mExternalStorageWriteable){
            File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "emby");
            directory = new File(directory, "subtitles");
            return new File(directory, filename).getPath();
        }
        else{
            return activity.getFileStreamPath(filename).getAbsolutePath();
        }
    }



}
