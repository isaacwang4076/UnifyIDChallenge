package com.example.isaacwang.unifyidchallenge;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import android.graphics.Matrix;
import android.widget.ImageView;


public class MainActivity extends AppCompatActivity {

    private Camera camera;
    private CameraPreview cameraPreview;
    private int numberOfPhotos = 0;
    private int numberShown = 0;
    private byte[][] allData = new byte[10][];
    private static final int MAGIC_ORIENTATION_NUMBER = 90;
    private static final int TOTAL_PHOTOS = 10;
    private static final int PAUSE_TIME  = 500;
    private static final String TAG = "matag";
    private static final String FILENAME = "super_secure_file_do_not_open";
    private byte[] key;

    // used in getOutputMediaFile
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    // callback that stores picture data after picture is taken
    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "picture taken with size " + data.length);

            // append the data
            /*byte[] combined = new byte[allData.length + data.length];
            System.arraycopy(allData, 0, combined, 0, allData.length);
            System.arraycopy(data, 0, combined, allData.length, data.length);
            allData = combined;*/

            allData[numberOfPhotos-1] = data;

            // if we've taken all 10 photos, we're ready to write
            if (numberOfPhotos == TOTAL_PHOTOS) {

                // hide the preview and show the buttons
                ((FrameLayout) findViewById(R.id.camera_preview)).removeAllViews();
                findViewById(R.id.buttonsLayout).setVisibility(View.VISIBLE);
                writeDataToFile(allData);
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
            findViewById(R.id.buttonsLayout).setVisibility(View.GONE);

            // get to work
            capturePhotos();
        }
    }

    public void showImages(View v) {

        // array that we will read into
        final byte[][] storedBytes = readDataFromFile();

        final Timer timer = new Timer();
        numberShown = 0;

        // task to show an image
        TimerTask showImage = new TimerTask() {
            @Override
            public void run() {

                // we're done showing images
                if (numberShown == TOTAL_PHOTOS) {

                    // stop the TimerTasks
                    timer.cancel();

                    // hide the image
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.imageView).setVisibility(View.GONE);
                        }
                    });

                    return;
                }

                // get the bitmap and rotate it
                byte[] imageBytes = storedBytes[numberShown];
                if (imageBytes == null) {
                    return;
                }
                Bitmap b = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                final Bitmap bRotated = RotateBitmap(b, -MAGIC_ORIENTATION_NUMBER);

                numberShown++;

                // display image
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView imageView = (ImageView) findViewById(R.id.imageView);
                        // set the image
                        imageView.setImageBitmap(bRotated);
                        // make sure we can see it
                        imageView.setVisibility(View.VISIBLE);
                    }
                });

            }
        };

        timer.schedule(showImage, 0, PAUSE_TIME);
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
        numberOfPhotos = 0;

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

    // deletes old data and writes new data
    private void writeDataToFile(byte[][] bytes) {

        // delete old data
        deleteOldData();

        // write current data
        for (int i = 0; i < TOTAL_PHOTOS; i ++) {
            try {
                FileOutputStream fos = openFileOutput(FILENAME + i, Context.MODE_PRIVATE);
                fos.write(bytes[i]);
                fos.close();
                Log.d(TAG, "finished writing data to file " + i);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    }

    public void deleteOldData(View v) {
        deleteOldData();
    }

    private void deleteOldData() {
        // delete old data
        File dir = getFilesDir();
        for (int i = 0; i < TOTAL_PHOTOS; i++) {
            (new File(dir, FILENAME + i)).delete();
        }
    }

    private byte[][] readDataFromFile() {
        byte[][] readBytes = new byte[10][];

        for (int i = 0; i < TOTAL_PHOTOS; i++) {
            try {
                FileInputStream fis = openFileInput(FILENAME + i);
                readBytes[i] = new byte[(int) fis.getChannel().size()];
                fis.read(readBytes[i]);
                fis.close();
                Log.d(TAG, "finished reading data from file " + i);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
        return readBytes;
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

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
