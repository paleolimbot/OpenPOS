package net.fishandwhistle.openpos.items;

import android.content.Context;
import android.widget.ArrayAdapter;

/**
 * Created by dewey on 2016-10-04.
 */

public class ScannedItemAdapter extends ArrayAdapter<ScannedItem> {

    public ScannedItemAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1);
    }

}
