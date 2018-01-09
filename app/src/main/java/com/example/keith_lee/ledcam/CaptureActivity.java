package com.example.keith_lee.ledcam;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Toast;

public class CaptureActivity extends AppCompatActivity {
    private static final String TAG = "CaptureActivity";
    Preview preview;
    Camera camera;
    Context ctx;

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

                camera.setDisplayOrientation(0);

                Camera.Parameters params = camera.getParameters();

                params.setRotation(0);
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
        setContentView(R.layout.activity_capture);

        camera = Camera.open();

        Camera.Parameters param = camera.getParameters();
        param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        camera.setParameters(param);
        camera.startPreview();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        camera.release();
    }
}
