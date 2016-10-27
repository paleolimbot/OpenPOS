package net.fishandwhistle.openpos.settings;

import android.content.SharedPreferences;

import net.fishandwhistle.openpos.actions.ActionBlank;
import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.fishandwhistle.openpos.barcode.CodabarSpec;
import net.fishandwhistle.openpos.barcode.Code128Spec;
import net.fishandwhistle.openpos.barcode.Code39Spec;
import net.fishandwhistle.openpos.barcode.DataBarExpandedSpec;
import net.fishandwhistle.openpos.barcode.DataBarSpec;
import net.fishandwhistle.openpos.barcode.EAN13Spec;
import net.fishandwhistle.openpos.barcode.EAN8Spec;
import net.fishandwhistle.openpos.barcode.ITFSpec;
import net.fishandwhistle.openpos.actions.ScannedItemAction;
import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by dewey on 2016-10-21.
 */

public class SettingsProfile {

    public String scanBackend;
    public BarcodeSpec[] barcodeSpecs;
    public String scanMode; //also Continuous
    public int[] aperture; //like Rect() constructor: left, right, top, bottom according to phone orientation

    public ScannedItemAction onNewBarcode;
    public ScannedItemAction onRepeatBarcode;
    public ScannedItemAction onClick;

    public SettingsProfile() {
        scanBackend = "ZBar"; //also Java
        barcodeSpecs = new BarcodeSpec[] {new EAN13Spec(), new Code128Spec(),
                new ITFSpec(), new CodabarSpec(), new Code39Spec(), new EAN8Spec(), new DataBarSpec(),
                new DataBarExpandedSpec()};
        scanMode = "Tap to scan";
        aperture = new int[] {0, 100, 10, 20};
        onNewBarcode = new ActionBlank(new JSONObject());
        onRepeatBarcode = new ActionBlank(new JSONObject());
        onClick = new ActionBlank(new JSONObject());
    }

    public void writeToPreferences(SharedPreferences preferences) {

    }

    public void loadFromPreferences(SharedPreferences preferences) {

    }
}
