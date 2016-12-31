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
import java.io.FileInputStream;
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
    private byte[][] allData = new byte[10][];
    private static final int MAGIC_ORIENTATION_NUMBER = 90;
    private static final int TOTAL_PHOTOS = 10;
    private static final int PAUSE_TIME  = 500;
    private static final String TAG = "matag";
    private static final String FILENAME = "super_secure_file_do_not_open";

    // used in getOutputMediaFile
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    // callback that stores picture data after picture is taken
    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "picture taken with size " + data.length);

            // append the data
            allData[numberOfPhotos-1] = data;

            // if we've taken all 10 photos, we're ready to write
            if (numberOfPhotos == TOTAL_PHOTOS) {
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
    private void capturePhotos() {
        Log.d(TAG, "capturePhotos called...");
        final Timer timer = new Timer();

        TimerTask takeAndStorePhoto = new TimerTask() {
            @Override
            public void run() {
                if (cameraPreview.safeToTakePicture) {
                    numberOfPhotos++;
                    camera.takePicture(null, null, pictureCallback);
                    if (numberOfPhotos == TOTAL_PHOTOS) {
                        timer.cancel();
                    }
                }
            }
        };

        timer.schedule(takeAndStorePhoto, 0, PAUSE_TIME);
    }

    // now that we have all our data, lets write it
    private void writeAllDataToFile() {
        File dir = getFilesDir();
        File f = new File(dir, FILENAME);
        f.delete();

        File file = new File(getApplicationContext().getFilesDir(), "myFiles");
        //File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        try {
            Log.d(TAG, "writing data to file...");
            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            int numBytes = 0;
            for (int i = 0; i < allData.length; i ++) {
                fos.write(allData[i]);
                numBytes += allData[i].length;
            }
            fos.close();
            Log.d(TAG, "finished writing data to file, total bytes written: " + numBytes);
            readDataFromFile(numBytes);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private void readDataFromFile(int numBytes) {
        try {
            Log.d(TAG, "reading data from file...");
            FileInputStream fis = openFileInput(FILENAME);
            byte[] bytes = new byte[numBytes];
            fis.read(bytes);
            Log.d(TAG, "finished reading data from file, total bytes read: " + bytes.length);
            fis.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
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
}
