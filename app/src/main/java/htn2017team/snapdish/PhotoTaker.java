package htn2017team.snapdish;

import android.annotation.SuppressLint;
//import android.graphics.Camera;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.hardware.Camera;
import android.widget.Button;

import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class PhotoTaker extends AppCompatActivity {
    private Camera mCamera;
    private CameraPreview cameraPreview;
    private Button shutterButton;
    private Button submitButton;
    private CameraState currentCameraState = CameraState.READY;
    private final String RETAKE_IMAGE = "resnap";
    private final String TAKE_IMAGE = "snap";
    private File pictureFile;

    private enum CameraState {
        FROZEN_IMAGE, READY
    }

    public static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phototaker);

        //pick file
        Button button = (Button) findViewById(R.id.button1);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mCamera.setDisplayOrientation(90);
        cameraPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(cameraPreview);

        submitButton = (Button) findViewById(R.id.button_submit);
        shutterButton = (Button) findViewById(R.id.button_capture);

        shutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentCameraState.equals(CameraState.READY)) {
                    mCamera.takePicture(null, null, mPicture);
                    shutterButton.setText(RETAKE_IMAGE);
                    submitButton.setVisibility(View.VISIBLE);
                    currentCameraState = CameraState.FROZEN_IMAGE;
                }
                else if (currentCameraState.equals(CameraState.FROZEN_IMAGE)) {
                    mCamera.startPreview();
                    shutterButton.setText(TAKE_IMAGE);
                    submitButton.setVisibility(View.INVISIBLE);
                    currentCameraState = CameraState.READY;
                }
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: send the current frozen image to IBM Watson
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //pick file
                pickImage();
            }
        });
    }

    public void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }
            try {
                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try  {
                            InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
                            File tempFile = File.createTempFile("animagefile", "jpg");
                            tempFile.deleteOnExit();
                            FileOutputStream out = new FileOutputStream(tempFile);
                            IOUtils.copy(inputStream, out);

                            VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
                            service.setApiKey("f56b5021bedba46eef82952c3ef5d1875a8b7cad");

                            ClassifyImagesOptions options = new ClassifyImagesOptions.Builder()
                                    .images(tempFile)
                                    .build();
                            VisualClassification result = service.classify(options).execute();
                            List<ImageClassification> images = result.getImages();
                            for (int i = 0; i < images.size(); i++) {
                                Log.e("l", images.get(i).toString());
                            };
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                thread.start();

            }
            catch(Exception e) {
                Log.e("watson exc", e.toString());
            }
            //Now you can do whatever you want with your inpustream, save it as file, upload to a server, decode a bitmap...
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.d(TAG, "Error getting camera instance: " + e.getMessage());
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    private void copyInputStreamToFile( InputStream in, File file ) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions.");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try  {

                        VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
                        service.setApiKey("f56b5021bedba46eef82952c3ef5d1875a8b7cad");

                        ClassifyImagesOptions options = new ClassifyImagesOptions.Builder()
                                .images(pictureFile)
                                .classifierIds("food")
                                .build();
                        VisualClassification result = service.classify(options).execute();
                        List<ImageClassification> images = result.getImages();
                        for (int i = 0; i < images.size(); i++) {
                            List<VisualClassifier> classifiers = images.get(i).getClassifiers();
                            for (int j = 0; j < classifiers.size(); j++) {
                                List<VisualClassifier.VisualClass> classes = classifiers.get(j).getClasses();
                                Log.e("most relevant:", classes.get(0).getName() + " : " + classes.get(0).getScore());
                            }
                        };
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        //releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
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
                Log.d("MyCameraApp", "failed to create directory");
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
