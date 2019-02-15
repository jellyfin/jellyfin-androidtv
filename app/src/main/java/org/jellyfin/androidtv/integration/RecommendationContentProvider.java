package org.jellyfin.androidtv.integration;
/**
 * The MIT License (MIT)
 * Copyright (c) 2014 David Carver
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Provides a background image for Recommendations for a RecommendationCardView.
 *
 * The card view changed to require a content provider be created. It requests
 * items via the openFile(Uri uri) method. This is an example of how to retrieve
 * an image from a web site and provide a ParcelFileDescriptor back that
 * contains the contents.
 *
 * This is based on code from the following stackoverflow description on how to
 * populate a ParcelFileDescriptor from any input stream.
 *
 * http://stackoverflow.com/a/14734310/1950264
 *
 * You still need to setup a ContentProvider entry and Authority in the
 * AndroidManifest.xml
 *
 * See
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 */
public class RecommendationContentProvider extends ContentProvider {

    public static String AUTHORITY = "org.jellyfin.androidtv.recommendations";
    public static String CONTENT_URI = "content://" + AUTHORITY + "/";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        ParcelFileDescriptor[] pipe = null;

        String url = uri.getPath();

        try {
            String decodedUrl = URLDecoder.decode(url.replaceFirst("/", ""),
                    "UTF-8");
            pipe = ParcelFileDescriptor.createPipe();

            // This uses OKHttp to
            OkHttpClient httpClient = new OkHttpClient();
            HttpURLConnection connection = new OkUrlFactory(httpClient).open(new URL(decodedUrl));

            new TransferThread(connection.getInputStream(),
                    new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]))
                    .start();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Exception opening pipe", e);
            throw new FileNotFoundException("Could not open pipe for: "
                    + uri.toString());
        }

        return (pipe[0]);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return "image/*";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        return 0;
    }

    static class TransferThread extends Thread {
        InputStream in;
        OutputStream out;

        TransferThread(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buf = new byte[8192];
            int len;

            try {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(),
                        "Exception transferring file", e);
            }
        }
    }

}