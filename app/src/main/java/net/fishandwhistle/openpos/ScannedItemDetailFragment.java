package net.fishandwhistle.openpos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * A placeholder fragment containing a simple view.
 */
public class ScannedItemDetailFragment extends DialogFragment {
    private static final String TAG = "ScannedItemDetailFragme";
    public static final String ARG_ITEM = "arg_item";
    private ScannedItem item;

    public ScannedItemDetailFragment() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_activity_scanned_item_detail);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setPositiveButton(R.string.detail_close, null);
        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_scanned_item_detail, null);

        Serializable s ;
        if(getArguments() != null && getArguments().containsKey(ARG_ITEM)) {
            s = getArguments().getSerializable(ARG_ITEM);
        } else if(getActivity() != null && getActivity().getIntent() != null) {
            s = getActivity().getIntent().getSerializableExtra(ARG_ITEM);
        } else {
            s = null;
        }

        if(s != null) {
            setItem((ScannedItem) s, v);
        } else {
            setItem(null, v);
        }
        builder.setView(v);

        return builder.create();
    }

    private void setItem(ScannedItem item, View v) {
        this.item = item;
        TableLayout kv = (TableLayout)v.findViewById(R.id.detail_keyvalues);
        kv.removeAllViews();

        if(item != null) {
            if(item.description != null) {
                ((TextView) v.findViewById(R.id.detail_description)).setText(item.description);
            } else {
                ((TextView) v.findViewById(R.id.detail_description)).setText(item.toString());
            }
            ((TextView)v.findViewById(R.id.detail_code)).setText(item.productCode);
            ((TextView)v.findViewById(R.id.detail_quantity)).setText(String.format(getString(R.string.detail_qty), item.nScans));

            ArrayList<String[]> vals = new ArrayList<>();
            vals.add(new String[] {getString(R.string.detail_codetype), item.barcodeType});
            vals.add(new String[] {getString(R.string.detail_scantime), formatTime(item.updateTime)});

            List<String> jsonKeys = item.getKeys();
            for(String key: jsonKeys) {
                if(key.startsWith("time_")) {
                    vals.add(new String[] {key, formatTime(item.getValue(key))});
                } else {
                    vals.add(new String[]{key, item.getValue(key)});
                }
            }

            LayoutInflater inflater = getActivity().getLayoutInflater();
            for(String[] keyval: vals) {
                View row = inflater.inflate(R.layout.scanned_item_details_kv, null);
                ((TextView)row.findViewById(R.id.key)).setText(keyval[0]);
                ((TextView)row.findViewById(R.id.value)).setText(keyval[1]);
                kv.addView(row);
            }

        } else {
            ((TextView)v.findViewById(R.id.detail_description)).setText(getString(R.string.detail_noitem));
        }
    }

    public static ScannedItemDetailFragment newInstance(ScannedItem item) {

        Bundle args = new Bundle();
        args.putSerializable(ARG_ITEM, item);
        ScannedItemDetailFragment fragment = new ScannedItemDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private String formatTime(String time) {
        try {
            return formatTime(Long.valueOf(time));
        } catch(NumberFormatException e) {
            Log.e(TAG, "Number format exception in formatTime", e);
            return time;
        }
    }

    private String formatTime(long time) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(time));

    }
}
