package net.fishandwhistle.openpos.extractors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.TextUtils;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.fishandwhistle.openpos.barcode.CodabarSpec;
import net.fishandwhistle.openpos.barcode.Code128Spec;
import net.fishandwhistle.openpos.barcode.Code39Spec;
import net.fishandwhistle.openpos.barcode.Code93Spec;
import net.fishandwhistle.openpos.barcode.DataBarExpandedSpec;
import net.fishandwhistle.openpos.barcode.DataBarSpec;
import net.fishandwhistle.openpos.barcode.EAN13Spec;
import net.fishandwhistle.openpos.barcode.EAN8Spec;
import net.fishandwhistle.openpos.barcode.ITFSpec;
import net.fishandwhistle.openpos.barcode.UPCESpec;
import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.IOException;

import static net.fishandwhistle.openpos.barcode.ArrayMath.filter;


/**
 * Created by dewey on 2016-10-19.
 */

public class ZBarExtractor extends BarcodeExtractor {

    private int[] symbols;

    public ZBarExtractor(BarcodeSpec[] specs) {
        super(specs);
        symbols = new int[specs.length] ;
        for(int i=0; i<symbols.length; i++) {
            symbols[i] = getZbarId(specs[i]);
        }
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
        image.setData(getYuv(b));
        b.recycle();
        return parseImage(image, getScanner(width, height));
    }

    private BarcodeSpec.Barcode parseImage(Image image, ImageScanner scanner) {
        int result = scanner.scanImage(image);
        if(result != 0) {
            SymbolSet syms = scanner.getResults();
            for (Symbol sym : syms) {
                String symData = sym.getData();
                int type = sym.getType();
                if (!TextUtils.isEmpty(symData)) {
                    BarcodeSpec s = getSpec(type);
                    BarcodeSpec.Barcode b = new BarcodeSpec.Barcode(s.getType());
                    //TODO may want to validate based on digits here
                    if(sym.getModifierMask() == 1) {
                        b.digits.add(new BarcodeSpec.BarcodeDigit("[FNC1]"));
                        for (int i = 0; i < symData.length(); i++) {
                            char c = symData.charAt(i);
                            if (c == 0x1d) {
                                b.digits.add(new BarcodeSpec.BarcodeDigit("[FNC1]"));
                            } else {
                                b.digits.add(new BarcodeSpec.BarcodeDigit(new String(new char[]{c})));
                            }
                        }
                    } else {
                        for (int i = 0; i < symData.length(); i++) {
                            b.digits.add(new BarcodeSpec.BarcodeDigit(symData.substring(i, i + 1)));
                        }
                    }
                    if(type == Symbol.I25 && b.toString().length() == 14) {
                        b.type = "ITF-14";
                    }
                    b.isValid = true;
                    return b;
                }
            }
            BarcodeSpec.Barcode b = new BarcodeSpec.Barcode("Zbar");
            b.tag = "Results were >0, but there was no value returned";
            return b;

        } else {
            BarcodeSpec.Barcode b = new BarcodeSpec.Barcode("Zbar");
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
        //ignore U and V, probably not used in the zbar library
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
        //for(int i=size; i<yuv.length; i++) {
        //    yuv[i] = (byte)128;
        //}
        return yuv;
    }

    private static byte[] getYuvSmooth(Bitmap b) {
        //gets YUV from a bitmap, ignoring U and V
        int width = b.getWidth();
        int height = b.getHeight();
        int size = width*height;

        if(width > height) {
            //average by y value
            double[] vals = new double[width];
            for(int i=0; i<width; i++) {
                double cumsum = 0;
                for(int j=0; j<height; j++) {
                    int color = b.getPixel(i, j);
                    cumsum += Color.red(color)*0.299 + Color.green(color)*0.587 + Color.blue(color)*0.114;
                }
                vals[i] = cumsum / height;
            }
            double[] smoothed = filter(vals, new double[] {1, 2, 4, 8, 10, 8, 4, 2, 1});
            byte[] yuv = new byte[size + 2*size/4];
            for(int i=0; i<size; i++) {
                double y = smoothed[i%width];
                yuv[i] = (byte)((int)(255-16*Math.pow(255-y, 0.5))); // gamma correction
            }
            return yuv;
        } else {
            //average by x value
            double[] vals = new double[height];
            for(int i=0; i<height; i++) {
                double cumsum = 0;
                for(int j=0; j<width; j++) {
                    int color = b.getPixel(j, i);
                    cumsum += Color.red(color)*0.299 + Color.green(color)*0.587 + Color.blue(color)*0.114;
                }
                vals[i] = cumsum / width;
            }
            double[] smoothed = filter(vals, new double[] {1, 2, 4, 8, 10, 8, 4, 2, 1});
            byte[] yuv = new byte[size + 2*size/4];
            for(int i=0; i<size; i++) {
                double y = vals[i/width];
                yuv[i] = (byte)((int)(255-16*Math.pow(255-y, 0.5))); // gamma correction
            }
            return yuv;
        }
    }

    protected static int[] yuvIndex(int size, int width, int x, int y) {
        return new int[] {y * size + x,
                (y / 2) * (width / 2) + (x / 2) + size,
                (y / 2) * (width / 2) + (x / 2) + size + (size / 4)};
    }

    private static BarcodeSpec getSpec(int zbarId) {
        switch (zbarId) {
            case Symbol.CODABAR:
                return new CodabarSpec();
            case Symbol.CODE128:
                return new Code128Spec();
            case Symbol.EAN8:
                return new EAN8Spec();
            case Symbol.EAN13:
                return new EAN13Spec();
            case Symbol.CODE39:
                return new Code39Spec();
            case Symbol.I25:
                return new ITFSpec();
            case Symbol.UPCE:
                return new UPCESpec();
            case Symbol.CODE93:
                return new Code93Spec();
            case Symbol.DATABAR:
                return new DataBarSpec();
            case Symbol.DATABAR_EXP:
                return new DataBarExpandedSpec();
            default: throw new RuntimeException("No java spec found for zbar id: " + zbarId);
        }
    }

    private static int getZbarId(BarcodeSpec spec) {
        switch(spec.getType()) {
            case "Codabar":
                return Symbol.CODABAR;
            case "Code128":
                return Symbol.CODE128;
            case "EAN-8":
                return Symbol.EAN8;
            case "EAN-13":
                return Symbol.EAN13;
            case "Code39":
                return Symbol.CODE39;
            case "ITF":
                return Symbol.I25;
            case "UPC-E":
                return Symbol.UPCE;
            case "DataBar":
                return Symbol.DATABAR;
            case "DataBarExt":
                return Symbol.DATABAR_EXP;
            default: return -1;
        }
    }

}
