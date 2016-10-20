package net.fishandwhistle.openpos.extractors;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;

import java.io.IOException;

/**
 * Created by dewey on 2016-10-19.
 */

public abstract class BarcodeExtractor {

    protected BarcodeSpec[] specs;

    public BarcodeExtractor(BarcodeSpec[] specs) {
        this.specs = specs;
    }

    public abstract BarcodeSpec.Barcode extractYUV(byte[] yuvData, int width, int height, int orientation, Rect decodeRegion) throws IOException;

    public abstract BarcodeSpec.Barcode extractJPEG(byte[] jpegData, int width, int height, int orientation, Rect decodeRegion) throws IOException;

}
