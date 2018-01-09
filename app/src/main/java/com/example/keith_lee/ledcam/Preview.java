package com.example.keith_lee.ledcam;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

/**
 * Created by keith_lee on 2018. 1. 9..
 */

public class Preview extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = "Preview";

    SurfaceView cSurfaceview;
    SurfaceHolder cHolder;
    Camera.Size cPreviewSize;
    List<Camera.Size> cSupportedPreviewSizes;
    Camera cCamera;

    Preview(Context context, SurfaceView sv){
        super(context);

        cSurfaceview = sv;

        cHolder = cSurfaceview.getHolder();
        cHolder.addCallback(this);
        cHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera){
        if(cCamera != null){
            cCamera.stopPreview();

            cCamera.release();

            cCamera = null;
        }

        cCamera = camera;
        if(cCamera != null){
            List<Camera.Size> localSizes = cCamera.getParameters().getSupportedPreviewSizes();
            cSupportedPreviewSizes = localSizes;
            requestLayout();

            Camera.Parameters params = cCamera.getParameters();

            List<String> focusModes = params.getSupportedFocusModes();
            if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                cCamera.setParameters(params);
            }

            try{
                cCamera.setPreviewDisplay(cHolder);
            } catch(IOException e){
                e.printStackTrace();
            }

            cCamera.startPreview();
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if(cSupportedPreviewSizes != null){
            cPreviewSize = getOptimalPreviewSize(cSupportedPreviewSizes, width, height);
        }
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b){
        if(changed && getChildCount() > 0){
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if(cPreviewSize != null){
                previewWidth = cPreviewSize.width;
                previewHeight = cPreviewSize.height;
            }

            if(width * previewHeight > height * previewWidth){
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0, (width + scaledChildWidth) / 2, height);
            } else{
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2, width, (height + scaledChildHeight) / 2);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder){
        Log.d("@@@", "surfaceCreated");

        try{
            if(cCamera != null){
                cCamera.setPreviewDisplay(holder);
            }
        } catch(IOException exception){
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder){
        if(cCamera != null){
            cCamera.stopPreview();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h){
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if(sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for(Camera.Size size : sizes){
            double ratio = (double) size.width / size.height;
            if(Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if(Math.abs(size.height - targetHeight) < minDiff){
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if(optimalSize == null){
            minDiff = Double.MAX_VALUE;
            for(Camera.Size size : sizes){
                if(Math.abs(size.height - targetHeight) < minDiff){
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h){
        if(cCamera != null){
            Camera.Parameters parameters = cCamera.getParameters();
            List<Camera.Size> allSizes = parameters.getSupportedPreviewSizes();
            Camera.Size size = allSizes.get(0);

            for(int i=0; i<allSizes.size(); i++){
                if(allSizes.get(i).width > size.width){
                    size = allSizes.get(i);
                }
            }

            parameters.setPreviewSize(size.width, size.height);

            cCamera.startPreview();
        }
    }
}
