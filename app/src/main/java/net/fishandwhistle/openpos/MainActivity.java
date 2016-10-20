package net.fishandwhistle.openpos;

import android.app.Dialog;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
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


import net.fishandwhistle.openpos.api.APIQuery;
import net.fishandwhistle.openpos.api.ISBNQuery;
import net.fishandwhistle.openpos.api.UPCQuery;
import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.fishandwhistle.openpos.barcode.CodabarSpec;
import net.fishandwhistle.openpos.barcode.Code128Spec;
import net.fishandwhistle.openpos.barcode.Code25Spec;
import net.fishandwhistle.openpos.barcode.Code39Spec;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BarcodeReaderActivity implements NavigationView.OnNavigationItemSelectedListener,
    ScannedItemAdapter.OnItemEditCallback, APIQuery.APICallback {

    private static final String TAG = "MainActivity";
    private static final String INSTANCE_ITEMS = "instance_items";

    private ScannedItemAdapter items ;
    private ListView list;
    private TextView scannedItemsText ;


    @Override
    protected BarcodeExtractor getExtractor() {
        return new ZBarExtractor(new BarcodeSpec[] {new EAN13Spec(), new Code128Spec(),
                new ITFSpec(), new CodabarSpec(), new Code39Spec(), new EAN8Spec()});
//        return new BarcodeSpec[] {new EAN13Spec(), new EAN8Spec(), new ITFSpec(14, true)};
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
            if (keys.contains("isbn13")) {
                ISBNQuery q = new ISBNQuery(this, item.getValue("isbn13"), item, this);
                q.query();
            } else if (keys.contains("gtin13")) {
                UPCQuery q = new UPCQuery(this, item.getValue("gtin13"), item, this);
                q.query();
            }
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
        }
        this.refreshItems(true);
        return true;
    }

    @Override
    public void onQueryResult(String input, boolean success, ScannedItem item) {
        refreshItems(success);
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
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        ScannedItemDetailFragment newFragment = ScannedItemDetailFragment.newInstance(item);
        newFragment.show(ft, "dialog");
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
                            ScannedItem i = new ScannedItem("KeyIn", newText.trim());
                            items.add(i);
                            refreshItems(true);
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
                            ScannedItem i = new ScannedItem("KeyIn", newText.trim());
                            items.add(i);
                            refreshItems(true);
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
