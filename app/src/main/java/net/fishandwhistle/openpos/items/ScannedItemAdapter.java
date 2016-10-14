package net.fishandwhistle.openpos.items;

import android.content.Context;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.fishandwhistle.openpos.R;

import java.util.ArrayList;

/**
 * Created by dewey on 2016-10-04.
 */

public class ScannedItemAdapter extends ArrayAdapter<ScannedItem> {

    private ArrayList<ScannedItem> allItems ;
    private int maxLength;
    private boolean enableQtyUpdate;
    private Context context;
    private OnItemEditCallback activityCallback;


    public ScannedItemAdapter(Context context, boolean enableQtyUpdate, OnItemEditCallback callback) {
        super(context, R.layout.item_scanner, R.id.item_desc);
        allItems = new ArrayList<>();
        this.maxLength = 0;
        this.enableQtyUpdate = enableQtyUpdate;
        this.context = context;
        this.activityCallback = callback;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        final ScannedItem i = this.getItem(position);
        assert i != null;
        Button qty = (Button)v.findViewById(R.id.item_qty);
        TextView name = (TextView)v.findViewById(R.id.item_name);
        TextView desc = (TextView)v.findViewById(R.id.item_desc);
        name.setText(String.format("Unknown %s", i.barcodeType));
        desc.setText(i.productCode);
        qty.setText(String.valueOf(i.nScans));

        ImageView minus = (ImageView)v.findViewById(R.id.item_button_minus);
        ImageView plus = (ImageView)v.findViewById(R.id.item_button_plus);
        if(i.nScans > 1) {
            minus.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_item_remove));
        } else {
            minus.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_item_delete));

        }
        if(enableQtyUpdate) {
            minus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if((activityCallback != null) && ((i.nScans - 1) == 0)) {
                        activityCallback.onScannerItemDelete(i);
                    } else {
                        ((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
                        i.nScans--;
                        i.updateTime = System.currentTimeMillis();
                        ScannedItemAdapter.this.notifyDataSetInvalidated();
                    }

                }
            });
            plus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    i.nScans++;
                    i.updateTime = System.currentTimeMillis();
                    ((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
                    ScannedItemAdapter.this.notifyDataSetInvalidated();
                }
            });
            qty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(activityCallback != null) {
                        activityCallback.onScannerItemQuantity(i);
                    }
                }
            });

        }

        minus.setEnabled(enableQtyUpdate);
        plus.setEnabled(enableQtyUpdate);
        v.findViewById(R.id.item_textwrap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(activityCallback!= null) {
                    activityCallback.onScannerItemClick(i);
                }
            }
        });

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
    public void remove(ScannedItem object) {
        allItems.remove(object);
        syncLists();
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

    public interface OnItemEditCallback {
        void onScannerItemDelete(ScannedItem item);
        void onScannerItemQuantity(ScannedItem item);
        void onScannerItemClick(ScannedItem item);
    }

}
