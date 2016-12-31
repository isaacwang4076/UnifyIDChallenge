package com.example.isaacwang.unifyidchallenge;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Camera camera;
    private CameraPreview cameraPreview;
    private int numberOfPhotos = 0;
    private byte[] allData = new byte[0];
    private static final int MAGIC_ORIENTATION_NUMBER = 90;
    private static final String TAG = "matag";

    // used in getOutputMediaFile
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "picture taken...");

            // append the data
            byte[] combined = new byte[allData.length + data.length];
            System.arraycopy(allData, 0, combined, 0, allData.length);
            System.arraycopy(data, 0, combined, allData.length, data.length);
            allData = combined;

            if (numberOfPhotos == 10) {
                writeAllDataToFile();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Do we have a camera?
        if (checkCameraHardware(getApplicationContext())) {

            // Do we still need permission?
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //ask for authorisation
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 50);
            }
        }
    }

    public void beginScan(View v) {
        // if camera is set up successfully
        if (setUpCamera()) {

            // get rid of the button
            ((LinearLayout) findViewById(R.id.mainLayout)).removeView(findViewById(R.id.button));

            // get to work
            capturePhotos();
        }
    }

    // returns whether camera is set up successfully
    private boolean setUpCamera() {
        camera = getCameraInstance();
        if (camera == null) {
            return false;
        }

        // initialize the preview
        cameraPreview = new CameraPreview(this, camera);
        // add it to the layout
        ((FrameLayout) findViewById(R.id.camera_preview)).addView(cameraPreview);

        return true;
    }

    // sets timertask to capture photos periodically
    public void capturePhotos() {
        Log.d(TAG, "capturePhotos called...");
        final Timer timer = new Timer();

        TimerTask takeAndStorePhoto = new TimerTask() {
            @Override
            public void run() {
                if (cameraPreview.safeToTakePicture) {
                    Log.d(TAG, "safe");
                    numberOfPhotos++;
                    camera.takePicture(null, null, pictureCallback);
                    if (numberOfPhotos == 10) {
                        timer.cancel();
                    }
                } else {
                    Log.d(TAG, "not yet safe");
                }
            }
        };

        timer.schedule(takeAndStorePhoto, 0, 500);
    }

    private void writeAllDataToFile() {
        File file = new File(getApplicationContext().getFilesDir(), "myFiles");
        //File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            //Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            //Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    // Does this device have a camera?
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    // Get the camera if we have permission
    // Returns null if we hit an exception
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); // attempt to get a Camera instance

            // camera is oriented sideways by default, this seems like a bit of a hack
            // but it sets the camera's orientation to portrait
            c.setDisplayOrientation(MAGIC_ORIENTATION_NUMBER);
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
}
