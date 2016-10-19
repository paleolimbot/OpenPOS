package net.fishandwhistle.openpos.extractors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
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



}
