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

    private boolean enableQtyUpdate;
    private Context context;
    private OnItemEditCallback activityCallback;


    public ScannedItemAdapter(Context context, boolean enableQtyUpdate, OnItemEditCallback callback) {
        super(context, R.layout.item_scanner, R.id.item_name);
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
        ImageView minus = (ImageView)v.findViewById(R.id.item_button_minus);
        ImageView plus = (ImageView)v.findViewById(R.id.item_button_plus);
        ImageView loading = (ImageView)v.findViewById(R.id.item_loading);

        if(i.isLoading) {
            loading.setVisibility(View.VISIBLE);
        } else {
            loading.setVisibility(View.GONE);
        }

        if(i.description != null) {
            name.setText(i.description);
        }

        desc.setText(i.productCode);
        qty.setText(String.valueOf(i.nScans));

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

    public int indexOf(ScannedItem item) {
        for(int i=0; i<this.getCount(); i++) {
            if(item.equals(this.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    public interface OnItemEditCallback {
        void onScannerItemDelete(ScannedItem item);
        void onScannerItemQuantity(ScannedItem item);
        void onScannerItemClick(ScannedItem item);
    }

}
