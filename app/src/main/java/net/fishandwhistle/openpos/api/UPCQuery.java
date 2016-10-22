package net.fishandwhistle.openpos.api;

import android.content.Context;
import android.util.Log;

import net.fishandwhistle.openpos.R;
import net.fishandwhistle.openpos.actions.JSONLookupItem;
import net.fishandwhistle.openpos.actions.URILookupAction;
import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by dewey on 2016-10-03.
 */

public class UPCQuery extends JSONLookupItem {

    public UPCQuery() {
        super("UPC-Lookup", "http://api.upcdatabase.org/json/2bf423e9a6b6800ff83ab88d74856501/{{gtin13}}", null);
    }

}
