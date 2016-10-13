package net.fishandwhistle.openpos;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.fishandwhistle.openpos.barcode.CodabarSpec;
import net.fishandwhistle.openpos.barcode.Code128Spec;
import net.fishandwhistle.openpos.barcode.Code25Spec;
import net.fishandwhistle.openpos.barcode.Code39Spec;
import net.fishandwhistle.openpos.barcode.EAN8Spec;
import net.fishandwhistle.openpos.barcode.EAN13Spec;
import net.fishandwhistle.openpos.barcode.ITFSpec;
import net.fishandwhistle.openpos.barcode.UPCESpec;
import net.fishandwhistle.openpos.items.ScannedItem;
import net.fishandwhistle.openpos.items.ScannedItemAdapter;

import java.util.ArrayList;

public class MainActivity extends BarcodeReaderActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final String INSTANCE_ITEMS = "instance_items";
    private static final String INSTANCE_ITEMLIST_STATE = "itemlist_state";

    private ScannedItemAdapter items ;
    private ListView list;
    private Button showHideButton ;
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
        items = new ScannedItemAdapter(this);
        list = ((ListView)findViewById(R.id.bcreader_itemlist));
        showHideButton = (Button)findViewById(R.id.bcreader_showhide);

        // get items from saved instance, if exists, and set visibility
        if(savedInstanceState != null) {
            if (savedInstanceState.containsKey(INSTANCE_ITEMS)) {
                ArrayList<ScannedItem> oldItems = (ArrayList<ScannedItem>) savedInstanceState.getSerializable(INSTANCE_ITEMS);
                assert oldItems != null;
                for (ScannedItem s : oldItems) {
                    items.add(s);
                }
            }
            if (savedInstanceState.containsKey(INSTANCE_ITEMLIST_STATE)) {
                //noinspection WrongConstant
                int vis = savedInstanceState.getInt(INSTANCE_ITEMLIST_STATE, View.VISIBLE);
                if(vis != View.VISIBLE) {
                    list.setVisibility(View.GONE);
                    showHideButton.setText(R.string.bcreader_show);
                } else {
                    list.setVisibility(View.VISIBLE);
                    showHideButton.setText(R.string.bcreader_hide);
                }
            }
        }
        list.setAdapter(items);
        showHideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(list.getVisibility() == View.VISIBLE) {
                    list.setVisibility(View.GONE);
                    showHideButton.setText(R.string.bcreader_show);
                } else {
                    list.setVisibility(View.VISIBLE);
                    showHideButton.setText(R.string.bcreader_hide);
                }
            }
        });


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
        outState.putInt(INSTANCE_ITEMLIST_STATE, list.getVisibility());
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

}
