package com.siyanhui.mojif.bqliveapp;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BQLCameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private int mCurrentCameraFacing;
    private int mPriviewWidth = 0;
    private int mPreviewHeight = 0;
    private SparseArray<String> mFlashModes = new SparseArray<>();
    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
        }
    };
    private CameraOpenListener mOpenListener;

    public BQLCameraPreview(Context context) {
        super(context);
        init();
    }

    public BQLCameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BQLCameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mCameraInfo = new Camera.CameraInfo();
        mCurrentCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mPriviewWidth != 0 && mPreviewHeight != 0) {
            float width = MeasureSpec.getSize(widthMeasureSpec);
            int height = (int) (width * mPriviewWidth / mPreviewHeight);
            setMeasuredDimension((int) width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            Rect touchRect = new Rect(
                    (int) (x - 100),
                    (int) (y - 100),
                    (int) (x + 100),
                    (int) (y + 100));


            final Rect targetFocusRect = new Rect(
                    touchRect.left * 2000 / this.getWidth() - 1000,
                    touchRect.top * 2000 / this.getHeight() - 1000,
                    touchRect.right * 2000 / this.getWidth() - 1000,
                    touchRect.bottom * 2000 / this.getHeight() - 1000);

            if (mCamera == null) {
                openCamera(mCurrentCameraFacing, getHolder());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && mCamera != null) {
                List<Camera.Area> focusList = new ArrayList<Camera.Area>();
                Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
                focusList.add(focusArea);

                Camera.Parameters param = mCamera.getParameters();
                try {
                    param.setFocusAreas(focusList);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                param.setMeteringAreas(focusList);
                try {
                    mCamera.setParameters(param);
                    mCamera.autoFocus(mAutoFocusCallback);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private Camera.Size chooseBestCameraSize(List<Camera.Size> sizes, int width, int height, boolean rotated) {
        Camera.Size optimalPreviewSize = null;
        float targetRatio = (float) width / height;
        int targetSize = width * height;
        float highestScore = 0;
        for (Camera.Size size : sizes) {
            int currentWidth;
            int currentHeight;
            //预览分辨率是否需要旋转？
            if (rotated) {
                currentWidth = size.height;
                currentHeight = size.width;
            } else {
                currentWidth = size.width;
                currentHeight = size.height;
            }
            float currentRatio = (float) currentWidth / currentHeight;
            float currentRatioDiff = Math.abs(currentRatio - targetRatio);
            float currentSize = currentWidth * currentHeight - targetSize;
            float score = currentRatioDiff < 0.02 ? 1 : 1 / (50 * currentRatioDiff);
            score += currentSize > targetSize ? 1 : currentSize / targetSize;
            if (score > highestScore) {
                highestScore = score;
                optimalPreviewSize = size;
            }
        }
        return optimalPreviewSize;
    }

    private void calculateCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalPreviewSize;

        int rotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            default:
                degrees = 270;
                break;
        }
        int result;
        if (mCurrentCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (mCameraInfo.orientation + 360 - degrees) % 360;
        }
        mCamera.setDisplayOrientation(result);
        boolean rotated = result == 90 || result == 270;
        optimalPreviewSize = chooseBestCameraSize(sizes, getWidth(), getHeight(), rotated);
        mPriviewWidth = optimalPreviewSize.width;
        mPreviewHeight = optimalPreviewSize.height;
        requestLayout();
        parameters.setPreviewSize(mPriviewWidth, mPreviewHeight);

        float previewRatio = (float) mPriviewWidth / mPreviewHeight;
        float minRatioDiff = Float.MAX_VALUE;
        Camera.Size selectedPicSize = null;
        List<Camera.Size> picSizes = parameters.getSupportedPictureSizes();
        for (Camera.Size size : picSizes) {
            float currentRatioDiff = Math.abs((float) size.width / size.height - previewRatio);
            if (currentRatioDiff < minRatioDiff) {
                minRatioDiff = currentRatioDiff;
                selectedPicSize = size;
            }
        }
        parameters.setPictureSize(selectedPicSize.width, selectedPicSize.height);
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        }
        if (mFlashModes.get(mCurrentCameraFacing) != null) {
            parameters.set("flash-mode", mFlashModes.get(mCurrentCameraFacing));
        }
        mCamera.setParameters(parameters);
    }

    private void openCamera(int facing, SurfaceHolder holder) {
        //打开相机
        Camera.CameraInfo currentCameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, currentCameraInfo);
            if (currentCameraInfo.facing == facing) {
                try {
                    mCamera = Camera.open(i);
                    mCameraInfo = currentCameraInfo;
                    break;
                } catch (RuntimeException e) {
                    if (mOpenListener != null) {
                        mOpenListener.onOpenFail();
                    }
                    return;
                }
            }
        }
        if (mCamera == null && Camera.getNumberOfCameras() != 0) {
            try {
                mCamera = Camera.open(0);
                Camera.getCameraInfo(0, mCameraInfo);
                facing = mCameraInfo.facing;
            } catch (RuntimeException e) {
                if (mOpenListener != null) {
                    mOpenListener.onOpenFail();
                }
                return;
            }
        }
        if (mCamera == null) {
            if (mOpenListener != null) {
                mOpenListener.onOpenFail();
            }
            return;
        }
        mCurrentCameraFacing = facing;
        calculateCameraParameters();
        //设置相机预览
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mCamera.release();
            mCamera = null;
            if (mOpenListener != null) {
                mOpenListener.onOpenFail();
            }
            return;
        }
        if (mOpenListener != null) {
            mOpenListener.onOpenSuccess();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        openCamera(mCurrentCameraFacing, holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera == null) {
            return;
        }
        getHolder().removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }


    public Camera getCamera() {
        return mCamera;
    }

    public int getCameraFacing() {
        return mCurrentCameraFacing;
    }

    public void setCameraOpenListener(CameraOpenListener listener) {
        mOpenListener = listener;
    }

    public int getCameraOrientation() {
        return mCameraInfo.orientation;
    }

    public void setFlashMode(String flashMode) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.set("flash-mode", flashMode);
            mCamera.setParameters(parameters);
            mFlashModes.put(mCurrentCameraFacing, flashMode);
        }
    }

    public void changeFacing() {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        if (mCurrentCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            openCamera(Camera.CameraInfo.CAMERA_FACING_BACK, getHolder());
        } else {
            openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT, getHolder());
        }

    }

    public void retryOpenCamera() {
        openCamera(mCurrentCameraFacing, getHolder());
    }

    public interface CameraOpenListener {
        void onOpenFail();

        void onOpenSuccess();
    }
}
