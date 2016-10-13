package net.fishandwhistle.openpos.items;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.fishandwhistle.openpos.R;

import java.util.ArrayList;

/**
 * Created by dewey on 2016-10-04.
 */

public class ScannedItemAdapter extends ArrayAdapter<ScannedItem> {

    private ArrayList<ScannedItem> allItems ;
    private int maxLength;
    private boolean enableQtyUpdate;

    public ScannedItemAdapter(Context context, boolean enableQtyUpdate) {
        super(context, R.layout.item_scanner, R.id.item_text);
        allItems = new ArrayList<>();
        this.maxLength = 0;
        this.enableQtyUpdate = enableQtyUpdate;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        final ScannedItem i = this.getItem(position);
        assert i != null;
        ((TextView)v.findViewById(R.id.item_qty)).setText(String.valueOf(i.nScans));
        if(enableQtyUpdate) {
            v.findViewById(R.id.item_button_minus).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    i.nScans--;
                    i.updateTime = System.currentTimeMillis();
                    ScannedItemAdapter.this.notifyDataSetInvalidated();
                }
            });
            v.findViewById(R.id.item_button_plus).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    i.nScans++;
                    i.updateTime = System.currentTimeMillis();
                    ScannedItemAdapter.this.notifyDataSetInvalidated();
                }
            });
        }
        v.findViewById(R.id.item_button_minus).setEnabled(enableQtyUpdate);
        v.findViewById(R.id.item_button_plus).setEnabled(enableQtyUpdate);


        return v;
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
