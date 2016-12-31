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
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Timer;
import java.util.TimerTask;
import android.graphics.Matrix;
import android.widget.ImageView;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {

    private Camera camera;
    private CameraPreview cameraPreview;
    private Cipher cipher;
    private int numberOfPhotos = 0;
    private int numberShown = 0;
    private byte[][] allData = new byte[10][];
    private static final int MAGIC_ORIENTATION_NUMBER = 90;
    private static final int TOTAL_PHOTOS = 10;
    private static final int PAUSE_TIME  = 500;
    private static final String TAG = "matag";
    private static final String FILENAME = "super_secure_file_do_not_open";
    private AESCrypt myCrypt;

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
        /*try {
            byte[] key = ("ABCDEFGHIJKLMNOP").getBytes();
            secretKeySpec = new SecretKeySpec(key, "AES");

            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        } catch (NoSuchAlgorithmException e) {

        } catch (NoSuchPaddingException e) {

        }*/
        try {
            myCrypt = new AESCrypt("password");
        } catch (java.lang.Exception e) {}

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

        // init cipher
        /*try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            Log.d(TAG, "Cipher encrypt init successful ");
        } catch (InvalidKeyException e) {
            Log.d(TAG, e.getMessage());
        }*/

        // write current data
        for (int i = 0; i < TOTAL_PHOTOS; i ++) {
            try {
                FileOutputStream fos = openFileOutput(FILENAME + i, Context.MODE_PRIVATE);
                /*CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                cos.write(bytes[i]);
                cos.flush();
                Log.d(TAG, "finished writing data to file " + i + ", bytes written: " + fos.getChannel().size());
                cos.close();*/
                try {
                    fos.write(myCrypt.encrypt(bytes[i]));
                } catch (java.lang.Exception e) {}
                fos.close();
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

        // array of byte arrays to read into
        byte[][] readBytes = new byte[10][];
        /*
        // init cipher
        try {
            byte[] IV = ("ABCDEFGHIJKLMNOP").getBytes();
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec (IV));
            Log.d(TAG, "Cipher decrypt init successful ");
        } catch (InvalidKeyException e) {
            Log.d(TAG, e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            Log.d(TAG, e.getMessage());
        }*/

        // read data
        for (int i = 0; i < TOTAL_PHOTOS; i++) {
            try {
                FileInputStream fis = openFileInput(FILENAME + i);
                /*CipherInputStream cis = new CipherInputStream(fis, cipher);
                //readBytes[i] = new byte[(int) fis.getChannel().size()];
                readBytes[i] = new byte[allData[i].length];

                cis.read(readBytes[i]);
                cis.close();*/
                readBytes[i] = new byte[(int) fis.getChannel().size()];
                fis.read(readBytes[i]);
                fis.close();
                try {
                    readBytes[i] = myCrypt.decrypt(readBytes[i]);
                } catch (java.lang.Exception e) {}

                Log.d(TAG, "finished reading data from file " + i + ", bytes read: " + readBytes[i].length);

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

    private class AESCrypt {

        private final Cipher cipher;
        private final SecretKeySpec key;
        private AlgorithmParameterSpec spec;


        public AESCrypt(String password) throws Exception
        {
            // hash password with SHA-256 and crop the output to 128-bit for key
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(password.getBytes("UTF-8"));
            byte[] keyBytes = new byte[32];
            System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);

            cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            key = new SecretKeySpec(keyBytes, "AES");
            spec = getIV();
        }

        public AlgorithmParameterSpec getIV()
        {
            byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };
            IvParameterSpec ivParameterSpec;
            ivParameterSpec = new IvParameterSpec(iv);

            return ivParameterSpec;
        }

        public byte[] encrypt(byte[] og) throws Exception
        {
            //String plainText = new String(og);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encrypted = cipher.doFinal(og);
            //String encryptedText = new String(Base64.encode(encrypted, Base64.DEFAULT), "UTF-8");

            return encrypted;
        }

        public byte[] decrypt(byte[] notOG) throws Exception
        {
            //String cryptedText = new String(notOG);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            //byte[] bytes = Base64.decode(cryptedText, Base64.DEFAULT);
            byte[] decrypted = cipher.doFinal(notOG);
            //String decryptedText = new String(decrypted, "UTF-8");

            return decrypted;
        }
    }

}
