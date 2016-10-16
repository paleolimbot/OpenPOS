package net.fishandwhistle.openpos.api;

import android.content.Context;

import net.fishandwhistle.openpos.barcode.Checksums;
import net.fishandwhistle.openpos.items.ScannedItem;

/**
 * Created by dewey on 2016-10-16.
 */

public class GTINQuery extends UPCQuery {

    public GTINQuery(Context context, String gtin, ScannedItem item, APICallback callback) {
        super(context, GTINtoEAN(gtin), item, callback);

    }

    private static String GTINtoEAN(String gtin) {
        if(gtin.length() != 14) throw new IllegalArgumentException("GTIN must be 14 digits");
        String upc = "0" + gtin.substring(1, 12);
        int[] numbers = new int[upc.length()+1];
        for(int i=0; i<upc.length(); i++) {
            numbers[i] = Integer.valueOf(upc.substring(i, i+1));
        }
        int checkDigit = Checksums.checksumDigit(numbers, 3, 1);
        return upc + String.valueOf(checkDigit);
    }
}
