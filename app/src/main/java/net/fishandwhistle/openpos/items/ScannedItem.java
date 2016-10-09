package net.fishandwhistle.openpos.items;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;

import java.io.Serializable;
import java.util.Locale;

/**
 * Created by dewey on 2016-10-04.
 */

public class ScannedItem implements Serializable {

    public long scanTime = 0;
    public String productCode = null;
    public String barcodeType = null;
    public String description = null;
    public double price = 0;

    public String json = null;

    public ScannedItem(String barcodeType, String productCode) {
        this.scanTime = System.currentTimeMillis();
        this.barcodeType = barcodeType;
        this.productCode = productCode;
    }

    public String toString() {

        String d ;
        if(description == null) {
            d = barcodeType + ":" + productCode;
        } else {
            d = description;
        }
        return d;
    }
}
