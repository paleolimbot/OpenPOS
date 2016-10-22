package net.fishandwhistle.openpos.settings;

import android.content.SharedPreferences;

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

/**
 * Created by dewey on 2016-10-21.
 */

public class SettingsProfile {

    public String scanBackend = "ZBar"; //also Java
    public BarcodeSpec[] barcodeSpecs = new BarcodeSpec[] {new EAN13Spec(), new Code128Spec(),
            new ITFSpec(), new CodabarSpec(), new Code39Spec(), new EAN8Spec(), new DataBarSpec(),
            new DataBarExpandedSpec()};
    public String scanMode = "Tap to scan"; //also Continuous
    public int[] aperture = new int[] {0, 100, 10, 20}; //like Rect() constructor: left, right, top, bottom according to phone orientation
    public boolean openDialog = true;
    public ScannedItemAction[] itemActions = new ScannedItemAction[] {};

    public SettingsProfile() {

    }

    public void writeToPreferences(SharedPreferences preferences) {

    }

    public void loadFromPreferences(SharedPreferences preferences) {

    }
}
