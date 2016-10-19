package net.fishandwhistle.openpos.extractors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;

/**
 * Created by dewey on 2016-10-19.
 */

public class JavaExtractor extends BarcodeExtractor {


    public JavaExtractor(BarcodeSpec[] specs) {
        super(specs);
    }

    @Override
    public BarcodeSpec.Barcode extract(byte[] jpegData, int width, int height, int orientation) {
        Bitmap b = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
        int[] vals = extractLineFromBitmap(b, orientation);
        b.recycle();
        return new ThresholdMultiExtractor(vals).multiExtract(specs, width>2000);
    }

}
