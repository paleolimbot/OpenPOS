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
import android.os.Vibrator;
import android.util.Log;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import net.fishandwhistle.openpos.barcode.BarcodeExtractor;
import net.fishandwhistle.openpos.barcode.BarcodeSpec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BarcodeReaderActivity extends AppCompatActivity implements CameraPreview.PreviewImageCallback,
 Camera.PictureCallback {

    private static final String TAG = "BarcodeReader";


    private Camera mCamera;
    private CameraPreview mPreview;
    private List<ImageBarcodeExtractor> extractors;

    private int cameraDisplayOrientation ;

    private boolean enableScanning ;
    private ProgressDialog highResDecodeProgress;


    protected abstract BarcodeSpec[] getBarcodeSpecs() ;

    protected abstract int getLayoutId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        mPreview = new CameraPreview(this);
        mPreview.setPreviewImageCallback(this);
        final FrameLayout mPreviewF = (FrameLayout) findViewById(R.id.bcreader_imageframe);
        mPreviewF.addView(mPreview, 0);

        mPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(enableScanning) {
                    //scan already in progress
                    return;
                }
                enableScanning = true;

                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if(success) {
                            // wait 250 ms before taking a picture
                            mPreview.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (enableScanning) {
                                        //still no barcode
                                        mCamera.takePicture(null, null, BarcodeReaderActivity.this);
                                    }
                                    //else ignore
                                }
                            }, 400);
                        } else {
                            if(enableScanning) {
                                Toast.makeText(BarcodeReaderActivity.this, "Auto focus failed", Toast.LENGTH_SHORT).show();
                                enableScanning = false;
                            }
                        }
                    }
                });
            }
        });

        mPreview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mCamera.takePicture(null, null, BarcodeReaderActivity.this);
                return true;
            }
        });

        extractors = new ArrayList<>();
        enableScanning = false;
        cameraDisplayOrientation = -1;

        highResDecodeProgress = new ProgressDialog(this);
        highResDecodeProgress.setCancelable(false);
        highResDecodeProgress.setIndeterminate(true);
        highResDecodeProgress.setMessage("Decoding image...");

    }

    @Override
    public void onPause() {
        super.onPause();
        mPreview.releaseCamera();
        mCamera.release();
        mCamera = null;
        this.resetExtractors(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        resetCamera(false);
    }

    private void resetExtractors(boolean restartPreview) {
        for(ImageBarcodeExtractor e: extractors) {
            e.cancel(false);
        }
        extractors.clear();
        if(restartPreview) {
            //try to restart preview (may not have been stopped)
            try {
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "resetExtractors: could not start preview", e);
            }
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
        focusareas.add(new Camera.Area(new Rect(-850, -800, -750, 800), 100));
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
        if(enableScanning) {
            Log.i(TAG, "Launching barcode extractor");
            ImageBarcodeExtractor extractor = new ImageBarcodeExtractor(data, format, width, height, getScreenOrientation());
            extractors.add(extractor);
            extractor.execute("");
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if(!enableScanning) {
            mCamera.startPreview();
            return;
        }
        Camera.Parameters params = camera.getParameters();
        int format = params.getPictureFormat();
        Camera.Size size = params.getPictureSize();
        highResDecodeProgress.show();
        ImageBarcodeExtractor extractor = new ImageBarcodeExtractor(data, format, size.width, size.height, getScreenOrientation());
        extractors.add(extractor);
        extractor.execute("");
    }

    private void onBarcodeRead(BarcodeSpec.Barcode b, int format) {
        boolean notifyFail = format == mCamera.getParameters().getPictureFormat();
        if(enableScanning) {
            //release extractor and close progressDialog, if showing
            if (highResDecodeProgress.isShowing()) {
                highResDecodeProgress.hide();
            }

            if (b.isValid) {
                enableScanning = !this.onNewBarcodeWrapper(b);
                if(!enableScanning) {
                    resetExtractors(true);
                }
            } else {
                String partial = b.toString();
                if (partial.length() > 0) {
                    partial = " Partial result: " + b.type + "/" + partial;
                }
                Log.i(TAG, "Barcode error." + partial);
                if (notifyFail) {
                    Toast.makeText(this, "Failed to read barcode." + partial, Toast.LENGTH_LONG).show();
                }
            }
        }
        if (notifyFail) {
            enableScanning = false;
            resetExtractors(true);
        }
    }

    protected boolean onNewBarcodeWrapper(BarcodeSpec.Barcode b) {
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(150);
        return this.onNewBarcode(b);
    }

    protected abstract boolean onNewBarcode(BarcodeSpec.Barcode b) ;

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
                    //f = new File(Environment.getExternalStorageDirectory(), "temppic.jpg");
                    //FileOutputStream fos = new FileOutputStream(f);
                    //b.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    //fos.close();
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
                boolean dofilter = this.format == ImageFormat.JPEG;
                barcode = e.multiExtract(getBarcodeSpecs(), dofilter);

            } catch(IOException e) {
                Log.e(TAG, "IO exception on write image", e);
            }
            Log.i(TAG, "Barcode read time: " + (System.currentTimeMillis() - start) + "ms");
            return barcode;
        }

        @Override
        protected void onPostExecute(BarcodeSpec.Barcode code) {
            BarcodeReaderActivity.this.onBarcodeRead(code, format);
        }
    }

}
