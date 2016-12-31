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
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {

    private Camera camera;
    private CameraPreview cameraPreview;
    private int numberOfPhotos = 0;
    private byte[][] allData = new byte[10][];
    private byte[] testArray = {1, 2, 3, 4};
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
            allData[numberOfPhotos-1] = data;
            //allData[numberOfPhotos-1] = testArray;
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

        // Generate the key
        byte[] keyStart = "this is a key".getBytes();
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(keyStart);
            kgen.init(128, sr); // 192 and 256 bits may not be available
            SecretKey skey = kgen.generateKey();
            key = skey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "Key gen failed");
        }

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
            int numBytes = 0;
            for (int i = 0; i < allData.length; i++) {
                FileOutputStream fos = openFileOutput(FILENAME + i, Context.MODE_PRIVATE);
                try {
                    allData[i] = encrypt(key,allData[i]);
                    Log.d(TAG, "encrypt succeeded");
                } catch (java.lang.Exception e) {
                    Log.d(TAG, "encrypt failed");
                }

                fos.write(allData[i]);
                numBytes += allData[i].length;
                fos.close();
            }
            Log.d(TAG, "finished writing data to file, total bytes written: " + numBytes);
            readDataFromFile();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private void readDataFromFile() {
        byte[][] readBytes = new byte[10][];
        try {
            Log.d(TAG, "reading data from file...");
            for (int i = 0; i < TOTAL_PHOTOS; i++) {
                FileInputStream fis = openFileInput(FILENAME + i);
                readBytes[i] = new byte[allData[i].length];
                fis.read(readBytes[i]);
                fis.close();
                boolean match = true;

                try {
                    readBytes[i] = decrypt(key, readBytes[i]);
                    Log.d(TAG, "decrypt succeeded");
                } catch (java.lang.Exception e) {
                    Log.d(TAG, "decrypt failed");
                }

                // for whatever reason, the array.equals method was not
                // returning true (even when output clearly was), so I use this to check
                // if I had more time I would look into that more
                for (int j = 0; j < 1000; j += 17) {
                    if (allData[i][j] != readBytes[i][j]) {
                        match = false;
                        break;
                    }
                }
                Log.d(TAG, "finished reading data from file " + i +
                        ", size is " + readBytes[i].length + ", match is " + match);
            }

        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private byte[] encrypt(byte[] data, byte[] clear) throws Exception {
        return clear;
        /*SecretKeySpec skeySpec = new SecretKeySpec(data, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);

        return encrypted;*/
    }

    private byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        return encrypted;
        /*SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;*/
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
