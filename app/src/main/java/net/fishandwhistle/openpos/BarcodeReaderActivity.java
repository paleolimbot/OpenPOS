package net.fishandwhistle.openpos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.fishandwhistle.openpos.api.ISBNQuery;
import net.fishandwhistle.openpos.api.UPCQuery;
import net.fishandwhistle.openpos.barcode.BarcodeExtractor;
import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.fishandwhistle.openpos.barcode.EANSpec;
import net.fishandwhistle.openpos.api.APIQuery;
import net.fishandwhistle.openpos.barcode.UPCASpec;
import net.fishandwhistle.openpos.barcode.UPCESpec;
import net.fishandwhistle.openpos.items.ScannedItem;
import net.fishandwhistle.openpos.items.ScannedItemAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BarcodeReaderActivity extends AppCompatActivity implements CameraPreview.PreviewImageCallback, APIQuery.APICallback {

    private static final String TAG = "BarcodeReader";

    private Camera mCamera;
    private CameraPreview mPreview;
    private BarcodeSpec.Barcode lastBarcode ;
    private ImageBarcodeExtractor extractor ;
    private ScannedItemAdapter items ;
    private ListView list;
    private TextView scannedItemsText ;
    private Button showHideButton ;

    private boolean enableScanning ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_bcreader);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPreview = new CameraPreview(this);
        mPreview.setPreviewImageCallback(this);
        FrameLayout mPreviewF = (FrameLayout) findViewById(R.id.bcreader_imageframe);
        mPreviewF.addView(mPreview, 0);

        mPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEnableScanning(!enableScanning);
            }
        });

        lastBarcode = null;
        extractor = null;

        items = new ScannedItemAdapter(this);
        list = ((ListView)findViewById(R.id.bcreader_itemlist));
        list.setAdapter(items);
        scannedItemsText = ((TextView)findViewById(R.id.bcreader_scannedtitle));
        showHideButton = (Button)findViewById(R.id.bcreader_showhide);
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

        refreshItems();
        enableScanning = true;
    }

    private void setEnableScanning(boolean value) {
        TextView text = (TextView)findViewById(R.id.bcreader_disablescantext);
        if(value) {
            text.setText(R.string.bcreader_disablescan);
            text.setTextColor(Color.argb(100, 255, 0, 0));
            findViewById(R.id.bcreader_redbar).setVisibility(View.VISIBLE);
        } else {
            text.setText(R.string.bcreader_enablescan);
            text.setTextColor(Color.argb(100, 0, 255, 0));
            findViewById(R.id.bcreader_redbar).setVisibility(View.GONE);
        }
        enableScanning = value;
    }

    @Override
    public void onPause() {
        super.onPause();
        mPreview.releaseCamera();
        mCamera.release();
        mCamera = null;
        if(extractor != null) {
            extractor.cancel(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resetCamera(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void refreshItems() {
        scannedItemsText.setText(String.format(getString(R.string.bcreader_scanneditems), items.getCount()));
        items.notifyDataSetInvalidated();
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
        focusareas.add(new Camera.Area(new Rect(-850, -200, -750, 200), 100));
        if (params.getMaxNumFocusAreas() > 0){ // set to 9/10 up the screen
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
        super.onBackPressed();
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

    @Override
    public void onPreviewImage(byte[] data, int format, int width, int height) {
        if(enableScanning && ((extractor == null) || extractor.isCancelled())) {
            Log.i(TAG, "Launching barcode extractor");
            extractor = new ImageBarcodeExtractor(data, format, width, height);
            extractor.execute("");
        }
    }

    private void onBarcodeRead(BarcodeSpec.Barcode b) {
        //release extractor
        extractor = null;

        if(b.isValid) {
            Log.i(TAG, b.type + " Read: " + b.toString());
            if(lastBarcode == null) {
                onNewBarcode(b);
            } else if(!b.equals(lastBarcode)) {
                onNewBarcode(b);
            } else if(b.equals(lastBarcode) && ((b.timeread - lastBarcode.timeread) > 1000)) {
                onNewBarcode(b);
            }
            lastBarcode = b;
        } else {
            Log.i(TAG, "Barcode error. " + b.type + ": " + b.toString());
        }
    }

    protected void onNewBarcode(BarcodeSpec.Barcode b) {
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(150);

        ScannedItem item = new ScannedItem(b.toString());
        item.scanTime = b.timeread;
        items.add(item);
        this.refreshItems();

        APIQuery q ;
        if(b.type.equals("EAN") && (b.digits.get(0).digit.equals("9"))) {
            q = new ISBNQuery(this, b.toString(), item, this);
        } else {
            q = new UPCQuery(this, b.toString(), item, this);
        }
        q.query();
    }

    @Override
    public void onQueryResult(String input, JSONObject object) {
        if(object != null) {
            this.refreshItems();
            Log.i(TAG, "Got result for input " + input);
        } else {
            Log.e(TAG, "No result for input " + input);
        }
    }


    private class ImageBarcodeExtractor extends AsyncTask<String, String, BarcodeSpec.Barcode> {
        private byte[] data;
        private int format;
        private int width;
        private int height;

        public ImageBarcodeExtractor(byte[] data, int format, int width, int height) {
            this.data = data;
            this.format = format;
            this.width = width;
            this.height = height;
        }

        @Override
        protected BarcodeSpec.Barcode doInBackground(String... params) {
            long start = System.currentTimeMillis();
            BarcodeSpec.Barcode barcode = null;
            try {
                YuvImage y = new YuvImage(data, format, width, height, null);
                File f = new File(BarcodeReaderActivity.this.getCacheDir(), "temppic.jpg");
                FileOutputStream fos = new FileOutputStream(f);
                y.compressToJpeg(new Rect(width / 10, 0, width / 10 + 25, height-1), 95, fos);
                fos.close();
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                double[] vals = new double[b.getHeight()];
                for(int i=0; i<b.getHeight(); i++) {
                    int col = b.getPixel(0, i);
                    vals[b.getHeight()-1-i] = (Color.red(col) + Color.blue(col) + Color.green(col)) / 256.0 / 3.0;
                }
                b.recycle();
                data = null;

                long decoded = System.currentTimeMillis();
                Log.i(TAG, "Image read time: " + (decoded - start) + "ms");
                start = System.currentTimeMillis();

                //do java decoding
                BarcodeExtractor e = new BarcodeExtractor(vals);
                barcode = e.multiExtract(new BarcodeSpec[] {new EANSpec(), new UPCASpec(), new UPCESpec()});

            } catch(IOException e) {
                Log.e(TAG, "IO exception on write image", e);
            }
            Log.i(TAG, "Barcode read time: " + (System.currentTimeMillis() - start) + "ms");
            return barcode;
        }

        @Override
        protected void onPostExecute(BarcodeSpec.Barcode code) {
            BarcodeReaderActivity.this.onBarcodeRead(code);
        }
    }

}
