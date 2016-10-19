package net.fishandwhistle.openpos.extractors;

import android.graphics.Bitmap;
import android.graphics.Color;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;

/**
 * Created by dewey on 2016-10-19.
 */

public abstract class BarcodeExtractor {

    protected BarcodeSpec[] specs;

    public BarcodeExtractor(BarcodeSpec[] specs) {
        this.specs = specs;
    }

    public abstract BarcodeSpec.Barcode extract(byte[] jpegData, int width, int height, int orientation);


    protected static int[] extractLineFromBitmap(Bitmap b, int orientation) {
        if(orientation == 0) {
            int[] vals = new int[b.getHeight()];
            for (int i = 0; i < b.getHeight(); i++) {
                int col = b.getPixel(0, i);
                vals[b.getHeight() - 1 - i] = (Color.red(col) + Color.blue(col) + Color.green(col)) / 256;
            }
            return vals;
        } else if (orientation == 1){
            int[] vals = new int[b.getWidth()];
            for (int i = 0; i < b.getWidth(); i++) {
                int col = b.getPixel(i, 0);
                vals[i] = (Color.red(col) + Color.blue(col) + Color.green(col)) / 256;
            }
            return vals;
        } else if (orientation == 3) {
            int[] vals = new int[b.getWidth()];
            for (int i = 0; i < b.getWidth(); i++) {
                int col = b.getPixel(i, 0);
                vals[b.getWidth()-1-i] = (Color.red(col) + Color.blue(col) + Color.green(col));
            }
            return vals;
        } else if (orientation == 2) {
            int[] vals = new int[b.getHeight()];
            for (int i = 0; i < b.getHeight(); i++) {
                int col = b.getPixel(0, i);
                vals[i] = (Color.red(col) + Color.blue(col) + Color.green(col)) / 256;
            }
            return vals;
        } else {
            throw new RuntimeException("Unsupported rotation detected: " + orientation);
        }
    }

}
