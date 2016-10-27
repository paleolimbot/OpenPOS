package net.fishandwhistle.openpos.actions;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import net.fishandwhistle.openpos.R;
import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuth10aServiceImpl;
import org.scribe.oauth.OAuthService;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlrpc.android.Tag;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFault;
import org.xmlrpc.android.XMLRPCSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static net.fishandwhistle.openpos.actions.Formatting.formatWithObject;

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
    public static final String OPTION_HEADER = "header";
    public static final String OPTION_VALID_CHECK = "valid_check";
    public static final String OPTION_INVALID_CHECK = "invalid_check";
    public static final String OPTION_OAUTH1 = "oauth1";

    private static final String TAG = "LookupAction" ;
    private static Set<String> currentRequests = new HashSet<>();
    private Map<String, String> keyMap;
    private String uriFormat;
    private LookupParser parser;
    private String encoding;
    private String apiType;
    private String request;
    private Map<String, String> header;
    private Map<String, String> validCheck;
    private Map<String, String> invalidCheck;

    private OAuthService oAuthService;

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
            case "text/xml":
                parser = new XMLParser();
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
        header = extractKeyMap(getOptionObject(OPTION_HEADER));
        validCheck = extractKeyMap(getOptionObject(OPTION_VALID_CHECK));
        invalidCheck = extractKeyMap(getOptionObject(OPTION_INVALID_CHECK));

        JSONObject oauth = getOptionObject(OPTION_OAUTH1);
        if(oauth != null) {
            try {
                if (!oauth.has("api_key") || !oauth.has("secret"))
                    throw new IllegalArgumentException("Oauth1.0 requires 'api_key' and 'secret'");
                oAuthService = new ServiceBuilder()
                        .provider(new Api() {
                            @Override
                            public OAuthService createService(OAuthConfig config) {
                                return new OAuth10aServiceImpl(new DefaultApi10a() {
                                    @Override
                                    public String getRequestTokenEndpoint() {
                                        return null;
                                    }

                                    @Override
                                    public String getAccessTokenEndpoint() {
                                        return null;
                                    }

                                    @Override
                                    public String getAuthorizationUrl(Token requestToken) {
                                        return null;
                                    }
                                }, config);
                            }
                        })
                        .apiKey(oauth.getString("api_key"))
                        .apiSecret(oauth.getString("secret"))
                        .build();
            } catch(JSONException e) {
                throw new IllegalArgumentException("Invalid JSON in constructor: " + e.getMessage());
            }
        }
    }

    private String getErrorKey() {
        return "error_" + getActionName();
    }

    private String getTimeKey() {
        return "time_" + getActionName();
    }

    @Override
    public boolean isApplicable(Context context, ScannedItem item, ActionExecutor executor) {
        return formatWithObject(uriFormat, item, false) != null;
    }

    public boolean doActionContent(Context context, ScannedItem item, ActionExecutor executor) throws ActionException {
        String url = formatWithObject(uriFormat, item, false);
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
            requestFormatted = formatWithObject(request, item, false);
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
                    return true;
                } else {
                    return false;
                }
            } else {
                if(isNetworkAvailable(context)) {
                    currentRequests.add(cacheUrl);
                    return doDownload(context, url, requestFormatted, cacheUrl, item, cache, executor);
                } else {
                    if(isQuiet()) {
                        item.putValue(getErrorKey(), context.getString(R.string.api_errornonetwork));
                        return false;
                    } else {
                        throw new ActionException(context.getString(R.string.api_errornonetwork));
                    }
                }
            }
        }
    }

    private boolean doDownload(Context context, String urlString, String requestFormatted, String cacheUrl, ScannedItem item, TextApiCache cache, ActionExecutor executor) throws ActionException {
        String out = null;
        InputStream input = null;
        ByteArrayOutputStream output = null;
        HttpURLConnection connection = null;
        OAuthRequest request = null;
        int fileLength = -1;
        try {
            if(oAuthService != null) {
                if(apiType.equals("JSON-RPC") || apiType.equals("XML-RPC")) {
                    String requestMime;
                    if(apiType.equals("JSON-RPC")) {
                        requestMime = "text/json";
                    } else {
                        requestMime = "text/xml";
                    }
                    byte[] requestData = requestFormatted.getBytes("UTF-8");
                    request = new OAuthRequest(Verb.POST, urlString);
                    request.setCharset("UTF-8");
                    request.addHeader("Content-Type", requestMime);
                    request.addHeader("Content-Length", Integer.toString(requestData.length));
                    request.addPayload(requestData);
                } else {
                    request = new OAuthRequest(Verb.GET, urlString);
                    if(header != null) {
                        for(Map.Entry<String, String> e: header.entrySet()) {
                            request.addHeader(e.getKey(), e.getValue());
                        }
                    }
                }
                Token accessToken = new Token("", "");
                oAuthService.signRequest(accessToken, request);
                Log.i(TAG, "starting download from " + urlString);
                Response response = request.send();
                if (response.getCode() != HttpURLConnection.HTTP_OK) {
                    currentRequests.remove(cacheUrl);
                    String error = String.format(context.getString(R.string.api_errorio),
                            "HTTP " + response.getMessage() + " " + response.getCode());
                    Log.e(TAG, "doDownload: " + error);

                    if(isQuiet()) {
                        item.putValue(getErrorKey(), error);
                        return false;
                    } else {
                        throw new ActionException(error);
                    }
                }
                try {
                    String fLength = response.getHeader("Content-Length");
                    if(fLength != null) {
                        fileLength = Integer.valueOf(fLength);
                    }
                } catch(NumberFormatException e) {
                    //ignore
                }
                input = response.getStream();
            } else {
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
                    if(header != null) {
                        for(Map.Entry<String, String> e: header.entrySet()) {
                            connection.setRequestProperty(e.getKey(), e.getValue());
                        }
                    }
                    connection.setUseCaches(false);
                    connection.getOutputStream().write(requestData);
                } else {
                    if(header != null) {
                        for(Map.Entry<String, String> e: header.entrySet()) {
                            connection.setRequestProperty(e.getKey(), e.getValue());
                        }
                    }
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
                        return false;
                    } else {
                        throw new ActionException(error);
                    }
                }
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
            }

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
                return false;
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
        return parser.parse(out, item);
    }

    private boolean checkValidity(Formatting.Formattable formatter) {
        if(validCheck != null) {
            for (Map.Entry<String, String> e : validCheck.entrySet()) {
                String value = formatWithObject(e.getKey(), formatter);
                if (value == null || !value.equals(e.getValue())) return false;
            }
        }
        if(invalidCheck != null) {
            for (Map.Entry<String, String> e : invalidCheck.entrySet()) {
                String value = formatWithObject(e.getKey(), formatter);
                if (value != null && value.equals(e.getValue())) return false;
            }
        }
        return true;
    }

    private interface LookupParser {
        boolean parse(String data, ScannedItem item) throws ActionException;
    }

    private class JSONParser implements LookupParser {

        @Override
        public boolean parse(String data, ScannedItem item) throws ActionException {
            try {
                final JSONObject o = new JSONObject(data);
                if(keyMap == null) {
                    Iterator<String> keys = o.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        item.putValue(key, o.getString(key));
                    }
                    return true;
                } else {
                    Formatting.Formattable formatter = new Formatting.Formattable() {
                        @Override
                        public String getValue(String key) {
                            return followPath(o, key.split("/"), 0);
                        }
                    };
                    int values = 0;
                    for(Map.Entry<String, String> e: keyMap.entrySet()) {
                        String value = formatWithObject(e.getValue(), formatter, false);
                        if(!TextUtils.isEmpty(value)) {
                            item.putValue(e.getKey(), value.trim());
                            values++;
                        } else {
                            if(!isQuiet()) {
                                item.putValue(e.getKey(), "NA");
                            }
                        }
                    }
                    if(!checkValidity(formatter)) return false;
                    if(values == 0) {
                        String error = "No results were obtained from query";
                        if(isQuiet()) {
                            item.putValue(getErrorKey(), error);
                            return false;
                        } else {
                            throw new ActionException(error);
                        }
                    } else {
                        return true;
                    }
                }
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

    private class XMLRPCParser implements LookupParser, Formatting.Formattable {

        @Override
        public boolean parse(String data, ScannedItem item) throws ActionException {
            Object result = parseRPC(data, item);
            if(result == null) {
                if(isQuiet()) {
                    item.putValue(getErrorKey(), "Null result from RPC Parser");
                    return false;
                } else {
                    throw new ActionException("Null result from RPC Parser");
                }
            } else if(result instanceof Map) {
                final Map<String, Object> map = (Map<String, Object>) result;
                if(keyMap == null) {
                    for(Map.Entry<String, Object> e: map.entrySet()) {
                        item.putValue(e.getKey(), e.getValue().toString());
                    }
                    return true;
                } else {
                    Formatting.Formattable formatter = new Formatting.Formattable() {
                        @Override
                        public String getValue(String key) {
                            return followPath(map, key.split("/"), 0);
                        }
                    };
                    int values = 0;
                    for(Map.Entry<String, String> e: keyMap.entrySet()) {
                        String value = formatWithObject(e.getValue(), formatter, false);
                        if(!TextUtils.isEmpty(value)) {
                            item.putValue(e.getKey(), value.trim());
                            values++;
                        } else {
                            if(!isQuiet()) {
                                item.putValue(e.getKey(), "NA");
                            }
                        }
                    }
                    if(!checkValidity(formatter)) return false;
                    if(values == 0) {
                        String error = "No results were obtained from query";
                        if(isQuiet()) {
                            item.putValue(getErrorKey(), error);
                            return false;
                        } else {
                            throw new ActionException(error);
                        }
                    } else {
                        return true;
                    }
                }
            } else {
                if(isQuiet()) {
                    item.putValue(getErrorKey(), "Non-map result from RPC Parser");
                    return false;
                } else {
                    throw new ActionException("Non-map result from RPC Parser");
                }
            }
        }

        @Override
        public String getValue(String key) {
            return null;
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

    private class XMLParser implements LookupParser {

        @Override
        public boolean parse(String data, ScannedItem item) throws ActionException {
            Document d = getDomElement(data);
            if(d != null) {
                final Node root = d.getFirstChild();
                if (keyMap == null) {
                    if(root.hasChildNodes()) {
                        NodeList l = root.getChildNodes();
                        for(int i=0; i<l.getLength(); i++) {
                            Node n = l.item(i);
                            item.putValue(n.getNodeName(), n.getNodeValue());
                        }
                        return true;
                    } else {
                        String error = "No data in XML response";
                        if(!isQuiet()) {
                            throw new ActionException(error);
                        } else {
                            item.putValue(getErrorKey(), error);
                            return false;
                        }
                    }
                } else {
                    Formatting.Formattable formatter = new Formatting.Formattable() {
                        @Override
                        public String getValue(String key) {
                            return followPath(root, key.split("/"), 0);
                        }
                    };
                    int values = 0;
                    for(Map.Entry<String, String> e: keyMap.entrySet()) {
                        String value = formatWithObject(e.getValue(), formatter, false);
                        if(!TextUtils.isEmpty(value)) {
                            item.putValue(e.getKey(), value.trim());
                            values++;
                        } else {
                            if(!isQuiet()) {
                                item.putValue(e.getKey(), "NA");
                            }
                        }
                    }
                    if(!checkValidity(formatter)) return false;
                    if(values == 0) {
                        String error = "No values were obtained from query";
                        if(isQuiet()) {
                            item.putValue(getErrorKey(), error);
                            return false;
                        } else {
                            throw new ActionException(error);
                        }
                    } else {
                        return true;
                    }
                }
            } else {
                return false;
            }
        }

        public Document getDomElement(String xml) {
            Document doc;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            dbf.setCoalescing(true);
            try {
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(xml));
                doc = db.parse(is);

            } catch (ParserConfigurationException e) {
                return null;
            } catch (SAXException e) {
                return null;
            } catch (IOException e) {
                return null;
            }

            return doc;

        }

        public String followPath(Node o, String[] path, int index) {
            Pattern INDEX = Pattern.compile("\\[(.*?)\\]");
            try {
                String key = path[index];
                Matcher m = INDEX.matcher(key);
                String arrInd ;
                if(m.find()) {
                    arrInd = m.group(1);
                    key = key.replace("["+arrInd+"]", "");
                } else {
                    arrInd = "0";
                }
                //is an index (e.g. data[0])
                List<Node> nodes = getChildrenByTagName(o, key);
                if(nodes == null || nodes.size() == 0) return null;
                try {
                    if(index == (path.length-1)) {
                        Node text = nodes.get(Integer.valueOf(arrInd));
                        if(text.hasChildNodes() && text.getChildNodes().item(0) != null) {
                            return text.getChildNodes().item(0).getNodeValue();
                        } else {
                            return null;
                        }
                    } else {
                        return followPath(nodes.get(Integer.valueOf(arrInd)), path, index+1);
                    }
                } catch(NumberFormatException e) {
                    //we are going to join the results of everything else on arrInd
                    String[] strings = new String[nodes.size()];
                    if(index == (path.length-1)) {
                        for(int i=0; i<nodes.size(); i++) {
                            Node text = nodes.get(i);
                            if(text.hasChildNodes() && text.getChildNodes().item(0) != null) {
                                strings[i] = text.getChildNodes().item(0).getNodeValue();
                            } else {
                                if(isQuiet()) {
                                    return null;
                                } else {
                                    strings[i] = "NA";
                                }
                            }
                        }
                    } else {
                        for(int i=0; i<nodes.size(); i++) {
                            String s = followPath(nodes.get(i), path, index + 1);
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

            } catch(DOMException e) {
                //Log.e("JSONLookupItem", "followPath: json exception", e);
                //'string not found' is very common, don't log
                return null;
            }
        }

        private List<Node> getChildrenByTagName(Node parent, String tagName) {
            if(parent.hasChildNodes()) {
                List<Node> out = new ArrayList<>();
                NodeList l = parent.getChildNodes();
                for(int i=0; i<l.getLength(); i++) {
                    if(l.item(i).getNodeName().equals(tagName)) {
                        out.add(l.item(i));
                    }
                }
                return out;
            } else {
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
