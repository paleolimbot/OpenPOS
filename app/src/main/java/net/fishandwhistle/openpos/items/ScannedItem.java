package net.fishandwhistle.openpos.items;

import android.util.Log;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by dewey on 2016-10-04.
 */

public class ScannedItem implements Serializable {
    private static final String TAG = "ScannedItem";

    public long scanTime = 0;
    public long updateTime = 0;
    public String productCode = null;
    public String barcodeType = null;
    public String description = null;
    public int nScans = 1;
    public boolean isLoading = false;

    private transient JSONObject jsonObject;
    private String json = "{}";
    private ArrayList<String> keys;

    public ScannedItem(String barcodeType, String productCode) {
        this.scanTime = System.currentTimeMillis();
        updateTime = scanTime;
        this.barcodeType = barcodeType;
        this.productCode = productCode;
        keys = new ArrayList<>();
        setJSON(this.json);
    }

    @Override
    public String toString() {
        return String.format("%s XX%s", this.barcodeType, this.productCode.substring(Math.max(0, productCode.length()-4)));
    }

    public List<String> getKeys() {
        List<String> out = new ArrayList<>();
        for(String key: keys) out.add(key);
        return out;
    }

    public String getValue(String key) {
        try {
            return jsonObject.getString(key);
        } catch(JSONException e) {
            return null;
        }
    }

    public void putValue(String key, String value) {
        try {
            jsonObject.put(key, value);
            keys.add(key);
        } catch(JSONException e) {
            throw new IllegalArgumentException("JSON Error thrown on setting value " + value + "(" + e.getMessage() + ")");
        }
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
                ((ScannedItem) othero).productCode.equals(this.productCode);
    }

}
