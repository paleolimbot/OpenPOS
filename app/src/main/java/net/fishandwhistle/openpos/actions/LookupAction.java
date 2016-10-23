package net.fishandwhistle.openpos.actions;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import net.fishandwhistle.openpos.R;
import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dewey on 2016-10-02.
 */

public class LookupAction extends ScannedItemAction {

    public static final String OPTION_URI_FORMAT = "uri_format";
    public static final String OPTION_KEYMAP = "key_map";
    public static final String OPTION_MIMETYPE = "mime_type";
    public static final String OPTION_ENCODING = "encoding";

    public static final String KEY_ERROR = "lookup_error";

    private static final String TAG = "LookupAction" ;
    private static Set<String> currentRequests = new HashSet<>();
    private Map<String, String> keyMap;
    private String uriFormat;
    private LookupParser parser;
    private String encoding;

    public LookupAction(String actionName, String jsonOptions) {
        super(actionName, jsonOptions);
        this.uriFormat = getOptionString(OPTION_URI_FORMAT);
        this.keyMap = extractKeyMap(getOptionObject(OPTION_KEYMAP));
        switch (getOptionString(OPTION_MIMETYPE)) {
            case "application/json":
                parser = new JSONParser();
                break;
            default: throw new IllegalArgumentException("No parser available for mime type " + getOptionString(OPTION_MIMETYPE));
        }
        //get/test encoding
        encoding = getOptionString(OPTION_ENCODING);
        if(encoding == null) {
            encoding = "UTF-8";
        } else {
            try {
                byte[] out = "derp".getBytes(encoding);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Invalid encoding: " + encoding);
            }
        }
    }

    private String getErrorKey() {
        return "error_" + getActionName();
    }

    private String getSourceKey() {
        return "source_" + getActionName();
    }

    private String getTimeKey() {
        return "time_" + getActionName();
    }

    private Map<String, String> getKeyMap() {
        return keyMap;
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

    public boolean doAction(Context context, ScannedItem item) throws ActionException {
        String url = formatUri(uriFormat, item);
        TextApiCache cache = new TextApiCache(context);

        if(currentRequests.contains(url)) {
            Log.i(TAG, "Disregarding redundant request: " + url);
            item.putValue(getErrorKey(), context.getString(R.string.api_errorredrequest));
            return false;
        } else {
            TextApiCache.CachedItem cached = cache.get(url);
            if(cached != null) {
                Log.i(TAG, "Using cached data for url " + url);
                if(this.parser.parse(cached.data, item)) {
                    item.putValue(getTimeKey(), String.valueOf(cached.queryTime));
                    item.putValue(getSourceKey(), url);
                    item.isLoading = false;
                    return true;
                } else {
                    if(isQuiet()) {
                        item.putValue(getErrorKey(), "Parse error");
                        return true;
                    } else {
                        throw new ActionException("Parse error");
                    }
                }
            } else {
                if(isNetworkAvailable(context)) {
                    currentRequests.add(url);
                    item.isLoading = true;
                    doDownload(context, url, item);
                    item.isLoading = false;
                    return true;
                } else {
                    if(isQuiet()) {
                        item.putValue(getErrorKey(), context.getString(R.string.api_errornonetwork));
                        return true;
                    } else {
                        throw new ActionException(context.getString(R.string.api_errornonetwork));
                    }
                }
            }
        }
    }

    private void doDownload(Context context, String urlString, ScannedItem item) throws ActionException {
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
                String error = String.format(context.getString(R.string.api_errorio),
                        "HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
                if(isQuiet()) {
                    item.putValue(getErrorKey(), error);
                    return;
                } else {
                    item.isLoading = false;
                    throw new ActionException(error);
                }
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
            String error = String.format(context.getString(R.string.api_errorio), e.getMessage());
            currentRequests.remove(urlString);
            if(isQuiet()) {
                item.putValue(getErrorKey(), error);
                return;
            } else {
                item.isLoading = false;
                throw new ActionException(error);
            }
        } finally {
            try {
                if (output != null) {
                    out = output.toString(encoding);
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
            //should not happen?
            currentRequests.remove(urlString);
            item.isLoading = false;
            throw new ActionException("Null output from download");
        }

        // do parsing
        currentRequests.remove(urlString);
        item.putValue(getTimeKey(), String.valueOf(System.currentTimeMillis()));
        item.putValue(getSourceKey(), urlString);
        parser.parse(out, item);
    }

    private interface LookupParser {
        boolean parse(String data, ScannedItem item) throws ActionException;
    }

    private class JSONParser implements LookupParser {

        @Override
        public boolean parse(String data, ScannedItem item) throws ActionException {
            try {
                JSONObject o = new JSONObject(data);
                Map<String, String> keyMap = getKeyMap();
                if(keyMap == null) {
                    Iterator<String> keys = o.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        o.put(key, o.getString(key));
                    }
                } else {
                    for(Map.Entry<String, String> e: keyMap.entrySet()) {
                        String itemKey = e.getValue();
                        if(itemKey.equals(KEY_ERROR)) {
                            itemKey = getErrorKey();
                        }
                        String[] path = e.getKey().split("/");
                        String value = followPath(o, path, 0);
                        if(!TextUtils.isEmpty(value)) {
                            item.putValue(itemKey, value);
                        } else {
                            if(!isQuiet()) item.putValue(itemKey, "NA");
                        }
                    }
                }
                return true;
            } catch(JSONException e) {
                String error = "JSON error in parse: " + e.getMessage();
                if(isQuiet()) {
                    item.putValue("lookup_error", error);
                    return true;
                } else {
                    item.isLoading = false;
                    throw new ActionException(error);
                }
            }
        }

        private String followPath(JSONObject o, String[] path, int index) {
            Pattern INDEX = Pattern.compile("\\[(.*?)\\]");
            try {
                String key = path[index];
                Matcher m = INDEX.matcher(key);
                if(m.find()) {
                    //is an index (e.g. data[0])
                    String arrInd = m.group(1);
                    key = key.replace("["+arrInd+"]", "");
                    JSONArray a = o.getJSONArray(key);
                    try {
                        if(index == (path.length-1)) {
                            return a.getString(Integer.valueOf(arrInd));
                        } else {
                            return followPath(a.getJSONObject(Integer.valueOf(arrInd)), path, index+1);
                        }
                    } catch(NumberFormatException e) {
                        //we are going to join the results of everything else on arrInd
                        String[] strings = new String[a.length()];
                        if(index == (path.length-1)) {
                            for(int i=0; i<a.length(); i++) {
                                strings[i] = a.getString(i);
                            }
                        } else {
                            for(int i=0; i<a.length(); i++) {
                                String s = followPath(a.getJSONObject(i), path, index + 1);
                                if(s == null) {
                                    if(isQuiet()) {
                                        return null;
                                    } else {
                                        s = "NA";
                                    }
                                }
                                strings[i] = s;
                            }
                        }
                        //join the results
                        return TextUtils.join(arrInd, strings);
                    }
                } else {
                    if(index == (path.length-1)) {
                        return o.getString(key);
                    } else {
                        return followPath(o.getJSONObject(key), path, index + 1);
                    }
                }

            } catch(JSONException e) {
                //Log.e("JSONLookupItem", "followPath: json exception", e);
                //'string not found' is very common, don't log
                return null;
            }
        }
    }


    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
