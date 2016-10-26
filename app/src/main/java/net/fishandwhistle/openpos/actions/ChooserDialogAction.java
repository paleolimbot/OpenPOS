package net.fishandwhistle.openpos.actions;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static net.fishandwhistle.openpos.actions.Formatting.formatWithObject;

/**
 * Created by dewey on 2016-10-26.
 */

public class ChooserDialogAction extends ScannedItemAction {
    public static final String OPTION_TITLE = "title";
    public static final String OPTION_NEGATIVE_TEXT = "negative_text";
    public static final String OPTION_LABELS = "labels";
    public static final String OPTION_ACTIONS = "actions";

    private String title;
    private String negativeText;
    private List<ScannedItemAction> actions;
    private String[] labels;

    public ChooserDialogAction(JSONObject jsonOptions) {
        super(jsonOptions);
        title = getOptionString(OPTION_TITLE);
        negativeText = getOptionString(OPTION_NEGATIVE_TEXT);
        this.actions = new ArrayList<>();
        JSONArray jsonActions = getOptionArray(OPTION_ACTIONS);
        JSONArray labs = getOptionArray(OPTION_LABELS);
        if(jsonActions == null) throw new IllegalArgumentException("Option 'actions' required");
        if(labs == null) throw new IllegalArgumentException("Option 'labels' is required");
        if(labs.length() != jsonActions.length()) throw new IllegalArgumentException("Length of labels and actions must be identical");
        if(labs.length() == 0) throw new IllegalArgumentException("Length of labels/actions must be greater than 0");
        try {
            for(int i=0; i<jsonActions.length(); i++) {
                JSONObject actionJson = jsonActions.getJSONObject(i);
                this.actions.add(ActionFactory.inflate(actionJson));
            }
            labels = new String[labs.length()];
            for(int i=0; i<labs.length(); i++) {
                labels[i] = labs.getString(i);
            }
        } catch(JSONException e) {
            throw new IllegalArgumentException("Invalid JSON in constructor: " + e.getMessage());
        }
    }

    @Override
    public boolean isApplicable(Context context, ScannedItem item, ActionExecutor executor) {
        return (title == null || formatWithObject(title, item, false) != null) && (negativeText == null || formatWithObject(negativeText, item, false) != null);
    }

    @Override
    public boolean doActionContent(final Context context, final ScannedItem item, final ActionExecutor executor) throws ActionException {
        //make sure all actions are enabled and applicable
        List<String> runtimeLabs = new ArrayList<>();
        List<ScannedItemAction> runtimeActions = new ArrayList<>();
        for(int i=0; i<actions.size(); i++) {
            ScannedItemAction a = actions.get(i);
            String lab = formatWithObject(labels[i], item, false);
            if(lab != null && a.isApplicable(context, item, executor)) {
                runtimeActions.add(a);
                runtimeLabs.add(labels[i]);
            }
        }
        if(runtimeLabs.size() == 0) return false;
        final String[] choices = new String[runtimeLabs.size()];
        for(int i=0; i<choices.length; i++) {
            choices[i] = runtimeLabs.get(i);
        }

        executor.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder b = new AlertDialog.Builder(context);
                if(title != null) b.setTitle(formatWithObject(title, item));
                if(negativeText != null) {
                    b.setNegativeButton(formatWithObject(negativeText, item), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            executor.setResponse("_NEGATIVE");
                        }
                    });
                }
                b.setItems(choices, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executor.setResponse(choices[which]);
                        dialog.dismiss();
                    }
                });
                b.setCancelable(true);
                b.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        executor.setResponse("_CANCELLED");
                    }
                });
                b.show();
            }
        });

        String response = executor.getResponse();
        switch (response) {
            case "_CANCELLED":
                return false;
            case "_NEGATIVE":
                return false;
            default:
                int ind = runtimeLabs.indexOf(response);
                if(ind != -1) {
                    return runtimeActions.get(ind).doAction(context, item, executor);
                } else {
                    if(isQuiet()) {
                        return false;
                    } else {
                        throw new ActionException("Unrecognized choice in ChooserDialog");
                    }
                }
        }
    }


}
