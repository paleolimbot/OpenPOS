package net.fishandwhistle.openpos;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.fishandwhistle.openpos.api.ISBNQuery;
import net.fishandwhistle.openpos.api.UPCQuery;
import net.fishandwhistle.openpos.barcode.BarcodeExtractor;
import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.fishandwhistle.openpos.barcode.CodabarSpec;
import net.fishandwhistle.openpos.barcode.EAN8Spec;
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
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class BarcodeReaderActivity extends AppCompatActivity implements CameraPreview.PreviewImageCallback, APIQuery.APICallback,
 Camera.PictureCallback {

    private static final String TAG = "BarcodeReader";
    private static final String INSTANCE_ITEMS = "instance_items";
    private static final String INSTANCE_ITEMLIST_STATE = "itemlist_state";

    private Camera mCamera;
    private CameraPreview mPreview;
    private BarcodeSpec.Barcode lastBarcode ;
    private ImageBarcodeExtractor extractor ;
    private ScannedItemAdapter items ;
    private ListView list;
    private TextView scannedItemsText ;
    private Button showHideButton ;
    private int cameraDisplayOrientation ;

    private boolean enableScanning ;
    private ProgressDialog highResDecodeProgress;

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
                mCamera.autoFocus(null);
            }
        });

        mPreview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mCamera.takePicture(null, null, BarcodeReaderActivity.this);
                return true;
            }
        });

        lastBarcode = null;
        extractor = null;

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
        scannedItemsText = ((TextView)findViewById(R.id.bcreader_scannedtitle));
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
        enableScanning = true;
        cameraDisplayOrientation = -1;

        highResDecodeProgress = new ProgressDialog(this);
        highResDecodeProgress.setCancelable(false);
        highResDecodeProgress.setIndeterminate(true);
        highResDecodeProgress.setMessage("Decoding image...");

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    private void resetCamera(boolean reset) {
        if(reset) {
            mPreview.releaseCamera();
            mCamera.release();
        }
        mCamera = getCameraInstance();
        int orientation = this.getScreenOrientation();
        Log.i(TAG, "resetCamera: Orientation: " + orientation);
        if(orientation == 0) {
            cameraDisplayOrientation = 90;
        } else if(orientation == 1) {
            cameraDisplayOrientation = 0;
        } else if(orientation == 2) {
            cameraDisplayOrientation = 270;
        } else if(orientation == 3) {
            cameraDisplayOrientation = 180;
        } else {
            throw new RuntimeException("Unsupported orientation selected");
        }
        mCamera.setDisplayOrientation(cameraDisplayOrientation);

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
            if(orientation == 0 || orientation == 2) {
                //pick 'tallest' option
                if (s.height > best.height) {
                    best = s;
                } else if (s.height == best.height && s.width < best.width) {
                    best = s;
                }
            } else {
                //pick 'widest' option
                if (s.width > best.width) {
                    best = s;
                } else if (s.width == best.width && s.height < best.height) {
                    best = s;
                }
            }
        }
        params.setPictureSize(best.width, best.height);
        Log.i(TAG, "resetCamera: Setting picture size to " + best.width + "x" + best.height);
        sizes = params.getSupportedPreviewSizes() ;
        best = sizes.get(0);
        for(int i=1; i<sizes.size(); i++) {
            Camera.Size s = sizes.get(i);
            if(orientation == 0 || orientation == 2) {
                //pick 'tallest' option
                if (s.height > best.height) {
                    best = s;
                } else if (s.height == best.height && s.width < best.width) {
                    best = s;
                }
            } else {
                //pick 'widest' option
                if (s.width > best.width) {
                    best = s;
                } else if (s.width == best.width && s.height < best.height) {
                    best = s;
                }
            }
        }
        params.setPreviewSize(best.width, best.height);
        Log.i(TAG, "resetCamera: Setting preview size to " + best.width + "x" + best.height);
        mCamera.setParameters(params);
        mPreview.setCamera(mCamera);
    }

    public int getScreenOrientation() {
        Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        return display.getRotation();
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
            extractor = new ImageBarcodeExtractor(data, format, width, height, getScreenOrientation());
            extractor.execute("");
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if(extractor != null) {
            extractor.cancel(false);
        }
        Camera.Parameters params = camera.getParameters();
        int format = params.getPictureFormat();
        Camera.Size size = params.getPictureSize();
        highResDecodeProgress.show();
        extractor = new ImageBarcodeExtractor(data, format, size.width, size.height, getScreenOrientation());
        extractor.execute("");
    }

    private void onBarcodeRead(BarcodeSpec.Barcode b) {
        //release extractor and close progressDialog, if showing
        int format = extractor.format;
        extractor = null;
        boolean notifyFail = format == mCamera.getParameters().getPictureFormat();
        if(highResDecodeProgress.isShowing()) {
            highResDecodeProgress.hide();
        }

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
            String partial = b.toString();
            if(partial.length() > 0) {
                partial = " Partial result: " + b.type + "/" + partial;
            }
            Log.i(TAG, "Barcode error." + partial);
            if(notifyFail) {
                Toast.makeText(this, "Failed to read barcode." + partial, Toast.LENGTH_LONG).show();
            }
        }
        if(notifyFail) {
            mCamera.startPreview();
        }
    }

    protected void onNewBarcode(BarcodeSpec.Barcode b) {
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(150);

        ScannedItem item = new ScannedItem(b.toString());
        item.scanTime = b.timeread;
        items.add(item);
        this.refreshItems(true);

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
            this.refreshItems(false);
            Log.i(TAG, "Got result for input " + input);
        } else {
            Log.e(TAG, "No result for input " + input);
        }
    }

    private Rect getRegion(int orientation, int width, int height) {
        if(orientation == 0) {
            return new Rect(width / 10, 0, width / 10 + 25, height-1);
        } else if (orientation == 1){
            return new Rect(0, height / 10, width-1, height / 10 + 25);
        } else if (orientation == 3) {
            return new Rect(0, 9*height / 10, width-1, 9*height / 10 + 25);
        } else if (orientation == 2) {
            return new Rect(9*width / 10, 0, 9*width / 10 + 25, height-1);
        } else {
            throw new RuntimeException("Unsupported rotation detected: " + orientation);
        }
    }

    private static double[] extractLineFromBitmap(Bitmap b, int orientation) {
        if(orientation == 0) {
            double[] vals = new double[b.getHeight()];
            for (int i = 0; i < b.getHeight(); i++) {
                int col = b.getPixel(0, i);
                vals[b.getHeight() - 1 - i] = (Color.red(col) + Color.blue(col) + Color.green(col)) / 256.0 / 3.0;
            }
            return vals;
        } else if (orientation == 1){
            double[] vals = new double[b.getWidth()];
            for (int i = 0; i < b.getWidth(); i++) {
                int col = b.getPixel(i, 0);
                vals[i] = (Color.red(col) + Color.blue(col) + Color.green(col)) / 256.0 / 3.0;
            }
            return vals;
        } else if (orientation == 3) {
            double[] vals = new double[b.getWidth()];
            for (int i = 0; i < b.getWidth(); i++) {
                int col = b.getPixel(i, 0);
                vals[b.getWidth()-1-i] = (Color.red(col) + Color.blue(col) + Color.green(col)) / 256.0 / 3.0;
            }
            return vals;
        } else if (orientation == 2) {
            double[] vals = new double[b.getHeight()];
            for (int i = 0; i < b.getHeight(); i++) {
                int col = b.getPixel(0, i);
                vals[i] = (Color.red(col) + Color.blue(col) + Color.green(col)) / 256.0 / 3.0;
            }
            return vals;
        } else {
            throw new RuntimeException("Unsupported rotation detected: " + orientation);
        }
    }

    private class ImageBarcodeExtractor extends AsyncTask<String, String, BarcodeSpec.Barcode> {
        private byte[] data;
        private int format;
        private int width;
        private int height;
        private int orientation;

        public ImageBarcodeExtractor(byte[] data, int format, int width, int height, int orientation) {
            this.data = data;
            this.format = format;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
        }

        @Override
        protected BarcodeSpec.Barcode doInBackground(String... params) {
            long start = System.currentTimeMillis();
            BarcodeSpec.Barcode barcode = null;
            File f = new File(BarcodeReaderActivity.this.getCacheDir(), "temppic.jpg");
            Rect decodeRegion = getRegion(orientation, width, height);
            Bitmap b;
            try {
                if((format != ImageFormat.NV21 && format != ImageFormat.YUY2)) {
                    Bitmap bigBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    b = Bitmap.createBitmap(bigBitmap, decodeRegion.left, decodeRegion.top,
                            decodeRegion.width(), decodeRegion.height());
                    //test to see what image we are analyzing
                    f = new File(Environment.getExternalStorageDirectory(), "temppic.jpg");
                    FileOutputStream fos = new FileOutputStream(f);
                    b.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    fos.close();
                    bigBitmap.recycle();
                } else {
                    YuvImage y = new YuvImage(data, format, width, height, null);
                    FileOutputStream fos = new FileOutputStream(f);
                    y.compressToJpeg(decodeRegion, 95, fos);
                    fos.close();
                    b = BitmapFactory.decodeFile(f.getAbsolutePath());
                }
                double[] vals = extractLineFromBitmap(b, orientation);
                b.recycle();
                data = null;

                long decoded = System.currentTimeMillis();
                Log.i(TAG, "Image read time: " + (decoded - start) + "ms");
                start = System.currentTimeMillis();

                //do java decoding
                BarcodeExtractor e = new BarcodeExtractor(vals);
                barcode = e.multiExtract(new BarcodeSpec[] {new CodabarSpec(), new UPCASpec(), new EANSpec(), new EAN8Spec()});

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
