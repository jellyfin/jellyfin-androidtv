package tv.emby.embyatv.util;

import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.http.HttpHeaders;
import mediabrowser.apiinteraction.http.HttpRequest;
import tv.emby.embyatv.TvApp;

/**
 * Created by Eric on 2/28/2016.
 */
public class LogReporter {
    public void sendReport(String cause, final EmptyResponse callback) {
        TvApp.getApplication().getLogger().Info("Sending log...");
        LogReport report = new LogReport();
        report.setOsVersionInt(Build.VERSION.SDK_INT);
        report.setOsVersionString(Build.VERSION.RELEASE);
        report.setCause(cause);
        report.setDeviceInfo(Build.MODEL);
        if (TvApp.getApplication().getCurrentSystemInfo() != null) report.setServerName(TvApp.getApplication().getCurrentSystemInfo().getServerName());
        if (TvApp.getApplication().getCurrentUser() != null) report.setUserName(TvApp.getApplication().getCurrentUser().getName());

        //get log
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log=new StringBuilder();
            String line = "";
            int numLines = 0;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line).append("\r\n");
                numLines++;
            }

            TvApp.getApplication().getLogger().Info("Log lines retrieved: "+numLines);

            report.setLogLines(log.toString());
        }
        catch (IOException e) {
            report.setLogLines("Unable to retrieve Log: "+e.getMessage());
        }

        String json = TvApp.getApplication().getSerializer().SerializeToString(report);
        HttpRequest request = new HttpRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.put("Accept", "application/json");
        request.setUrl("http://mb3admin.com/admin/service/logReport/send");
        request.setMethod("POST");
        request.setRequestHeaders(headers);
        request.setRequestContent(json);
        request.setRequestContentType("application/json");
        TvApp.getApplication().getHttpClient().Send(request, new mediabrowser.apiinteraction.Response<String>() {
            @Override
            public void onResponse(String response) {
                TvApp.getApplication().getLogger().Info("Response from log report send: "+response);
                if (callback != null) callback.onResponse();
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error sending log", exception);
                if (callback != null) callback.onResponse();
            }
        });
    }
}
