package net.fishandwhistle.openpos.api;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by dewey on 2016-10-02.
 */

public abstract class APIQuery {

    private static final String TAG = "APIQuery" ;
    private static Set<String> currentRequests = new HashSet<>();

    private String input;
    private APICallback callback ;
    private Context context ;
    private DownloadTask task;
    private TextApiCache cache;
    private ScannedItem item;

    public APIQuery(Context context, String input, ScannedItem item, APICallback callback) {
        this.input = input;
        this.callback = callback;
        this.context = context;
        this.item = item;
        this.cache = new TextApiCache(context);
    }

    protected abstract String getUrl(String input) ;
    
    protected abstract boolean parseJSON(String json, ScannedItem item);
    
    public boolean query() {
        String url = this.getUrl(this.input);
        if(currentRequests.contains(url)) {
            Log.i(TAG, "Disregarding redundant request: " + input);
            return false;
        } else {
            TextApiCache.CachedItem cached = cache.get(url);
            if(cached != null) {
                Log.i(TAG, "Using cached data for input " + input);
                boolean result = this.parseJSON(cached.data, item);
                item.jsonTime = cached.queryTime;

                callback.onQueryResult(this.input, result, item);
                return false;
            } else {
                currentRequests.add(url);
                task = new DownloadTask(context);
                task.execute(url);
                return true;
            }
        }
    }

    public interface APICallback {
        void onQueryResult(String input, boolean success, ScannedItem item);
    }

    protected class DownloadTask extends AsyncTask<String, Integer, Boolean> {

        private Context context;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(String... sUrl) {
            String out = null;
            InputStream input = null;
            ByteArrayOutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                Log.i(TAG, "starting download from " + url.toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    currentRequests.remove(sUrl[0]);
                    Log.e(TAG, "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                    return false;
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new ByteArrayOutputStream();

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        currentRequests.remove(sUrl[0]);
                        return false;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
                Log.i(TAG, "Download complete");
            } catch (Exception e) {
                Log.e(TAG, "Exception in download", e);
                currentRequests.remove(sUrl[0]);
                return false;
            } finally {
                try {
                    if (output != null) {
                        out = output.toString("UTF-8");
                        output.close();
                    }
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }

            if(out == null) {
                currentRequests.remove(sUrl[0]);
                return false;
            }

            // do parsing
            cache.put(sUrl[0], out);
            currentRequests.remove(sUrl[0]);
            item.jsonTime = System.currentTimeMillis();
            return parseJSON(out, item);

        }

        @Override
        protected void onPostExecute(Boolean result) {
            callback.onQueryResult(input, result, item);
        }
    }

}
