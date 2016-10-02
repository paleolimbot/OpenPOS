package net.fishandwhistle.openpos;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by dewey on 2016-09-24.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static String TAG = "CameraPreview";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int pHeight;
    private int pWidth;
    private PreviewImageCallback callback ;

    public CameraPreview(Context context) {
        super(context);
        pHeight = 0;
        pWidth = 0;
        callback = null;
        this.setMeasuredDimension(0, 0);
    }


    public void setPreviewImageCallback(PreviewImageCallback callback) {
        this.callback = callback;
    }

    public void setCamera(Camera camera) {
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void releaseCamera() {
        try {
            mCamera.stopPreview();
        } catch(Exception e) {
            Log.e(TAG, "Exception releasing camera", e);
        }
        mCamera.setPreviewCallback(null);
        mHolder = null;
        mCamera = null;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(callback != null) {
            Camera.Parameters params = camera.getParameters();
            Camera.Size size = params.getPreviewSize() ;
            int format = params.getPreviewFormat() ;
            callback.onPreviewImage(data, format, size.width, size.height);
        }
    }

    public interface PreviewImageCallback {
        void onPreviewImage(byte[] data, int format, int width, int height);
    }

}
