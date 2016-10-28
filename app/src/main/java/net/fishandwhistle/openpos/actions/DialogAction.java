package net.fishandwhistle.openpos.actions;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static net.fishandwhistle.openpos.actions.Formatting.formatWithObject;

/**
 * Created by dewey on 2016-10-24.
 */

public class DialogAction extends ScannedItemAction {

    public static final String OPTION_TITLE = "title";
    public static final String OPTION_MESSAGE = "message";
    public static final String OPTION_POSITIVE_TEXT = "positive_text";
    public static final String OPTION_NEUTRAL_TEXT = "neutral_text";
    public static final String OPTION_NEGATIVE_TEXT = "negative_text";
    public static final String OPTION_INPUT_TYPE = "input_type";
    public static final String OPTION_INPUT_HINT = "input_hint";
    public static final String OPTION_OUT_KEY = "out_key";
    public static final String OPTION_LABELS = "labels";
    public static final String OPTION_VALUES = "values";

    private String title;
    private String message;
    private String positiveText;
    private String neutralText;
    private String negativeText;
    private String inputType;
    private String outKey;
    private String inputHint;
    private String[] labels;
    private String[] values;

    public DialogAction(JSONObject jsonOptions) {
        super(jsonOptions);
        title = getOptionString(OPTION_TITLE);
        message = getOptionString(OPTION_MESSAGE, null);
        positiveText = getOptionString(OPTION_POSITIVE_TEXT, null);
        neutralText = getOptionString(OPTION_NEUTRAL_TEXT, null);
        negativeText = getOptionString(OPTION_NEGATIVE_TEXT, null);
        inputType = getOptionString(OPTION_INPUT_TYPE, null);
        inputHint = getOptionString(OPTION_INPUT_HINT, "");

        JSONArray labs = getOptionArray(OPTION_LABELS, null);
        JSONArray vals = getOptionArray(OPTION_VALUES, null);
        if(labs != null || vals != null) {
            if(labs == null) {
                labs = vals;
            } else if (vals == null) {
                vals = labs;
            }
            if (labs.length() != vals.length())
                throw new IllegalArgumentException("Labels and values must have identical length");
            if (labs.length() == 0)
                throw new IllegalArgumentException("Labels must have length > 0");
            try {
                labels = new String[labs.length()];
                values = new String[labs.length()];
                for (int i = 0; i < labs.length(); i++) {
                    labels[i] = labs.getString(i);
                    values[i] = vals.getString(i);
                }
            } catch (JSONException e) {
                throw new IllegalArgumentException("Invalid JSON in constructor: " + e.getMessage());
            }
        } else {
            labels = null;
            values = null;
        }

        if(inputType != null && values != null) throw new IllegalArgumentException("Labels and input type cannot both be specified");

        if(inputType != null) {
            if(positiveText == null) {
                positiveText = "Enter";
            }
            if(negativeText == null) {
                negativeText = "Cancel";
            }
            if(message != null) throw new IllegalArgumentException("Cannot specify message when using inputType");
        } else if(values != null) {
            if(message != null) throw new IllegalArgumentException("Cannot specify message when using values");
        } else {
            if(positiveText == null) {
                positiveText = "Close";
            }
        }

        outKey = getOptionString(OPTION_OUT_KEY, null);
        if((inputType != null || labels != null) && outKey == null) throw new IllegalArgumentException("Cannot collect input without option 'outkey'");
    }

    @Override
    public boolean doActionContent(final Context context, final ScannedItem item, final ActionExecutor executor) throws ActionException {

        executor.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(inputType != null) {
                    getText(context, formatWithObject(title, item), "", inputHint, getInputType(inputType),
                            formatWithObject(positiveText, item), new OnTextSavedListener() {
                                @Override
                                public void onTextSaved(String oldText, String newText) {
                                    executor.setResponse(newText);
                                }
                            }, formatWithObject(negativeText, item), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    executor.setResponse("_CANCELLED");
                                }
                            });
                } else {
                    AlertDialog.Builder b = new AlertDialog.Builder(context);
                    if(title != null) b.setTitle(formatWithObject(title, item));
                    if(message != null) b.setMessage(formatWithObject(message, item));
                    if(positiveText != null) b.setPositiveButton(formatWithObject(positiveText, item), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            executor.setResponse("_POSITIVE");
                        }
                    });
                    if(neutralText != null) b.setNeutralButton(formatWithObject(neutralText, item), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            executor.setResponse("_NEUTRAL");
                        }
                    });
                    if(negativeText != null) b.setNegativeButton(formatWithObject(negativeText, item), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            executor.setResponse("_NEGATIVE");
                        }
                    });
                    if(labels != null) {
                        final List<String> valuesRuntime = new ArrayList<>();
                        List<String> labelsRuntime = new ArrayList<>();
                        for(int i=0; i<values.length; i++) {
                            String val = formatWithObject(values[i], item, false);
                            String lab = formatWithObject(labels[i], item, false);
                            if(val != null && lab != null) {
                                valuesRuntime.add(val);
                                labelsRuntime.add(lab);
                            }
                        }
                        if(valuesRuntime.size() == 0) return;

                        String[] items = new String[labelsRuntime.size()];
                        for(int i=0; i<items.length; i++) {
                            items[i] = labelsRuntime.get(i);
                        }
                        b.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                executor.setResponse(valuesRuntime.get(which));
                                dialog.dismiss();
                            }
                        });
                    }
                    b.setCancelable(true);
                    b.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            executor.setResponse("_CANCELLED");
                        }
                    });
                    b.show();
                }
            }
        });
        String response = executor.getResponse(); //should block until response gets issued
        //will be one of _POSITIVE, _NEGATIVE, _NEUTRAL, or _CANCELLED
        //if inputType is set, the response will be whatever is in the input field
        if (outKey != null && !TextUtils.isEmpty(response)) {
            if(response.equals("_CANCELLED") && (inputType != null || values != null)) {
                return false;
            } else {
                item.putValue(outKey, response.trim());
                return true;
            }
        }

        return !TextUtils.isEmpty(response) && (response.equals("_POSITIVE") || response.equals("_NEUTRAL"));
    }

    private interface OnTextSavedListener {
        void onTextSaved(String oldText, String newText);
    }

    private static void getText(final Context context, String title, final String itemText, String itemHint, int inputType,
                         String okText, final OnTextSavedListener ok,
                         String cancelText, final DialogInterface.OnClickListener cancel) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(title);
        final EditText t = new EditText(context);
        t.setInputType(inputType);
        t.setText(itemText);
        t.setHint(itemHint);
        t.setSelectAllOnFocus(true);

        b.setView(t);
        b.setCancelable(true);
        b.setPositiveButton(okText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(ok != null) ok.onTextSaved(itemText, t.getText().toString());
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(t.getWindowToken(),0);
            }
        });
        b.setNegativeButton(cancelText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(cancel != null) cancel.onClick(dialog, which);
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(t.getWindowToken(),0);
            }
        });
        b.setCancelable(true);
        b.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(cancel != null) cancel.onClick(dialog, Dialog.BUTTON_NEGATIVE);
            }
        });
        final AlertDialog d = b.create();
        d.show();
        t.requestFocus();
        t.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
            }
        }, 50);
        t.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    d.getButton(Dialog.BUTTON_POSITIVE).performClick();
                    return true;
                }
                else {
                    return false;
                }
            }
        });
    }

    private static int getInputType(String inputType) {
        switch (inputType) {
            case "number":
                return EditorInfo.TYPE_CLASS_NUMBER;
            default:
                return EditorInfo.TYPE_CLASS_TEXT;
        }
    }
}
