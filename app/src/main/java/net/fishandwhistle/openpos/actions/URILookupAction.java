package net.fishandwhistle.openpos.actions;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import net.fishandwhistle.openpos.R;
import net.fishandwhistle.openpos.items.ScannedItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by dewey on 2016-10-02.
 */

public abstract class URILookupAction extends ScannedItemAction {

    private static final String TAG = "URILookup" ;
    private static Set<String> currentRequests = new HashSet<>();
    protected Map<String, String> keyMap;

    private String uriFormat;

    public URILookupAction(String actionName, String uriFormat, Map<String, String> keyMap) {
        super(actionName);
        this.uriFormat = uriFormat;
        this.keyMap = keyMap;
    }

    private static String formatUri(String format, ScannedItem item) {
        String out = format;
        List<String> keys = item.getKeys();
        for(String key: keys) {
            String replace = "{{"+key+"}}";
            out = out.replace(replace, item.getValue(key));
        }
        if(out.contains("{{") || out.contains("}}")) {
            return null;
        } else {
            return out;
        }
    }

    protected abstract boolean parse(String data, ScannedItem item);
    
    public boolean doAction(Context context, ScannedItem item) {
        String url = formatUri(uriFormat, item);
        TextApiCache cache = new TextApiCache(context);

        if(currentRequests.contains(url)) {
            Log.i(TAG, "Disregarding redundant request: " + url);
            item.putValue("lookup_error", context.getString(R.string.api_errorredrequest));
            return false;
        } else {
            TextApiCache.CachedItem cached = cache.get(url);
            if(cached != null) {
                Log.i(TAG, "Using cached data for url " + url);
                if(this.parse(cached.data, item)) {
                    item.putValue("json_time", String.valueOf(cached.queryTime));
                    item.putValue("json_source", url);
                    item.isLoading = false;
                    return true;
                } else {
                    return false;
                }
            } else {
                if(isNetworkAvailable(context)) {
                    currentRequests.add(url);
                    item.isLoading = true;
                    if(doDownload(context, url, item)) {
                        item.isLoading = false;
                        return true;
                    } else {
                        item.isLoading = false;
                        return false;
                    }
                } else {
                    item.putValue("lookup_error", context.getString(R.string.api_errornonetwork));
                    return false;
                }
            }
        }
    }

    private boolean doDownload(Context context, String urlString, ScannedItem item) {
        String out = null;
        InputStream input = null;
        ByteArrayOutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            Log.i(TAG, "starting download from " + url.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                currentRequests.remove(urlString);
                Log.e(TAG, "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage());
                item.putValue("lookup_error", String.format(context.getString(R.string.api_errorio),
                        "HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage()));
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
//                if (isCancelled()) {
//                    input.close();
//                    currentRequests.remove(sUrl[0]);
//                    return false;
//                }
                total += count;
                // publishing the progress....
//                if (fileLength > 0) // only if total length is known
//                    publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }
            Log.i(TAG, "Download complete");
        } catch (Exception e) {
            Log.e(TAG, "Exception in download", e);
            item.putValue("lookup_error", String.format(context.getString(R.string.api_errorio), e.getMessage()));
            currentRequests.remove(urlString);
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
            currentRequests.remove(urlString);
            return false;
        }

        // do parsing
        currentRequests.remove(urlString);
        item.putValue("json_time", String.valueOf(System.currentTimeMillis()));
        item.putValue("json_source", urlString);
        return parse(out, item);
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
