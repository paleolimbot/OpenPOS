package net.fishandwhistle.openpos;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.w3c.dom.Text;

import java.io.Serializable;

/**
 * A placeholder fragment containing a simple view.
 */
public class ScannedItemDetailFragment extends Fragment {

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scanned_item_detail, container, false);
    }

    private void setItem(ScannedItem item, View v) {
        this.item = item;
        TextView text = (TextView)v.findViewById(R.id.details_text);
        if(item != null) {
            text.setText(item.toString());
        } else {
            text.setText("Null item!");
        }
    }
}
