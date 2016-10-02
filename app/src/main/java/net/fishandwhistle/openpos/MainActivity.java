package net.fishandwhistle.openpos;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import net.fishandwhistle.openpos.barcode.BarcodeExtractor;
import net.fishandwhistle.openpos.barcode.BarcodeSpec;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, Camera.PictureCallback, CameraPreview.PreviewImageCallback {

    private static final String TAG = "MainActivity";
    private static final String CHARS = "0123456789ABCDEF";

    private Camera mCamera;
    private CameraPreview mPreview;
    private FileWriter bos ;
    private List<String> barcodes ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mPreview = new CameraPreview(this);
        mPreview.setPreviewImageCallback(this);
        FrameLayout mPreviewF = (FrameLayout) findViewById(R.id.main_imageframe);
        mPreviewF.addView(mPreview, 0);

        File fout = new File(Environment.getExternalStorageDirectory(), "output.txt");
        try {
            bos = new FileWriter(fout);
        } catch(IOException e) {
            this.finish();
        }

        barcodes = new ArrayList<>();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.takePicture(null, null, MainActivity.this);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mPreview.releaseCamera();
        mCamera.release();
        mCamera = null;
        try {
            bos.flush();
            if(isFinishing()) {
                bos.close();
            }
        } catch(IOException e) {
            Log.e(TAG, "ERROR flushing stream");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resetCamera(false);
    }

    private void resetCamera(boolean reset) {
        if(reset) {
            mPreview.releaseCamera();
            mCamera.release();
        }
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);
        // get Camera parameters
        Camera.Parameters params = mCamera.getParameters();
        // set the focus mode
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        List<Camera.Area> focusareas = new ArrayList<>();
        focusareas.add(new Camera.Area(new Rect(-550, -200, -450, 200), 100));
        if (params.getMaxNumFocusAreas() > 0){ // set to 3/4 up the screen
            params.setFocusAreas(focusareas);
        }
        if(params.getMaxNumMeteringAreas() > 0) {
            params.setMeteringAreas(focusareas);
        }
        // set Camera parameters
        List<Camera.Size> sizes = params.getSupportedPictureSizes() ;
        Camera.Size best = sizes.get(0);
        for(int i=1; i<sizes.size(); i++) {
            Camera.Size s = sizes.get(i);
            if(s.height > best.height) {
                best = s;
            } else if(s.height == best.height && s.width < best.width) {
                best = s;
            }
        }
        params.setPictureSize(best.width, best.height);
        sizes = params.getSupportedPreviewSizes() ;
        best = sizes.get(0);
        for(int i=1; i<sizes.size(); i++) {
            Camera.Size s = sizes.get(i);
            if(s.height > best.height) {
                best = s;
            } else if(s.height == best.height && s.width < best.width) {
                best = s;
            }
        }
        params.setPreviewSize(best.width, best.height);
        mCamera.setParameters(params);
        mPreview.setCamera(mCamera);
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

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.e(TAG, "Exception on open of camera", e);
        }
        return c; // returns null if camera is unavailable
    }

    private void onBarcodeRead(BarcodeSpec.Barcode b) {
        String bc = b.toString();
        if(!barcodes.contains(bc)) {
            barcodes.add(bc);
            Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(200);
            Toast.makeText(this, "ISBN Read: " + b.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void onPreviewImage(byte[] data, int format, int width, int height) {
        try {
            long start = System.currentTimeMillis();

            YuvImage y = new YuvImage(data, format, width, height, null);
            File f = new File(Environment.getExternalStorageDirectory(), "temppic.jpg");
            FileOutputStream fos = new FileOutputStream(f);
            y.compressToJpeg(new Rect(width / 4, 0, width / 4 + 25, height-1), 95, fos);
            fos.close();
            Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
            double[] vals = new double[b.getHeight()];
            for(int i=0; i<b.getHeight(); i++) {
                int col = b.getPixel(0, i);
                vals[b.getHeight()-1-i] = (Color.red(col) + Color.blue(col) + Color.green(col)) / 256.0 / 3.0;
                bos.write(String.valueOf(vals[b.getHeight()-1-i])) ;
                if(i < 0) {
                    bos.write(",");
                }
            }
            bos.flush();
            b.recycle();

            long decoded = System.currentTimeMillis();
            Log.i(TAG, "Image read time: " + (decoded - start) + "ms");
            start = System.currentTimeMillis();

            //try java decoding
            BarcodeExtractor e = new BarcodeExtractor(vals);
            BarcodeSpec.Barcode barcode = e.multiExtract(BarcodeSpec.ISBN);
            if(barcode.isValid) {
                Log.i(TAG, "ISBN Read: " + barcode.toString());
                this.onBarcodeRead(barcode);
            } else {
                Log.i(TAG, "Error getting barcode: " + barcode.tag + ". Partial: " + barcode.toString());
            }
            Log.i(TAG, "Barcode read time: " + (System.currentTimeMillis() - start) + "ms");
        } catch(IOException e) {
            Log.e(TAG, "IO exception on write image", e);
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        Camera.Size size = params.getPictureSize();
        int width = size.width;
        int height = size.height;
        try {
            long start = System.currentTimeMillis();
            BitmapRegionDecoder d = BitmapRegionDecoder.newInstance(data, 0, data.length, false);
            BitmapFactory.Options o = new BitmapFactory.Options();

            Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
            double[] vals = new double[b.getHeight()];
            for(int i=0; i<b.getHeight(); i++) {
                int col = b.getPixel(width / 4, i);
                vals[b.getHeight()-1-i] = (Color.red(col) + Color.blue(col) + Color.green(col)) / 256.0 / 3.0;
                bos.write(String.valueOf(vals[b.getHeight()-1-i])) ;
                if(i < 0) {
                    bos.write(",");
                }
            }
            bos.write("\n");
            bos.flush();
            b.recycle();
            long decoded = System.currentTimeMillis();
            Log.i(TAG, "Image read time: " + (decoded - start) + "ms");
            start = System.currentTimeMillis();

            //try java decoding
            BarcodeExtractor e = new BarcodeExtractor(vals);
            BarcodeSpec.Barcode barcode = e.multiExtract(BarcodeSpec.ISBN);
            if(barcode.isValid) {
                Toast.makeText(this, "ISBN Read: " + barcode.toString(),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Error getting barcode: " + barcode.tag + ". Partial: " + barcode.toString(),
                        Toast.LENGTH_LONG).show();
            }
            Log.i(TAG, "Barcode read time: " + (System.currentTimeMillis() - start) + "ms");

        } catch(IOException e) {
            Log.e(TAG, "IOException on image decode", e);
        }
        mCamera.startPreview();
    }

}
