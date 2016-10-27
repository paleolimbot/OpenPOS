package net.fishandwhistle.openpos.actions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;

import net.fishandwhistle.openpos.R;
import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by dewey on 2016-10-27.
 */

public class DetailsDialogAction extends ScannedItemAction {
    public DetailsDialogAction(JSONObject jsonOptions) {
        super(jsonOptions);
    }

    @Override
    public boolean doActionContent(final Context context, final ScannedItem item, final ActionExecutor executor) throws ActionException {
        executor.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.title_activity_scanned_item_detail);
                builder.setIcon(R.mipmap.ic_launcher);
                builder.setPositiveButton(R.string.detail_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executor.setResponse("_POSITIVE");
                    }
                });
                builder.setCancelable(true);
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        executor.setResponse("_CANCELLED");
                    }
                });
                View v = ((Activity)context).getLayoutInflater().inflate(R.layout.fragment_scanned_item_detail, null);
                setItem(context, item, v);
                builder.setView(v);
                builder.show();
            }
        });
        String response = executor.getResponse();
        return response == null || !response.equals("_CANCELLED");
    }

    private void setItem(Context c, ScannedItem item, View v) {
        TableLayout kv = (TableLayout)v.findViewById(R.id.detail_keyvalues);
        kv.removeAllViews();

        if(item != null) {
            if(item.description != null) {
                ((TextView) v.findViewById(R.id.detail_description)).setText(item.description);
            } else {
                ((TextView) v.findViewById(R.id.detail_description)).setText(item.toString());
            }
            ((TextView)v.findViewById(R.id.detail_code)).setText(item.barcodeText);
            ((TextView)v.findViewById(R.id.detail_quantity)).setText(String.format(c.getString(R.string.detail_qty), item.nScans));

            ArrayList<String[]> vals = new ArrayList<>();
            vals.add(new String[] {c.getString(R.string.detail_codetype), item.barcodeType});
            vals.add(new String[] {c.getString(R.string.detail_scantime), formatTime(item.updateTime)});

            List<String> jsonKeys = item.getKeys();
            for(String key: jsonKeys) {
                if(key.startsWith("time_")) {
                    vals.add(new String[] {key, formatTime(item.getValue(key))});
                } else {
                    vals.add(new String[]{key, item.getValue(key)});
                }
            }

            LayoutInflater inflater = ((Activity)c).getLayoutInflater();
            for(String[] keyval: vals) {
                View row = inflater.inflate(R.layout.scanned_item_details_kv, null);
                ((TextView)row.findViewById(R.id.key)).setText(keyval[0]);
                ((TextView)row.findViewById(R.id.value)).setText(keyval[1]);
                kv.addView(row);
            }

        } else {
            ((TextView)v.findViewById(R.id.detail_description)).setText(c.getString(R.string.detail_noitem));
        }
    }

    private String formatTime(String time) {
        try {
            return formatTime(Long.valueOf(time));
        } catch(NumberFormatException e) {
            return time;
        }
    }

    private String formatTime(long time) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(time));

    }
}
