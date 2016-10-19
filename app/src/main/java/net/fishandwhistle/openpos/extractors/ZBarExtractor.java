package net.fishandwhistle.openpos.extractors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    public BarcodeSpec.Barcode extract(byte[] jpegData, int width, int height, int orientation) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
        int[] vals = extractLineFromBitmap(bitmap, orientation);
        Image image = new Image(vals.length, 1, "GRAY");
        image.setData(vals);
        bitmap.recycle();
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

}
