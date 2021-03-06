package net.fishandwhistle.openpos;

import android.app.Dialog;
import android.os.AsyncTask;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import net.fishandwhistle.openpos.actions.ActionFactory;
import net.fishandwhistle.openpos.actions.ScannedItemAction;
import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.fishandwhistle.openpos.barcode.CodabarSpec;
import net.fishandwhistle.openpos.barcode.Code128Spec;
import net.fishandwhistle.openpos.barcode.Code25Spec;
import net.fishandwhistle.openpos.barcode.Code39Spec;
import net.fishandwhistle.openpos.barcode.DataBarExpandedSpec;
import net.fishandwhistle.openpos.barcode.DataBarSpec;
import net.fishandwhistle.openpos.barcode.EAN8Spec;
import net.fishandwhistle.openpos.barcode.EAN13Spec;
import net.fishandwhistle.openpos.barcode.GS1Parser;
import net.fishandwhistle.openpos.barcode.ITFSpec;
import net.fishandwhistle.openpos.barcode.MSISpec;
import net.fishandwhistle.openpos.barcode.PharmacodeSpec;
import net.fishandwhistle.openpos.barcode.UPCESpec;
import net.fishandwhistle.openpos.extractors.BarcodeExtractor;
import net.fishandwhistle.openpos.extractors.JavaExtractor;
import net.fishandwhistle.openpos.extractors.ZBarExtractor;
import net.fishandwhistle.openpos.items.ItemFormatter;
import net.fishandwhistle.openpos.items.ScannedItem;
import net.fishandwhistle.openpos.items.ScannedItemAdapter;
import net.fishandwhistle.openpos.items.ScannedItemManager;
import net.fishandwhistle.openpos.settings.SettingsProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BarcodeReaderActivity implements NavigationView.OnNavigationItemSelectedListener,
    ScannedItemAdapter.OnItemEditCallback, ScannedItemAction.ScannerItemActionCallback {

    private static final String TAG = "MainActivity";
    private static final String INSTANCE_ITEMS = "instance_items";

    private ScannedItemAdapter items ;
    private ListView list;
    private TextView scannedItemsText ;

    private SettingsProfile settings;
    private BarcodeExtractor extractor;
    private ScannedItemManager itemManager;

    private String sessionName;

    @Override
    protected BarcodeExtractor getExtractor() {
        return extractor;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        scannedItemsText = (TextView)findViewById(R.id.bcreader_scannedtitle);
        items = new ScannedItemAdapter(this, true, this);
        itemManager = new ScannedItemManager(this);
        sessionName = "default";

        list = ((ListView)findViewById(R.id.bcreader_itemlist));

        findViewById(R.id.bcreader_showall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Show all", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.bcreader_keynum).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyInNumber();
            }
        });
        findViewById(R.id.bcreader_keytext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyInText();
            }
        });

        // get items from saved instance, if exists, and set visibility
        if(savedInstanceState != null) {
            if (savedInstanceState.containsKey(INSTANCE_ITEMS)) {
                ArrayList<ScannedItem> oldItems = (ArrayList<ScannedItem>) savedInstanceState.getSerializable(INSTANCE_ITEMS);
                assert oldItems != null;
                for (ScannedItem s : oldItems) {
                    items.add(s);
                }
            }
        }
        list.setAdapter(items);
        refreshItems(true);

        //initialize the data for all barcode specs (to avoid the first scan problem)
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                new CodabarSpec().initialize();
                new Code25Spec().initialize();
                new Code39Spec().initialize();
                new Code128Spec().initialize();
                new EAN8Spec().initialize();
                new EAN13Spec().initialize();
                new ITFSpec().initialize();
                new MSISpec().initialize();
                new PharmacodeSpec().initialize();
                new UPCESpec().initialize();
                GS1Parser.initialize();
                return null;
            }
        }.execute();

        settings = new SettingsProfile();
        if(settings.scanBackend.equals("ZBar")) {
            extractor = new ZBarExtractor(settings.barcodeSpecs);
        } else {
            extractor = new JavaExtractor(settings.barcodeSpecs);
        }
        try {
            StringBuilder buf=new StringBuilder();
            InputStream json=getAssets().open("default_new.json");
            BufferedReader in=
                    new BufferedReader(new InputStreamReader(json, "UTF-8"));
            String str;

            while ((str=in.readLine()) != null) {
                buf.append(str);
            }
            in.close();
            JSONObject o = new JSONObject(buf.toString());
            settings.onNewBarcode = ActionFactory.inflate(o);

            buf=new StringBuilder();
            json=getAssets().open("default_existing.json");
            in=new BufferedReader(new InputStreamReader(json, "UTF-8"));
            while ((str=in.readLine()) != null) {
                buf.append(str);
            }
            in.close();
            o = new JSONObject(buf.toString());
            settings.onRepeatBarcode = ActionFactory.inflate(o);

            buf=new StringBuilder();
            json=getAssets().open("default_click.json");
            in=new BufferedReader(new InputStreamReader(json, "UTF-8"));
            while ((str=in.readLine()) != null) {
                buf.append(str);
            }
            in.close();
            o = new JSONObject(buf.toString());
            settings.onClick = ActionFactory.inflate(o);

        } catch (IOException e) {

        } catch(JSONException e) {

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // write dump to json
        try {
            File out = new File(Environment.getExternalStorageDirectory(), "books.json");
            FileWriter fw = new FileWriter(out);
            fw.write(itemManager.dump());
            fw.close();
        } catch(IOException e) {
            Log.e(TAG, "onPause: io", e);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<ScannedItem> scanned = new ArrayList<>();
        for(int i=0; i<items.getCount(); i++) {
            scanned.add(items.getItem(i));
        }
        if(scanned.size() > 0) {
            outState.putSerializable(INSTANCE_ITEMS, scanned);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected boolean onNewBarcode(BarcodeSpec.Barcode b) {
        ScannedItem item = new ItemFormatter().format(b);
        List<String> keys = item.getKeys();

        int index = items.indexOf(item);
        if(index == -1) {
            //item is not currently in the index
            settings.onNewBarcode.doActionAsync(this, item, this);
            items.add(item);
        } else {
            ScannedItem current = items.getItem(index);
            assert current != null;
            //copy metadata from new item to current
            for(String key: keys) {
                current.putValue(key, item.getValue(key));
            }
            //move current to end of the list and increment nScans
            current.nScans++;
            items.remove(current);
            items.add(current);
            settings.onRepeatBarcode.doActionAsync(this, current, this);
        }
        this.refreshItems(true);
        return true;
    }



    private void refreshItems(boolean scrollToEnd) {
        scannedItemsText.setText(String.format(getString(R.string.bcreader_scanneditems), items.getCount()));
        items.notifyDataSetInvalidated();
        if(scrollToEnd && items.getCount() > 1) {
            list.post(new Runnable() {
                @Override
                public void run() {
                    // Select the last row so it will scroll into view...
                    list.setSelection(items.getCount() - 1);
                }
            });
        }
    }

    @Override
    public void onScannerItemDelete(final ScannedItem item) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.main_confirmdelete_title);
        b.setMessage(String.format(getString(R.string.main_confirmdelete_message), item.toString()));
        b.setCancelable(true);
        b.setPositiveButton(R.string.main_confirmdelete_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                items.remove(item);
                refreshItems(false);
            }
        });
        b.setNegativeButton(R.string.main_dialog_cancel, null);
        b.create().show();
    }

    @Override
    public void onScannerItemQuantity(final ScannedItem item) {
        getText(getString(R.string.main_quantity_title), String.valueOf(item.nScans), "", EditorInfo.TYPE_CLASS_NUMBER,
                getString(R.string.main_quantity_enter),
                new OnTextSavedListener() {
                    @Override
                    public void onTextSaved(String oldText, String newText) {
                        try {
                            item.nScans = Integer.valueOf(newText);
                            if(item.nScans == 0) {
                                onScannerItemDelete(item);
                            }
                            refreshItems(false);
                        } catch(NumberFormatException e) {
                            Toast.makeText(MainActivity.this, R.string.main_keyin_invalid, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, getString(R.string.main_dialog_cancel), null);
    }

    @Override
    public void onScannerItemClick(ScannedItem item) {
        settings.onClick.doActionAsync(this, item, this);
    }

    @Override
    public void onScannerItemAction(String actionName, ScannedItem item) {
        Log.i(TAG, "onScannerItemAction: received action " + actionName + " for item " + item);
        refreshItems(false);
    }

    @Override
    public void onActionException(String actionName, ScannedItem item, String error) {
        Toast.makeText(this, "Error occurred executing " + actionName + " for item " + item + ": " + error,
                Toast.LENGTH_SHORT).show();
    }

    private interface OnTextSavedListener {
        void onTextSaved(String oldText, String newText);
    }

    private void keyInNumber() {
        getText(getString(R.string.main_keynum_title), "", "0123456789", EditorInfo.TYPE_CLASS_NUMBER, "Enter",
                new OnTextSavedListener() {
                    @Override
                    public void onTextSaved(String oldText, String newText) {
                        if(newText.trim().length() > 0) {
                            newText = newText.trim();
                            BarcodeSpec.Barcode b = new BarcodeSpec.Barcode("KeyIn");
                            for(int i=0; i<newText.length(); i++) {
                                b.digits.add(new BarcodeSpec.BarcodeDigit(newText.substring(i, i+1)));
                            }
                            b.isValid = true;
                            onNewBarcode(b);
                        } else {
                            Toast.makeText(MainActivity.this, R.string.main_keyin_invalid, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, getString(R.string.main_dialog_cancel), null);
    }

    private void keyInText() {
        getText(getString(R.string.main_keytext_title), "", "abcd1234", EditorInfo.TYPE_CLASS_TEXT,
                getString(R.string.main_keyin_enter),
                new OnTextSavedListener() {
                    @Override
                    public void onTextSaved(String oldText, String newText) {
                        if(newText.trim().length() > 0) {
                            newText = newText.trim();
                            BarcodeSpec.Barcode b = new BarcodeSpec.Barcode("KeyIn");
                            for(int i=0; i<newText.length(); i++) {
                                b.digits.add(new BarcodeSpec.BarcodeDigit(newText.substring(i, i+1)));
                            }
                            b.isValid = true;
                            onNewBarcode(b);
                        } else {
                            Toast.makeText(MainActivity.this, R.string.main_keyin_invalid, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, getString(R.string.main_dialog_cancel), null);
    }

    private void getText(String title, final String itemText, String itemHint, int inputType,
                         String okText, final OnTextSavedListener ok,
                         String cancelText, final DialogInterface.OnClickListener cancel) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(title);
        final EditText t = new EditText(this);
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
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(t.getWindowToken(),0);
            }
        });
        b.setNegativeButton(cancelText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(cancel != null) cancel.onClick(dialog, which);
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(t.getWindowToken(),0);
            }
        });
        final AlertDialog d = b.create();
        d.show();
        t.requestFocus();
        t.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
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

}
