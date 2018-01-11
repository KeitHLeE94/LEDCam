package com.example.keith_lee.ledcam;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.sqrt;

public class CaptureActivity extends AppCompatActivity {
    private static final String TAG = "CaptureActivity";
    Preview preview;
    Camera camera;
    Context ctx;
    double bDev = 0;
    double gDev = 0;
    double rDev = 0;
    double bAvr;
    double gAvr;
    double rAvr;

    private final static int PERMISSIONS_REQUEST_CODE = 100;
    private final static int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;
    private AppCompatActivity cActivity;

    public static void doRestart(Context c){
        try{
            if(c != null){
                PackageManager pm = c.getPackageManager();

                if(pm != null){
                    Intent cStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );

                    if(cStartActivity != null){
                        cStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        int cPendingIntentId = 223344;

                        PendingIntent cPendingIntent = PendingIntent.getActivity(c, cPendingIntentId, cStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);

                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, cPendingIntent);

                        System.exit(0);
                    } else{
                        Log.e(TAG, "Was not able to restart application, " +
                        "cStartActivity null");
                    }
                } else{
                    Log.e(TAG, "Was not able to restart application, pm null");
                }
            } else{
                Log.e(TAG, "Was not able to restart application, Context null");
            }
        } catch(Exception ex){
            Log.e(TAG, "Was not able to restart application");
        }
    }

    public void startCamera(){
        if(preview == null){
            preview = new Preview(this, (SurfaceView) findViewById(R.id.surfaceView));
            preview.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
            ((FrameLayout) findViewById(R.id.layout)).addView(preview);
            preview.setKeepScreenOn(true);
        }

        preview.setCamera(null);
        if(camera != null){
            camera.release();
            camera = null;
        }

        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0){
            try{
                camera = Camera.open(CAMERA_FACING);
                camera.setDisplayOrientation(setCameraDisplayOrientation(this, CAMERA_FACING, camera));

                Camera.Parameters params = camera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                //params.setRotation(setCameraDisplayOrientation(this, CAMERA_FACING, camera));
                camera.setParameters(params);

                camera.startPreview();

            } catch(RuntimeException ex){
                Toast.makeText(ctx, "camera_not_found " + ex.getMessage().toString(), Toast.LENGTH_LONG).show();
                Log.d(TAG, "camera not found " + ex.getMessage().toString());
            }
        }

        preview.setCamera(camera);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        cActivity = this;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture);

        ImageButton button = (ImageButton) findViewById(R.id.btnCapture);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                camera.takePicture(shutterCallback, rawCallback, jpegCallback);
            }
        });

        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                int hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
                int hasWriteStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if(hasCameraPermission == PackageManager.PERMISSION_GRANTED && hasWriteStoragePermission == PackageManager.PERMISSION_GRANTED){
                    ;
                }
                else{
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
                }
            }
            else{
                ;
            }
        }
        else{
            Toast.makeText(CaptureActivity.this, "Camera Not Supported", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        startCamera();
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(camera != null){
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;
        }

        ((FrameLayout) findViewById(R.id.layout)).removeView(preview);
        preview = null;
    }

    private void resetCam(){
        startCamera();
    }

    private void refreshGallery(File file){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            Log.d(TAG, "onShutter'd ");
        }
    };

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken - raw");
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            int w = camera.getParameters().getPictureSize().width;
            int h = camera.getParameters().getPictureSize().height;
            long bSum = 0, gSum = 0, rSum = 0;

            int orientation = setCameraDisplayOrientation(CaptureActivity.this, CAMERA_FACING, camera);


            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);

            for(int i=0; i<1000; i++){
                for(int j=0; j<500; j++){
                    bSum += bitmap.getPixel(i, j) & 0x000000FF;
                    gSum += (bitmap.getPixel(i, j) & 0x0000FF00) >> 8;
                    rSum += (bitmap.getPixel(i, j) & 0x00FF0000) >> 16;
                }
            }

            long numOfPixels = 1000 * 500;

            //rgb값 평균
            bAvr = bSum / numOfPixels;
            gAvr = gSum / numOfPixels;
            rAvr = rSum / numOfPixels;

            //rgb값 표준편차
            for(int i=0; i<1000; i++){
                for(int j=0; j<500; j++){
                    double bSquare = ((bitmap.getPixel(i, j) & 0x0000FF) * (bitmap.getPixel(i, j) & 0x0000FF)) - (bAvr * bAvr);
                    bDev += bSquare / numOfPixels;
                    double gSquare = (((bitmap.getPixel(i, j) & 0x0000FF00) >> 8) * ((bitmap.getPixel(i, j) & 0x0000FF00) >> 8)) - (gAvr * gAvr);
                    gDev += gSquare / numOfPixels;
                    double rSquare = (((bitmap.getPixel(i, j) & 0x00FF0000) >> 16) * ((bitmap.getPixel(i, j) & 0x00FF0000) >> 16)) - (rAvr * rAvr);
                    rDev += rSquare / numOfPixels;
                }
            }

            bDev = sqrt(bDev);
            gDev = sqrt(gDev);
            rDev = sqrt(rDev);

            show();

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] currentData = stream.toByteArray();

            new SaveImageTask().execute(currentData);
            resetCam();
            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };

    private class SaveImageTask extends AsyncTask<byte[], Void, Void>{

        @Override
        protected Void doInBackground(byte[]... data){
            FileOutputStream outStream = null;

            try{
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/LEDCam");
                dir.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);

                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

                refreshGallery(outFile);
            } catch(FileNotFoundException e){
                e.printStackTrace();
            } catch(IOException e){
                e.printStackTrace();
            } finally{

            }
            return null;
        }
    }

    public static int setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grandResults){
        if(requestCode == PERMISSIONS_REQUEST_CODE && grandResults.length > 0){
            int hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            int hasWriteExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if(hasCameraPermission == PackageManager.PERMISSION_GRANTED && hasWriteExternalStoragePermission == PackageManager.PERMISSION_GRANTED){
                doRestart(this);
            }
            else{
                checkPermissions();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions(){
        int hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int hasWriteExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        boolean cameraRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
        boolean writeExternalStorageRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if((hasCameraPermission == PackageManager.PERMISSION_DENIED && cameraRationale)
                || (hasWriteExternalStoragePermission == PackageManager.PERMISSION_DENIED && writeExternalStorageRationale)){
            showDialogForPermission("앱을 실행하시려면 퍼미션을 허가해야 합니다.");
        }
        else if((hasCameraPermission == PackageManager.PERMISSION_DENIED && !cameraRationale)
                || (hasWriteExternalStoragePermission == PackageManager.PERMISSION_DENIED && !writeExternalStorageRationale)){
            showDialogForPermissionSetting("퍼미션 거부 + Don't ask again(다시 묻지 않음) " +
            "체크 박스를 설정한 경우로 설정에서 퍼미션을 허가해야 합니다.");
        }
        else if(hasCameraPermission == PackageManager.PERMISSION_GRANTED
                || (hasWriteExternalStoragePermission == PackageManager.PERMISSION_GRANTED)){
            doRestart(this);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(CaptureActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                ActivityCompat.requestPermissions(CaptureActivity.this,
                        new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_CODE);
            }
        });

        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showDialogForPermissionSetting(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(CaptureActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package: " + cActivity.getPackageName()));
                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                cActivity.startActivity(myAppSettings);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    void show()
    {
        final List<String> ListItems = new ArrayList<>();
        ListItems.add("평균(R, G, B): " + "(" + rAvr + ", " + gAvr + ", " + bAvr + ")");
        ListItems.add("표준 편차(R, G, B): " + "(" + rDev + ", " + gDev + ", " + bDev + ")");
        final CharSequence[] items =  ListItems.toArray(new String[ ListItems.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("RGB 분석 결과");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {
                String selectedText = items[pos].toString();
                Toast.makeText(CaptureActivity.this, selectedText, Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }
}
