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
import java.util.Locale;

/**
 * A placeholder fragment containing a simple view.
 */
public class ScannedItemDetailFragment extends DialogFragment {
    private static final String TAG = "ScannedItemDetailFragment";
    public static final String ARG_ITEM = "arg_item";
    private ScannedItem item;

    public ScannedItemDetailFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();


    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_activity_scanned_item_detail);
        builder.setPositiveButton(R.string.detail_close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "onClick: close!");
                    }
                });
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
                ((TextView) v.findViewById(R.id.detail_description)).setText(String.format(getString(R.string.detail_nodescription), item.barcodeType));
            }
            ((TextView)v.findViewById(R.id.detail_quantity)).setText(String.format(getString(R.string.detail_qty), item.nScans));

            ArrayList<String[]> vals = new ArrayList<>();
            vals.add(new String[] {getString(R.string.detail_codetype), item.barcodeType});
            vals.add(new String[] {getString(R.string.detail_scantime), formatTime(item.updateTime)});

            if(item.json != null) {
                try {
                    JSONObject o = new JSONObject(item.json);
                    Iterator<String> keys = o.keys();
                    while(keys.hasNext()) {
                        String key = keys.next();
                        vals.add(new String[] {key, o.getString(key)});
                    }
                } catch(JSONException e) {
                    vals.add(new String[] {"json_error", e.getMessage()});
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
            ((TextView)v.findViewById(R.id.detail_description)).setText(formatText("no item to show"));
        }
    }

    public static ScannedItemDetailFragment newInstance(ScannedItem item) {

        Bundle args = new Bundle();
        args.putSerializable(ARG_ITEM, item);
        ScannedItemDetailFragment fragment = new ScannedItemDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private static String formatText(String text) {
        if(text == null) {
            return "None";
        } else {
            return text;
        }
    }

    private String formatTime(long time) {
        return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault()).format(new Date(time));
    }
}
