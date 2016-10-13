package net.fishandwhistle.openpos.items;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;

import java.io.Serializable;
import java.util.Locale;

/**
 * Created by dewey on 2016-10-04.
 */

public class ScannedItem implements Serializable {

    public long scanTime = 0;
    public long updateTime = 0;
    public String productCode = null;
    public String barcodeType = null;
    public String description = null;
    public int nScans = 1;
    public double price = 0;

    public String json = null;

    public ScannedItem(String barcodeType, String productCode) {
        this.scanTime = System.currentTimeMillis();
        updateTime = scanTime;
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

    @Override
    public boolean equals(Object othero) {
        return othero instanceof ScannedItem &&
                ((ScannedItem) othero).barcodeType.equals(this.barcodeType) &&
                ((ScannedItem) othero).productCode.equals(this.productCode);
    }
}
