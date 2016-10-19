package net.fishandwhistle.openpos;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
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

import net.fishandwhistle.openpos.extractors.BarcodeExtractor;
import net.fishandwhistle.openpos.extractors.ThresholdMultiExtractor;
import net.fishandwhistle.openpos.barcode.BarcodeSpec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BarcodeReaderActivity extends AppCompatActivity implements CameraPreview.PreviewImageCallback,
 Camera.PictureCallback {

    private static final String TAG = "BarcodeReader";
    private static final int READ_DELAY = 1500;

    enum ScanModes {TAP, CONTINUOUS, NONE}

    private Camera mCamera;
    private CameraPreview mPreview;

    private int cameraDisplayOrientation ;
    private ScanModes scanMode;
    private ScanModes userScanMode;
    private boolean enableScanning ;
    private ProgressDialog highResDecodeProgress;
    private BarcodeSpec.Barcode lastBarcode;
    private BarcodeSpec.Barcode lastValidBarcode ;

    private long currentReadRequest ;
    private ImageBarcodeExtractor extractor;

    protected abstract BarcodeExtractor getExtractor();

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
                if(scanMode == ScanModes.CONTINUOUS) {
                    mCamera.autoFocus(null);
                } else if(scanMode == ScanModes.TAP) {
                    if (hasReadRequest()) {
                        //scan already in progress
                        return;
                    }
                    mCamera.cancelAutoFocus();
                    startReadRequest();

                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {
                                // wait 400 ms before taking a picture
                                mPreview.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (hasReadRequest()) {
                                            //still no barcode
                                            mCamera.takePicture(null, null, BarcodeReaderActivity.this);
                                        }
                                        //else ignore
                                    }
                                }, 400);
                            } else {
                                if (hasReadRequest()) {
                                    Toast.makeText(BarcodeReaderActivity.this, "Auto focus failed", Toast.LENGTH_SHORT).show();
                                    finishReadRequest(null);
                                }
                            }
                        }
                    });
                } else if(scanMode == ScanModes.NONE) {
                    Log.i(TAG, "onClick: ignoring click when scanMode=ScanModes.NONE");
                }
            }
        });

        extractor = null;
        cameraDisplayOrientation = -1;
        scanMode = ScanModes.NONE;
        userScanMode = ScanModes.TAP;
        enableScanning = false;
        lastBarcode = null;
        lastValidBarcode = null;
        currentReadRequest = 0;

        highResDecodeProgress = new ProgressDialog(this);
        highResDecodeProgress.setCancelable(false);
        highResDecodeProgress.setIndeterminate(true);
        highResDecodeProgress.setMessage("Reading high-res image...");

    }

    @Override
    public void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        startCameraAsync();
    }

    private void setScanMode(ScanModes scanMode) {
        finishReadRequest(null);
        if(scanMode == ScanModes.CONTINUOUS) {
            this.enableScanning = true;
            startReadRequest();
        } else if(scanMode == ScanModes.TAP) {
            this.enableScanning = false;
        } else if(scanMode == ScanModes.NONE) {
            this.enableScanning = false;
        }
        this.scanMode = scanMode;
    }

    private void startReadRequest() {
        currentReadRequest = System.currentTimeMillis();
        Log.i(TAG, "Launching read request " + currentReadRequest);
        enableScanning = true;
    }

    private boolean hasReadRequest() {
        return currentReadRequest != 0;
    }

    private void finishReadRequest(BarcodeSpec.Barcode b) {
        Log.i(TAG, "Finishing read request " + currentReadRequest);
        extractor = null;
        currentReadRequest = 0;
        if(b != null) {
            lastValidBarcode = b;
        }
        if(scanMode == ScanModes.TAP) {
            enableScanning = false;
            try {
                mCamera.startPreview();
            } catch(Exception e) {
                Log.e(TAG, "finishReadRequest: Could not start preview", e);
            }
            highResDecodeProgress.dismiss();
        }
    }

    private void startCameraAsync() {
        if(mCamera != null) {
            Log.i(TAG, "startCameraAsync: camera instance exists, cancelling asyncrhonous load");
            return;
        }
        Log.i(TAG, "startCameraAsync: starting asynchronous load of mCamera");
        new AsyncTask<Void, Void, Camera>() {
            @Override
            protected Camera doInBackground(Void... v) {
                Camera mCamera = getCameraInstance();
                if(mCamera == null) {
                    return null;
                }
                int orientation = getScreenOrientation();
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
                focusareas.add(new Camera.Area(new Rect(-850, -800, -750, 800), 1000));
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
                return mCamera;
            }

            @Override
            protected void onPostExecute(Camera camera) {
                Log.i(TAG, "onPostExecute: Camera loader returned with " + camera);
                if(camera != null) {
                    mCamera = camera;
                    mPreview.setCamera(mCamera);
                    if(userScanMode == null) {
                        scanMode = ScanModes.TAP;
                    }
                    setScanMode(userScanMode);
                } else {
                    Toast.makeText(BarcodeReaderActivity.this, "Could not open camera", Toast.LENGTH_SHORT).show();
                    setScanMode(ScanModes.NONE);
                }
            }
        }.execute();


    }

    public void stopCamera() {
        setScanMode(ScanModes.NONE);
        mPreview.releaseCamera();
        if(mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    public int getScreenOrientation() {
        Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        return display.getRotation();
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
        if(enableScanning && hasReadRequest()) {
            Log.i(TAG, "Launching barcode extractor (" + currentReadRequest + ")");
            extractor = new ImageBarcodeExtractor(currentReadRequest, data, format, width, height, getScreenOrientation());
            extractor.execute("");
            if(scanMode == ScanModes.CONTINUOUS) {
                enableScanning = false;
            }
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if(!hasReadRequest()) {
            mCamera.startPreview();
            return;
        }
        Camera.Parameters params = camera.getParameters();
        int format = params.getPictureFormat();
        Camera.Size size = params.getPictureSize();
        highResDecodeProgress.show();
        extractor = new ImageBarcodeExtractor(currentReadRequest, data, format, size.width, size.height, getScreenOrientation());
        extractor.execute("");
    }

    private void onBarcodeRead(long request, BarcodeSpec.Barcode b, int format) {
        if(request != currentReadRequest) {
            return;
        }
        boolean highres = format == mCamera.getParameters().getPictureFormat();


        if(scanMode == ScanModes.CONTINUOUS) {
            if(b.isValid && ((lastValidBarcode == null) || (b.timeread - lastValidBarcode.timeread) > READ_DELAY)) {
                if(((lastBarcode != null) && !b.equals(lastBarcode)) && onNewBarcodeWrapper(b)) {
                    finishReadRequest(b);
                } else {
                    finishReadRequest(null);
                }
            } else {
                finishReadRequest(null);
            }
            startReadRequest();
        } else if(scanMode == ScanModes.TAP) {
            if(b.isValid && this.onNewBarcodeWrapper(b)) {
                finishReadRequest(b);
            } else {
                String partial = b.toString();
                if (partial.length() > 0) {
                    partial = " Partial result: " + b.type + "/" + partial;
                }
                Log.i(TAG, "Barcode error." + partial);
                if (highres) {
                    Toast.makeText(this, "Failed to read barcode." + partial, Toast.LENGTH_LONG).show();
                    finishReadRequest(null);
                }
            }
        }

        lastBarcode = b;
    }

    protected boolean onNewBarcodeWrapper(BarcodeSpec.Barcode b) {
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(150);
        return this.onNewBarcode(b);
    }

    protected abstract boolean onNewBarcode(BarcodeSpec.Barcode b) ;

    private Rect getRegion(int orientation, int width, int height) {
        if(orientation == 0) {
            return new Rect(width / 10, 0, width / 10 + 24, height);
        } else if (orientation == 1){
            return new Rect(0, height / 10, width, height / 10 + 24);
        } else if (orientation == 3) {
            return new Rect(0, 9*height / 10, width, 9*height / 10 + 24);
        } else if (orientation == 2) {
            return new Rect(9*width / 10, 0, 9*width / 10 + 24, height);
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
        private long request;

        public ImageBarcodeExtractor(long request, byte[] data, int format, int width, int height, int orientation) {
            this.data = data;
            this.format = format;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.request = request;
        }

        @Override
        protected BarcodeSpec.Barcode doInBackground(String... params) {
            long start = System.currentTimeMillis();
            BarcodeSpec.Barcode barcode = null;
            Rect decodeRegion = getRegion(orientation, width, height);
            try {
                BarcodeExtractor extractor = getExtractor();
                if((format != ImageFormat.NV21 && format != ImageFormat.YUY2)) {
                    barcode = extractor.extractJPEG(data, width, height, orientation, decodeRegion);
                } else {
                    barcode = extractor.extractYUV(data, width, height, orientation, decodeRegion);
                }
                data = null;
                Log.i(TAG, "Extract time: " + (System.currentTimeMillis() - start) + "ms");
            } catch(IOException e) {
                Log.e(TAG, "IO exception on write image", e);
            }
            return barcode;
        }

        @Override
        protected void onPostExecute(BarcodeSpec.Barcode code) {
            BarcodeReaderActivity.this.onBarcodeRead(request, code, format);
        }
    }

}
