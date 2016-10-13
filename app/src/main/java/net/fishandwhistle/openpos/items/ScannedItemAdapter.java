package net.fishandwhistle.openpos.items;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * Created by dewey on 2016-10-04.
 */

public class ScannedItemAdapter extends ArrayAdapter<ScannedItem> {

    private ArrayList<ScannedItem> allItems ;
    private int maxLength;

    public ScannedItemAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1);
        allItems = new ArrayList<>();
        this.maxLength = 0;
    }

    @Override
    public void add(ScannedItem object) {
        if(allItems.contains(object)) {
            int ind = allItems.indexOf(object);
            ScannedItem i = allItems.remove(ind);
            i.nScans += 1;
            allItems.add(i);
        } else {
            allItems.add(object);
        }
        this.syncLists();
    }

    @Override
    public void clear() {
        allItems.clear();
        this.syncLists();
    }

    private void syncLists() {
        super.clear();
        int sizelimit;
        if(maxLength == 0) {
            sizelimit = allItems.size();
        } else {
            sizelimit = maxLength;
        }
        for(int i=0; i<sizelimit; i++) {
            int ind = allItems.size() - sizelimit + i;
            if(ind >= 0) {
                super.add(allItems.get(ind));
            }
        }
    }
}
