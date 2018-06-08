package com.nvt.cameracapturedemo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static android.os.Environment.DIRECTORY_DOWNLOADS;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String KEY_IMAGE_STORAGE_PATH = "image_path";

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int BITMAP_SAMPLE_SIZE = 8;

    // Gallery directory name to store the images or videos
    public static final String GALLERY_DIRECTORY_NAME = "Hello Camera";

    // Image and Video file extensions
    public static final String IMAGE_EXTENSION = "jpg";
    public static final String VIDEO_EXTENSION = "mp4";

    private static String imageStoragePath;

    private ImageView imgPreview;
    private Button btnTakePhoto;
    SurfaceView surfaceView;
    android.hardware.Camera.ShutterCallback shutterCallback;
    android.hardware.Camera.PictureCallback jpegCallback;
    android.hardware.Camera camera;
    Bitmap bitmap;
    RelativeLayout rltPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!CameraUtils.isDeviceSupportCamera(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "Sorry ! Your device is not support camera", Toast.LENGTH_LONG).show();
            finish();
        }
        imgPreview = findViewById(R.id.imgCaptured);
        surfaceView = findViewById(R.id.captureSurfaceView);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        rltPicture = findViewById(R.id.rltPicture);
        jpegCallback = new android.hardware.Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, android.hardware.Camera camera) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, (bytes) != null ? bytes.length : 0);
                File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS) + "/" + System.currentTimeMillis() + ".jpg");
                imageStoragePath = file.getAbsolutePath();
                Matrix matrix = new Matrix();
                matrix.setRotate(90);
                FileOutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    outStream.flush();
                    outStream.close();
                    Bitmap second = addText(bitmap);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    second.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(),
                            second, imageStoragePath, "" + new Date().getTime());
                    previewCapturedImage();
                } catch (Exception e) {
                    e.getMessage();
                    Log.d("getMessage", e.getMessage());
                }


            }


        };
        restoreFromBundle(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (CameraUtils.checkPermissions(getApplicationContext())) {
            captureImage();
        } else
            requestCameraPermission(1);
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.takePicture(shutterCallback, null, jpegCallback);
            }
        });

    }

    private void captureImage() {
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        camera = android.hardware.Camera.open();
        try {
            setFocus();
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        rltPicture.setVisibility(View.VISIBLE);

    }

    private void requestCameraPermission(final int type) {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {

                            if (type == MEDIA_TYPE_IMAGE) {
                                // capture picture
                                captureImage();
                            }

                        } else if (report.isAnyPermissionPermanentlyDenied()) {
                            showPermissionsAlert();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }



    private void showPermissionsAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissions required!")
                .setMessage("Camera needs few permissions to work properly. Grant them in settings.")
                .setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        CameraUtils.openSettings(MainActivity.this);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    private void previewCapturedImage() {
        try {

            // hide video preview
            rltPicture.setVisibility(View.GONE);
            imgPreview.setVisibility(View.VISIBLE);

            Bitmap bitmap = CameraUtils.optimizeBitmap(BITMAP_SAMPLE_SIZE, imageStoragePath);

            imgPreview.setImageBitmap(bitmap);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on screen orientation
        // changes
        outState.putString(KEY_IMAGE_STORAGE_PATH, imageStoragePath);
    }

    /**
     * Restoring image path from saved instance state
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        imageStoragePath = savedInstanceState.getString(KEY_IMAGE_STORAGE_PATH);
    }

    private void restoreFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_IMAGE_STORAGE_PATH)) {
                imageStoragePath = savedInstanceState.getString(KEY_IMAGE_STORAGE_PATH);
                if (!TextUtils.isEmpty(imageStoragePath)) {
                    if (imageStoragePath.substring(imageStoragePath.lastIndexOf(".")).equals("." + IMAGE_EXTENSION)) {
                        previewCapturedImage();
                    } else if (imageStoragePath.substring(imageStoragePath.lastIndexOf(".")).equals("." + VIDEO_EXTENSION)) {
                        //previewVideo();
                    }
                }
            }
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    private Bitmap addText(Bitmap toEdit){

        Bitmap dest = toEdit.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(dest);

        Paint paint = new Paint();  //set the look
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK);

        int pictureHeight = dest.getHeight();
        paint.setTextSize(pictureHeight * .04629f);
        canvas.drawLine(1,101,dest.getWidth()/2,101,paint);
        canvas.drawText("Hello World" , dest.getWidth()/2,  100, paint);
        return dest;
    }
    private void setRotation(android.hardware.Camera camera, android.hardware.Camera.Parameters parameters) {
        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(getApplicationContext().WINDOW_SERVICE);
        int degrees = 0;
        switch (windowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                Log.e(TAG, "Bad rotation value");
        }

        android.hardware.Camera.CameraInfo cameraInfo = new android.hardware.Camera.CameraInfo();

        int angle;
        int displayAngle;
        if (cameraInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
            angle = (cameraInfo.orientation + degrees) % 360;
            displayAngle = (360 - angle) % 360; // compensate for it being mirrored
        } else {  // back-facing
            angle = (cameraInfo.orientation - degrees + 360) % 360;
            displayAngle = angle;
        }


        camera.setDisplayOrientation(90);
        parameters.setRotation(0);
    }
    public void setFocus() {
        android.hardware.Camera.Parameters params = camera.getParameters();

        List<android.hardware.Camera.Size> sizes = params.getSupportedPictureSizes();
        android.hardware.Camera.Size size = sizes.get(0);
        for (int i = 0; i < sizes.size(); i++) {

            if (sizes.get(i).width > size.width)
                size = sizes.get(i);


        }
        params.setPictureSize(size.width, size.height);
        if (params.getSupportedFocusModes().contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else {
            params.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);
        }
        params.setJpegQuality(100);
        camera.setParameters(params);
    }


}
