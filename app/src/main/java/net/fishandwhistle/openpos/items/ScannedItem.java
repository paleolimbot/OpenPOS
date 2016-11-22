package net.fishandwhistle.openpos.items;

import android.util.Log;

import net.fishandwhistle.openpos.actions.Formatting;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by dewey on 2016-10-04.
 */

public class ScannedItem implements Serializable, Formatting.Formattable {
    private static final String TAG = "ScannedItem";

    public static final String KEY_BARCODE_TEXT = "_barcode_text";
    public static final String KEY_DESCRIPTION = "_description";
    public static final String KEY_SUBTEXT = "_subtext";
    public static final String KEY_BARCODE_TYPE = "_barcode_type";

    public String session = "default";
    public long dbId = 0;
    public long scanTime = 0;
    public long updateTime = 0;
    public String barcodeText = null;
    public String barcodeType = null;
    public String description = null;
    public String subtext = null;
    public int nScans = 1;
    public boolean isLoading = false;

    private transient JSONObject jsonObject;
    private String json = "{}";
    private ArrayList<String> keys;

    public ScannedItem(String barcodeType, String barcodeText) {
        this.scanTime = System.currentTimeMillis();
        updateTime = scanTime;
        this.barcodeType = barcodeType;
        this.barcodeText = barcodeText;
        keys = new ArrayList<>();
        setJSON(this.json);
    }

    @Override
    public String toString() {
        return String.format("%s XX%s", this.barcodeType, this.barcodeText.substring(Math.max(0, barcodeText.length()-4)));
    }

    public List<String> getKeys() {
        List<String> out = new ArrayList<>();
        for(String key: keys) out.add(key);
        return out;
    }

    @Override
    public String getValue(String key) {
        switch (key) {
            case KEY_BARCODE_TEXT:
                return this.barcodeText;
            case KEY_DESCRIPTION:
                return this.description;
            case KEY_SUBTEXT:
                return this.subtext;
            case KEY_BARCODE_TYPE:
                return this.barcodeType;
            default:
                try {
                    return jsonObject.getString(key);
                } catch (JSONException e) {
                    return null;
                }
        }
    }

    public void putValue(String key, String value) {
        switch(key) {
            case KEY_BARCODE_TEXT:
                throw new IllegalArgumentException("Setting of " + key  + " not allowed");
            case KEY_DESCRIPTION:
                this.description = value;
                return;
            case KEY_SUBTEXT:
                this.subtext = value;
                return;
            case KEY_BARCODE_TYPE:
                throw new IllegalArgumentException("Setting of " + key  + " not allowed");
            default:
                try {
                    if(value == null) {
                        if(keys.contains(key)) {
                            jsonObject.remove(key);
                            keys.remove(key);
                        }
                    } else {
                        boolean addkey = !keys.contains(key);
                        jsonObject.put(key, value);
                        if (addkey) keys.add(key);
                    }
                } catch(JSONException e) {
                    throw new IllegalArgumentException("JSON Error thrown on setting value " + value + "(" + e.getMessage() + ")");
                }
        }
    }

    public String getJSON() {
        return jsonObject.toString();
    }

    public void setJSON(String json) {
        try {
            jsonObject = new JSONObject(json);
            this.json = json;
            keys = new ArrayList<>();
            Iterator<String> jsonKeys = jsonObject.keys();
            while(jsonKeys.hasNext())
                keys.add(jsonKeys.next());
        } catch(JSONException e) {
            Log.e(TAG, "ScannedItem: JSON exception on constructor", e);
            throw new IllegalArgumentException("Illegal JSON " + json + "(" + e.getMessage() + ")");
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        this.json = jsonObject.toString();
        stream.defaultWriteObject();
    }

    @Override
    public boolean equals(Object othero) {
        return othero instanceof ScannedItem &&
                ((ScannedItem) othero).barcodeType.equals(this.barcodeType) &&
                ((ScannedItem) othero).barcodeText.equals(this.barcodeText);
    }

}
