package net.fishandwhistle.openpos.extractors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.text.TextUtils;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;


/**
 * Created by dewey on 2016-10-19.
 */

public class ZBarExtractor extends BarcodeExtractor {

    private ImageScanner mScanner;

    public ZBarExtractor(BarcodeSpec[] specs) {
        super(specs);
        int[] symbols = new int[] {Symbol.CODABAR, Symbol.CODE39, Symbol.EAN13, Symbol.CODE128, Symbol.UPCE, Symbol.EAN8};

        mScanner = new ImageScanner();
        mScanner.setConfig(0, Config.X_DENSITY, 3);
        mScanner.setConfig(0, Config.Y_DENSITY, 3);

        mScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
        for (int symbol : symbols) {
            mScanner.setConfig(symbol, Config.ENABLE, 1);
        }
    }

    @Override
    public BarcodeSpec.Barcode extractYUV(byte[] yuvData, int width, int height, int orientation, Rect decodeRegion) {
        Image image = new Image(decodeRegion.width(), decodeRegion.height(), "Y800");
        image.setData(cropYuv(yuvData, width, height, orientation, decodeRegion));
        return parseImage(image);
    }

    @Override
    public BarcodeSpec.Barcode extractJPEG(byte[] jpegData, int width, int height, int orientation, Rect decodeRegion) {
        return new BarcodeSpec.Barcode("invalid");
    }

    private BarcodeSpec.Barcode parseImage(Image image) {
        BarcodeSpec.Barcode b = new BarcodeSpec.Barcode("ZBar");

        int result = mScanner.scanImage(image);
        if(result != 0) {
            SymbolSet syms = mScanner.getResults();
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

    protected byte[] cropYuv(byte[] yuvData, int width, int height, int orientation, Rect decodeRegion) {
        int newSize = decodeRegion.width() * decodeRegion.height();
        int oldSize = width * height;
        byte[] yuvOut = new byte[newSize + 2*newSize/4];
        int i=0;
//        int j=newSize;
//        int k=newSize + newSize/4;
        for(int y=decodeRegion.top; y<decodeRegion.bottom; y++) {
            for (int x=decodeRegion.left; x<decodeRegion.right; x++) {
                yuvOut[i] = yuvData[y * width + x];
                i++;
//                if((y%2==0) && (x%2==0)) {
//                    yuvOut[j] = yuvData[(y / 2) * (width / 2) + (x / 2) + oldSize];
//                    yuvOut[k] = yuvData[(y / 2) * (width / 2) + (x / 2) + oldSize + (oldSize / 4)];
//                    j++; k++;
//                }
            }
        }

        return yuvOut;
    }

    protected static int[] yuvIndex(int size, int width, int x, int y) {
        return new int[] {y * size + x,
                (y / 2) * (width / 2) + (x / 2) + size,
                (y / 2) * (width / 2) + (x / 2) + size + (size / 4)};
    }

}
