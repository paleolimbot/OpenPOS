package net.fishandwhistle.openpos;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.fishandwhistle.openpos.barcode.CodabarSpec;
import net.fishandwhistle.openpos.barcode.Code128Spec;
import net.fishandwhistle.openpos.barcode.Code25Spec;
import net.fishandwhistle.openpos.barcode.Code39Spec;
import net.fishandwhistle.openpos.barcode.EAN8Spec;
import net.fishandwhistle.openpos.barcode.EAN13Spec;
import net.fishandwhistle.openpos.barcode.ITFSpec;
import net.fishandwhistle.openpos.items.ScannedItem;
import net.fishandwhistle.openpos.items.ScannedItemAdapter;

import java.util.ArrayList;

public class MainActivity extends BarcodeReaderActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final String INSTANCE_ITEMS = "instance_items";

    private ScannedItemAdapter items ;
    private ListView list;
    private TextView scannedItemsText ;



    @Override
    protected BarcodeSpec[] getBarcodeSpecs() {
        //TODO get this based on preferences
        return new BarcodeSpec[] {new EAN13Spec(), new Code128Spec(),
                new ITFSpec(), new CodabarSpec(), new Code25Spec(), new Code39Spec(), new EAN8Spec()};
//        return new BarcodeSpec[] {new EAN13Spec(), new EAN8Spec(), new ITFSpec(14, true), new UPCESpec()};
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
        items = new ScannedItemAdapter(this, true, new ScannedItemAdapter.OnItemEditCallback() {
            @Override
            public void onScanerItemDelete(ScannedItem item) {
                confirmItemDelete(item);
            }

            @Override
            public void onScannerItemQuantity(ScannedItem item) {
                editItemQuantity(item);
            }
        });

        list = ((ListView)findViewById(R.id.bcreader_itemlist));

        findViewById(R.id.bcreader_showall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Show all", Toast.LENGTH_SHORT).show();
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
        ScannedItem item = new ScannedItem(b.type, b.toString());
        item.scanTime = b.timeread;
        items.add(item);
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

    private void confirmItemDelete(final ScannedItem item) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Confirm Delete");
        b.setMessage("Delete item " + item.toString() + "?");
        b.setCancelable(true);
        b.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                items.remove(item);
                refreshItems(false);
            }
        });
        b.setNegativeButton("Cancel", null);
        b.create().show();
    }

    private void editItemQuantity(final ScannedItem item) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Edit Quantity");
        final EditText t = new EditText(this);
        t.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
        t.setText(String.valueOf(item.nScans));
        t.setSelectAllOnFocus(true);

        b.setView(t);
        b.setCancelable(true);
        b.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    item.nScans = Integer.valueOf(t.getText().toString());
                    refreshItems(false);
                } catch(NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid number", Toast.LENGTH_SHORT).show();
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(t.getWindowToken(),0);
            }
        });
        b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(t.getWindowToken(),0);
            }
        });
        b.create().show();
        t.requestFocus();
        t.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
            }
        }, 100);

    }

}
