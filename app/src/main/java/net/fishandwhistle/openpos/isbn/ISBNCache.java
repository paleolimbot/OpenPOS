package net.fishandwhistle.openpos.isbn;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dewey on 2016-10-02.
 */

public class ISBNCache {

    private Map<String, String> memoryCache;

    public ISBNCache(Context context) {
        memoryCache = new HashMap<>();
    }

    public String get(String isbn) {
        if(memoryCache.containsKey(isbn)) {
            return memoryCache.get(isbn);
        } else {
            return null;
        }
    }

    public void put(String isbn, String response) {
        memoryCache.put(isbn, response);
    }

}
