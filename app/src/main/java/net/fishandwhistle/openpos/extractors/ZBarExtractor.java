package net.fishandwhistle.openpos.extractors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.text.TextUtils;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by dewey on 2016-10-19.
 */

public class ZBarExtractor extends BarcodeExtractor {

    private int[] symbols;

    public ZBarExtractor(BarcodeSpec[] specs) {
        super(specs);
        symbols = new int[] {Symbol.CODABAR, Symbol.CODE39, Symbol.EAN13, Symbol.CODE128, Symbol.UPCE, Symbol.EAN8};
    }

    private ImageScanner getScanner(int width, int height) {
        ImageScanner mScanner = new ImageScanner();
        if(width > height) {
            mScanner.setConfig(0, Config.X_DENSITY, 1);
            mScanner.setConfig(0, Config.Y_DENSITY, 0);
        } else {
            mScanner.setConfig(0, Config.X_DENSITY, 0);
            mScanner.setConfig(0, Config.Y_DENSITY, 1);
        }
        mScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
        for (int symbol : symbols) {
            mScanner.setConfig(symbol, Config.ENABLE, 1);
        }
        return mScanner;
    }

    @Override
    public BarcodeSpec.Barcode extractYUV(byte[] yuvData, int width, int height, int orientation, Rect decodeRegion) {
        Image image = new Image(decodeRegion.width(), decodeRegion.height(), "Y800");
        image.setData(cropYuv(yuvData, width, height, orientation, decodeRegion));
        return parseImage(image, getScanner(width, height));
    }

    @Override
    public BarcodeSpec.Barcode extractJPEG(byte[] jpegData, int width, int height, int orientation, Rect decodeRegion) throws IOException {
        BitmapRegionDecoder regionDecoder = BitmapRegionDecoder.newInstance(jpegData, 0, jpegData.length, true);
        Bitmap b = regionDecoder.decodeRegion(decodeRegion, new BitmapFactory.Options());
        Image image = new Image(decodeRegion.width(), decodeRegion.height(), "Y800");
        //temp debug: write YUV image to disk
        //File f = new File(Environment.getExternalStorageDirectory(), "temppic.jpg");
        //FileOutputStream fos = new FileOutputStream(f);
        byte[] yuv = getYuv(b);
        //YuvImage y = new YuvImage(yuv, ImageFormat.NV21, b.getWidth(), b.getHeight(), null);
        //y.compressToJpeg(new Rect(0, 0, b.getWidth(), b.getHeight()), 95, fos);
        image.setData(yuv);
        return parseImage(image, getScanner(width, height));
    }

    private BarcodeSpec.Barcode parseImage(Image image, ImageScanner scanner) {
        BarcodeSpec.Barcode b = new BarcodeSpec.Barcode("ZBar");

        int result = scanner.scanImage(image);
        if(result != 0) {
            SymbolSet syms = scanner.getResults();
            for (Symbol sym : syms) {
                String symData = sym.getData();
                int type = sym.getType();
                if (!TextUtils.isEmpty(symData)) {
                    b.type = "Zbar" + type;
                    for(int i=0; i<symData.length(); i++) {
                        b.digits.add(new BarcodeSpec.BarcodeDigit(symData.substring(i, i+1)));
                    }
                    b.isValid = true;
                    return b;
                }
            }
            b.tag = "Results were >0, but there was no value returned";
            return b;
        } else {
            b.tag = "No result";
            return b;
        }
    }

    private static byte[] cropYuv(byte[] yuvData, int width, int height, int orientation, Rect decodeRegion) {
        //crops YUV data without regard for U or V (because we are converting to grayscale anyway)
        //see https://en.wikipedia.org/wiki/YUV#Y.E2.80.B2UV420p_.28and_Y.E2.80.B2V12_or_YV12.29_to_RGB888_conversion
        int newSize = decodeRegion.width() * decodeRegion.height();
        byte[] yuvOut = new byte[newSize + 2*newSize/4];
        int i=0;
        for(int y=decodeRegion.top; y<decodeRegion.bottom; y++) {
            for (int x=decodeRegion.left; x<decodeRegion.right; x++) {
                yuvOut[i] = yuvData[y * width + x];
                i++;
            }
        }
        return yuvOut;
    }

    private static byte[] getYuv(Bitmap b) {
        //gets YUV from a bitmap, ignoring U and V
        int width = b.getWidth();
        int height = b.getHeight();
        int size = width*height;
        byte[] yuv = new byte[size + 2*size/4];
        for(int i=0; i<size; i++) {
            int color = b.getPixel(i%width, i/width);
            double y = Color.red(color)*0.299 + Color.green(color)*0.587 + Color.blue(color)*0.114;
            yuv[i] = (byte)((int)(255-16*Math.pow(255-y, 0.5))); // gamma correction
        }
        //this doesn't seem to matter for the preview images, but helps with interpretation
        for(int i=size; i<yuv.length; i++) {
            yuv[i] = (byte)128;
        }
        return yuv;
    }


    protected static int[] yuvIndex(int size, int width, int x, int y) {
        return new int[] {y * size + x,
                (y / 2) * (width / 2) + (x / 2) + size,
                (y / 2) * (width / 2) + (x / 2) + size + (size / 4)};
    }

}
