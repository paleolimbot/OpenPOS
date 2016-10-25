package net.fishandwhistle.openpos.actions;

import android.app.Notification;
import android.content.Context;
import android.drm.DrmStore;
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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlrpc.android.Tag;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;
import org.xmlrpc.android.XMLRPCSerializer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
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
    public static final String OPTION_API_TYPE = "api_type";
    public static final String OPTION_REQUEST = "request";

    public static final String KEY_ERROR = "lookup_error";

    private static final String TAG = "LookupAction" ;
    private static Set<String> currentRequests = new HashSet<>();
    private Map<String, String> keyMap;
    private String uriFormat;
    private LookupParser parser;
    private String encoding;
    private String apiType;
    private String request;

    public LookupAction(JSONObject jsonObject) {
        super(jsonObject);
        this.uriFormat = getOptionString(OPTION_URI_FORMAT);
        this.keyMap = extractKeyMap(getOptionObject(OPTION_KEYMAP));
        switch (getOptionString(OPTION_MIMETYPE)) {
            case "application/json":
                parser = new JSONParser();
                break;
            case "text/xml-rpc":
                parser = new XMLRPCParser();
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
        apiType = getOptionString(OPTION_API_TYPE);
        if(apiType == null) {
            apiType = "REST";
        }
        request = getOptionString(OPTION_REQUEST);
        if(apiType.equals("XML-RPC") || apiType.equals("JSON-RPC")) {
            if(request == null) throw new IllegalArgumentException("XML-RPC and JSON-RPC require option 'request'");
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

    public boolean doAction(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        String url = StringFormatAction.formatWithObject(uriFormat, item, false);
        String cacheUrl = url;
        if(url == null) {
            if(isQuiet()) {
                return false;
            } else {
                throw new ActionException("Unmapped key in uri: " + uriFormat);
            }
        }
        String requestFormatted;
        if(request != null) {
            requestFormatted = StringFormatAction.formatWithObject(request, item, false);
            if(requestFormatted == null) {
                if(isQuiet()) {
                    return false;
                } else {
                    throw new ActionException("Unmapped key in request: " + request);
                }
            }
            cacheUrl = url + "#" + requestFormatted;
        } else {
            requestFormatted = null;
        }

        TextApiCache cache = new TextApiCache(context);

        if(currentRequests.contains(cacheUrl)) {
            Log.i(TAG, "Disregarding redundant request: " + url);
            item.putValue(getErrorKey(), context.getString(R.string.api_errorredrequest));
            return false;
        } else {
            TextApiCache.CachedItem cached = cache.get(cacheUrl);
            if(cached != null) {
                Log.i(TAG, "Using cached data for url " + url);
                if(this.parser.parse(cached.data, item)) {
                    item.putValue(getTimeKey(), String.valueOf(cached.queryTime));
                    item.putValue(getSourceKey(), url);
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
                    currentRequests.add(cacheUrl);
                    doDownload(context, url, requestFormatted, cacheUrl, item, cache, executor);
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

    private void doDownload(Context context, String urlString, String requestFormatted, String cacheUrl, ScannedItem item, TextApiCache cache, ActionExecutor executor) throws ActionException {
        String out = null;
        InputStream input = null;
        ByteArrayOutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection)url.openConnection();
            if(apiType.equals("JSON-RPC") || apiType.equals("XML-RPC")) {
                String requestMime;
                if(apiType.equals("JSON-RPC")) {
                    requestMime = "text/json";
                } else {
                    requestMime = "text/xml";
                }
                byte[] requestData = requestFormatted.getBytes("UTF-8");
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", requestMime);
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Content-Length", Integer.toString(requestData.length));
                connection.setUseCaches(false);
                connection.getOutputStream().write(requestData);
            }
            Log.i(TAG, "starting download from " + urlString);
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                currentRequests.remove(cacheUrl);
                Log.e(TAG, "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage());
                String error = String.format(context.getString(R.string.api_errorio),
                        "HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
                if(isQuiet()) {
                    item.putValue(getErrorKey(), error);
                    return;
                } else {
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
                // allow canceling
                if (executor.isCancelled()) {
                    input.close();
                    currentRequests.remove(cacheUrl);
                    String error = "User cancelled";
                    if(isQuiet()) {
                        item.putValue(getErrorKey(), error);
                    } else {
                        throw new ActionException(error);
                    }
                }
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
            currentRequests.remove(cacheUrl);
            if(isQuiet()) {
                item.putValue(getErrorKey(), error);
                return;
            } else {
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
            currentRequests.remove(cacheUrl);
            throw new ActionException("Null output from download");
        } else {
            cache.put(cacheUrl, out);
        }

        // do parsing
        currentRequests.remove(cacheUrl);
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
                if(keyMap == null) {
                    Iterator<String> keys = o.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        item.putValue(key, o.getString(key));
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


    private class XMLRPCParser implements LookupAction.LookupParser {

        @Override
        public boolean parse(String data, ScannedItem item) throws ActionException {
            Object result = parseRPC(data, item);
            if(result == null) {
                if(isQuiet()) {
                    item.putValue(getErrorKey(), "Null result from RPC Parser");
                } else {
                    throw new ActionException("Null result from RPC Parser");
                }
            } else if(result instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) result;
                if(keyMap == null) {
                    for(Map.Entry<String, Object> e: map.entrySet()) {
                        item.putValue(e.getKey(), e.getValue().toString());
                    }
                } else {
                    for(Map.Entry<String, String> e: keyMap.entrySet()) {
                        String itemKey = e.getValue();
                        if(itemKey.equals(KEY_ERROR)) {
                            itemKey = getErrorKey();
                        }
                        String[] path = e.getKey().split("/");
                        String value = followPath(map, path, 0);
                        if(!TextUtils.isEmpty(value)) {
                            item.putValue(itemKey, value);
                        } else {
                            if(!isQuiet()) item.putValue(itemKey, "NA");
                        }
                    }
                }
            } else {
                if(isQuiet()) {
                    item.putValue(getErrorKey(), "Non-map result from RPC Parser");
                } else {
                    throw new ActionException("Non-map result from RPC Parser");
                }
            }
            return true;
        }

        private String followPath(Map<String, Object> o, String[] path, int index) {
            Pattern INDEX = Pattern.compile("\\[(.*?)\\]");
            try {
                String key = path[index];
                Matcher m = INDEX.matcher(key);
                if (m.find()) {
                    //is an index (e.g. data[0])
                    String arrInd = m.group(1);
                    key = key.replace("[" + arrInd + "]", "");
                    if (!o.containsKey(key))
                        throw new IndexOutOfBoundsException("'" + key + "' not in Map");
                    List<Object> a = (List<Object>) o.get(key);
                    try {
                        if (index == (path.length - 1)) {
                            return a.get(Integer.valueOf(arrInd)).toString();
                        } else {
                            return followPath((Map<String, Object>) a.get(Integer.valueOf(arrInd)), path, index + 1);
                        }
                    } catch (NumberFormatException e) {
                        //we are going to join the results of everything else on arrInd
                        String[] strings = new String[a.size()];
                        if (index == (path.length - 1)) {
                            for (int i = 0; i < a.size(); i++) {
                                strings[i] = a.get(i).toString();
                            }
                        } else {
                            for (int i = 0; i < a.size(); i++) {
                                String s = followPath((Map<String, Object>) a.get(i), path, index + 1);
                                if (s == null) {
                                    if (isQuiet()) {
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
                    if (index == (path.length - 1)) {
                        if(!o.containsKey(key)) throw new IndexOutOfBoundsException("'" + key + "' not in map");
                        return o.get(key).toString();
                    } else {
                        return followPath((Map<String, Object>) o.get(key), path, index + 1);
                    }
                }

            } catch(ClassCastException e) {
                //kind of like json exception
                return null;
            } catch(IndexOutOfBoundsException e) {
                return null;
            }
        }

        private Object parseRPC(String rpcString, ScannedItem item) throws ActionException {
            //stolen from XMLRPCCLient
            String error;
            XMLRPCSerializer iXMLRPCSerializer = new XMLRPCSerializer();
            try {
                XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
// for testing purposes only
                StringReader reader = new StringReader(rpcString);
                pullParser.setInput(reader);

                // lets start pulling...
                pullParser.nextTag();
                pullParser.require(XmlPullParser.START_TAG, null, Tag.METHOD_RESPONSE);

                pullParser.nextTag(); // either Tag.PARAMS (<params>) or Tag.FAULT (<fault>)
                String tag = pullParser.getName();
                if (tag.equals(Tag.PARAMS)) {
                    // normal response
                    pullParser.nextTag(); // Tag.PARAM (<param>)
                    pullParser.require(XmlPullParser.START_TAG, null, Tag.PARAM);
                    pullParser.nextTag(); // Tag.VALUE (<value>)
                    // no parser.require() here since its called in XMLRPCSerializer.deserialize() below

                    // deserialize result
                    Object obj = iXMLRPCSerializer.deserialize(pullParser);
                    //entity.consumeContent();
                    return obj;
                } else
                if (tag.equals(Tag.FAULT)) {
                    // fault response
                    pullParser.nextTag(); // Tag.VALUE (<value>)
                    // no parser.require() here since its called in XMLRPCSerializer.deserialize() below

                    // deserialize fault result
                    Map<String, Object> map = (Map<String, Object>) iXMLRPCSerializer.deserialize(pullParser);
                    String faultString = (String) map.get(Tag.FAULT_STRING);
                    int faultCode = (Integer) map.get(Tag.FAULT_CODE);
                    //entity.consumeContent();
                    throw new XMLRPCFault(faultString, faultCode);
                } else {
                    //entity.consumeContent();
                    throw new XMLRPCException("Bad tag <" + tag + "> in XMLRPC response - neither <params> nor <fault>");
                }
            } catch(XmlPullParserException e) {
                error = "XML Exception in RPC response: " + e.getMessage();
            } catch(IOException e) {
                error = "IO Exception in RPC response: " + e.getMessage();
            } catch(XMLRPCException e) {
                error = e.getMessage();
            }
            if(isQuiet()) {
                item.putValue(getErrorKey(), error);
                return null;
            } else {
                throw new ActionException(error);
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
