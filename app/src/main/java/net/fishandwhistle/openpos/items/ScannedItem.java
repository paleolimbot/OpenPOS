package net.fishandwhistle.openpos.items;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;

import java.util.Locale;

/**
 * Created by dewey on 2016-10-04.
 */

public class ScannedItem {

    public long scanTime = 0;
    public String productCode = null;
    public String description = null;
    public double price = 0;

    public String json = null;

    public ScannedItem(String productCode) {
        this.scanTime = System.currentTimeMillis();
        this.productCode = productCode;
    }

    public String toString() {

        String d ;
        if(description == null) {
            d = productCode;
        } else {
            d = description;
        }

        return String.format(Locale.CANADA, "$%.2f %s", price, d);
    }
}
