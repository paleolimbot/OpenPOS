package net.fishandwhistle.openpos;

import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.w3c.dom.Text;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A placeholder fragment containing a simple view.
 */
public class ScannedItemDetailFragment extends DialogFragment {

    public static final String ARG_ITEM = "arg_item";
    private ScannedItem item;

    public ScannedItemDetailFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();
        Serializable s ;
        if(getArguments() != null && getArguments().containsKey(ARG_ITEM)) {
            s = getArguments().getSerializable(ARG_ITEM);
        } else if(getActivity() != null && getActivity().getIntent() != null) {
            s = getActivity().getIntent().getSerializableExtra(ARG_ITEM);
        } else {
            s = null;
        }

        if(s != null) {
            setItem((ScannedItem) s, getView());
        } else {
            setItem(null, getView());
        }

        if(getDialog() != null) {
            getDialog().setTitle(R.string.title_activity_scanned_item_detail);
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            if(getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout((6 * width) / 7, (4 * height) / 5);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scanned_item_detail, container, false);
    }

    private void setItem(ScannedItem item, View v) {
        this.item = item;
        if(item != null) {
            if(item.description != null) {
                ((TextView) v.findViewById(R.id.detail_description)).setText(item.description);
            } else {
                ((TextView) v.findViewById(R.id.detail_description)).setText(String.format(getString(R.string.detail_nodescription), item.barcodeType));
            }
            ((TextView)v.findViewById(R.id.detail_quantity)).setText(String.format(getString(R.string.detail_qty), item.nScans));
            ((TextView)v.findViewById(R.id.detail_scantime)).setText(String.format(getString(R.string.detail_scantime),
                    formatTime(item.updateTime)));
            ((TextView)v.findViewById(R.id.detail_codetype)).setText(String.format(getString(R.string.detail_codetype), item.barcodeType));
            ((TextView)v.findViewById(R.id.detail_code)).setText(item.productCode);
            TextView json = (TextView)v.findViewById(R.id.detail_json);
            TextView jsonSource = (TextView)v.findViewById(R.id.detail_jsonsource);
            if(item.json != null) {
                json.setVisibility(View.VISIBLE);
                jsonSource.setVisibility(View.VISIBLE);
                json.setText(formatText(item.json));
                jsonSource.setText(String.format(getString(R.string.details_jsonsource),
                        formatText(item.jsonSource), formatTime(item.jsonTime)));
            } else {
                json.setVisibility(View.GONE);
                jsonSource.setVisibility(View.GONE);
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
