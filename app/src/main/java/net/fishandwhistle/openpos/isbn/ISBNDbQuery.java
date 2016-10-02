package net.fishandwhistle.openpos.isbn;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;

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

public class ISBNDbQuery {

    private static Set<String> currentRequests = new HashSet<>();

    private String isbnNum ;
    private ISBNCallback callback ;
    private Context context ;
    private DownloadTask task;

    public ISBNDbQuery(Context context, String isbn, ISBNCallback callback) {
        this.isbnNum = isbn;
        this.callback = callback;
        this.context = context;
    }

    public boolean query() {
        String apikey = "T89SFTZN";
        String url = "http://isbndb.com/api/v2/json/" + apikey + "/book/" + isbnNum;
        if(currentRequests.contains(url)) {
            Log.i("ISBNDbQuery", "Disregarding redundant request: " + isbnNum);
            return false;
        } else {
            currentRequests.add(url);
            task = new DownloadTask(context);
            task.execute(url);
            return true;
        }
    }

    public interface ISBNCallback {
        void onISBNQueried(String isbn, String title);
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            String out = null;
            InputStream input = null;
            ByteArrayOutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                Log.i("ISBNDbQuery", "starting download from " + url.toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    currentRequests.remove(sUrl[0]);
                    Log.e("ISBNDbQuery", "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                    return null;
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
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
                Log.i("ISBNDbQuery", "Download complete");
            } catch (Exception e) {
                Log.e("ISBNDbQuery", "Exception in download", e);
                currentRequests.remove(sUrl[0]);
                return null;
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
                return null;
            }

            // do parsing
            try {
                Log.i("ISBNDbQuery", "Parsing JSON data");
                JSONObject o = new JSONObject(out);
                if(o.has("error")) {
                    Log.e("ISBNDbQuery", "Error from database: " + o.getString("error"));
                    currentRequests.remove(sUrl[0]);
                    return null;
                } else {
                    JSONArray a = o.getJSONArray("data");
                    JSONObject book = a.getJSONObject(0);
                    currentRequests.remove(sUrl[0]);
                    return book.getString("title");
                }
            } catch(JSONException e) {
                Log.e("ISBNDbQuery", "Error parsing JSON", e);
                currentRequests.remove(sUrl[0]);
                return null;
            }

        }

        @Override
        protected void onPostExecute(String s) {
            if(s==null) {
                Log.e("ISBNDbQuery", "onPostExecute: null string");
            }
            callback.onISBNQueried(isbnNum, s);
        }
    }

}
