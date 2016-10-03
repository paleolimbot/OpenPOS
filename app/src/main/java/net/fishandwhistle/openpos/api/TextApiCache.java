package net.fishandwhistle.openpos.api;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by dewey on 2016-10-02.
 */

public class TextApiCache {

    private static final String TAG = "TextApiCache";

    private Context context ;

    public TextApiCache(Context context) {
        this.context = context;
    }

    private File getFile(String url) {
        return new File(context.getCacheDir(), digest(url));
    }

    private boolean contains(String url) {
        return getFile(url).exists();
    }

    public String get(String url) {
        File f = getFile(url);
        if(f.exists()) {
            // read file
            try {
                FileInputStream fis = new FileInputStream(f);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[256];
                int count = 0;
                while ((count = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, count);
                }
                fis.close();
                String out = baos.toString("UTF-8");
                baos.close();
                return out;
            } catch(IOException e) {
                Log.e(TAG, "get: IO exception", e);
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean put(String url, String data) {
        File f = getFile(url);

        // write file
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(data.getBytes("UTF-8"));
            fos.close();
            return true;
        } catch(IOException e) {
            Log.e(TAG, "get: IO exception", e);
            return false;
        }
    }


    private static String digest(String in) {
        try {
            MessageDigest d = MessageDigest.getInstance("MD5");
            byte[] b = d.digest(in.getBytes("UTF-8"));
            BigInteger bigInt = new BigInteger(1, b);
            return bigInt.toString(16);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "digest: no md5 digest algorithm");
            throw new RuntimeException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "digest: no utf8 encoding");
            throw new RuntimeException(e.getMessage());
        }
    }
}
