package net.fishandwhistle.openpos.extractors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by dewey on 2016-10-19.
 */

public class JavaExtractor extends BarcodeExtractor {


    public JavaExtractor(BarcodeSpec[] specs) {
        super(specs);
    }

    public BarcodeSpec.Barcode extractYUV(byte[] yuvData, int width, int height, int orientation, Rect decodeRegion) throws IOException {
        YuvImage y = new YuvImage(yuvData, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        y.compressToJpeg(decodeRegion, 95, fos);
        byte[] jpegData = fos.toByteArray();
        Bitmap b = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
        int[] vals = extractLineFromBitmap(b, orientation);
        b.recycle();
        return new ThresholdMultiExtractor(vals).multiExtract(specs, false);
    }

    @Override
    public BarcodeSpec.Barcode extractJPEG(byte[] jpegData, int width, int height, int orientation, Rect decodeRegion) throws IOException {
        BitmapRegionDecoder regionDecoder = BitmapRegionDecoder.newInstance(jpegData, 0, jpegData.length, true);
        Bitmap b = regionDecoder.decodeRegion(decodeRegion, new BitmapFactory.Options());
        int[] vals = extractLineFromBitmap(b, orientation);
        b.recycle();
        return new ThresholdMultiExtractor(vals).multiExtract(specs, true);
    }


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
