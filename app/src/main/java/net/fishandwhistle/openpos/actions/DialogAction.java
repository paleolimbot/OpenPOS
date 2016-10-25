package net.fishandwhistle.openpos.actions;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONObject;

import static net.fishandwhistle.openpos.actions.StringFormatAction.formatWithObject;

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

    private String title;
    private String message;
    private String positiveText;
    private String neutralText;
    private String negativeText;
    private String inputType;
    private String outKey;
    private String inputHint;

    public DialogAction(JSONObject jsonOptions) {
        super(jsonOptions);
        title = getOptionString(OPTION_TITLE);
        if(title == null) throw new IllegalArgumentException("Option 'title' is required");
        message = getOptionString(OPTION_MESSAGE);
        positiveText = getOptionString(OPTION_POSITIVE_TEXT);
        neutralText = getOptionString(OPTION_NEUTRAL_TEXT);
        negativeText = getOptionString(OPTION_NEGATIVE_TEXT);
        inputType = getOptionString(OPTION_INPUT_TYPE);
        if(inputType != null) {
            if(positiveText == null) {
                positiveText = "Enter";
            }
            if(negativeText == null) {
                negativeText = "Cancel";
            }
        } else {
            if(positiveText == null) {
                positiveText = "Close";
            }
        }
        inputHint = getOptionString(OPTION_INPUT_HINT);
        if(inputHint == null) {
            inputHint = "";
        }
        outKey = getOptionString(OPTION_OUT_KEY);
        if(inputType != null && outKey == null) throw new IllegalArgumentException("Cannot collect input without option 'outkey'");
    }

    @Override
    public boolean doAction(final Context context, final ScannedItem item, final ActionExecutor executor) throws ActionException {

        executor.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(inputType == null) {
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
                    b.setCancelable(true);
                    b.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            executor.setResponse("_CANCELLED");
                        }
                    });
                    b.show();
                } else {
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
                }
            }
        });
        String response = executor.getResponse(); //should block until response gets issued
        //will be one of _POSITIVE, _NEGATIVE, _NEUTRAL, or _CANCELLED
        //if inputType is set, the response will be whatever is in the input field
        if (outKey != null && !TextUtils.isEmpty(response)) {
            if(response.equals("_CANCELLED") && inputType != null) {
                return false;
            } else {
                item.putValue(outKey, response.trim());
                return true;
            }
        } else {
            return !TextUtils.isEmpty(response) && (response.equals("_POSITIVE") || response.equals("_NEUTRAL"));
        }
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
