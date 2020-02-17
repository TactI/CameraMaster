package com.example.cameramaster.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.opengl.GLES11Ext;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CameraProxy extends BaseCameraProxy{
    public static final String TAG = "CameraProxy";

    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private Camera.Size mPrewSize;
    private View mPreView;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording;
    private long startTime,endTime;


    public CameraProxy(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void setParam(VideoCaptureParam param,ICaptureDataCallback callback){
        this.mParam = param;
        this.mCallback = callback;
        this.mPreView = mParam.getPreview();
    }

    @Override
    public void startCapture(){
        if (mCamera != null){
            Log.e(TAG,"camera already open!");
            return;
        }
        openCamera();
        // 没有摄像头
        if (mCamera == null) {
            return;
        }
        setCameraParam();
        setPreview();
        mCamera.addCallbackBuffer(new byte[mPrewSize.width * mPrewSize.height * 3 / 2]);
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mCallback != null){
                    mCallback.captureData(data);
                }
                mCamera.addCallbackBuffer(data);
            }
        });
        mCamera.startPreview();
    }

    @Override
    public void stopCapture(){
        if (mCamera != null){
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void startRecord() throws CameraException{
        // 判断录像功能是否开启
        if (!mParam.isIsRecord()){
            throw new CameraException(CameraException.RECORD_UNAVIABLE_EXCEPTION);
        }
        // 判断录像路径是否保存路径
        if (TextUtils.isEmpty(mParam.getVideoPath())){
            throw new CameraException(CameraException.VIDEO_PATH_EXCEPTION);
        }
        // 录制最短时间要小于最大时间
        if (mParam.getMinRecordTime() > mParam.getMaxRecordTime()){
            throw new CameraException(CameraException.PARAM_EXCEPTION);
        }
        if (mCamera == null){
            openCamera();
        }
        // 没有摄像头
        if (mCamera == null) {
            return;
        }
        startTime = System.currentTimeMillis();
        isRecording = true;
        setCameraParam();
        setPreview();
        if (setUpMediaRecorder()){
            mMediaRecorder.start();
        }
    }

    @Override
    public void stopRecord() throws CameraException {
        if (mMediaRecorder != null && isRecording){
            endTime = System.currentTimeMillis();
            if ((endTime-startTime) < mParam.getMinRecordTime() * 1000){
                stopVideo(false);
                throw new CameraException(CameraException.TIME_SHORT_EXCEPTION);
            }else{
                stopVideo(true);
            }
        }
    }

    /**
     *  停止录像
     */
    private void stopVideo(boolean isSave){
        if (mMediaRecorder == null){
            return;
        }
        if (isSave){
            mMediaRecorder.stop();
            mMediaRecorder.release();
        }else{
            if (mFile != null){
                mFile.delete();
            }
            mMediaRecorder.reset();
            mMediaRecorder.release();
        }
        isRecording = false;
    }
    /**
     *  打开摄像头
     */
    private void openCamera(){
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for(int i=0; i<numCameras; i++){
            if (i == mParam.getCameraId()){
                Camera.getCameraInfo(i,cameraInfo);
                mCameraInfo = cameraInfo;
                mCamera = Camera.open(mParam.getCameraId());
            }
        }
    }
    /**
     * 设置预览参数
     */
    private void setCameraParam() {
        Camera.Parameters parameters = mCamera.getParameters();
        // 获取支持的分辨率
        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();
        mPrewSize = getCloselyPreSize(mParam.getWidth(),mParam.getHeight(),sizeList);
        // 设置预览宽高
        parameters.setPreviewSize(mPrewSize.width,mPrewSize.height);
        // 设置预览数据格式
        parameters.setPreviewFormat(ImageFormat.NV21);
        // 设置预览帧范围
//        parameters.setPreviewFpsRange(10,20);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(mParam.getDegree());
    }

    /**
     * 设置预览界面
     */
    private void setPreview(){
        if (mPreView == null){
            // 不设置SurfaceTexture不会有预览帧数据回调
            try {
                // 主要是surfaceTexture获取预览数据，但不显示
                SurfaceTexture surfaceTexture = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mPreView instanceof SurfaceView){
            SurfaceView surfaceView = (SurfaceView) mPreView;
            try {
                mCamera.setPreviewDisplay(surfaceView.getHolder());
                surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {

                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                        stopVideo(false);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if (mPreView instanceof TextureView){
            TextureView textureView = (TextureView) mPreView;
            try {
                mCamera.setPreviewTexture(textureView.getSurfaceTexture());
                textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                        stopVideo(false);
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择）
     * @return 得到与原宽高比例最接近的尺寸
     */
    private Camera.Size getCloselyPreSize(int surfaceWidth, int surfaceHeight,
                                            List<Camera.Size> preSizeList) {

        int ReqTmpWidth;
        int ReqTmpHeight;
        ReqTmpWidth = surfaceWidth;
        ReqTmpHeight = surfaceHeight;
        //先查找preview中是否存在与surfaceview相同宽高的尺寸
        for(Camera.Size size : preSizeList){
            if((size.width == ReqTmpWidth) && (size.height == ReqTmpHeight)){
                return size;
            }
        }
        // 得到与传入的宽高比最接近的size
        float reqRatio = ((float) ReqTmpWidth) / ReqTmpHeight;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Camera.Size retSize = null;
        for (Camera.Size size : preSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }
        return retSize;
    }

    /**
     *  配置MediaReorder
     */
    private boolean setUpMediaRecorder(){
        try {
            //要在实例化MediaRecorder之前就解锁好相机
            mCamera.unlock();
            if (mMediaRecorder == null){
                mMediaRecorder = new MediaRecorder();
                // 必须在初始化后设置
                mMediaRecorder.setCamera(mCamera);
                // 这两项需要放在setOutputFormat之前
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                // Set output file format
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                // 这两项需要放在setOutputFormat之后
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            }
            setRecordSurface();
            mMediaRecorder.setMaxDuration(mParam.getMaxRecordTime() * 1000);
//            mMediaRecorder.setVideoEncodingBitRate(3 * 1024 * 1024);
            mMediaRecorder.setOrientationHint(mParam.getDegree());
            mMediaRecorder.setVideoSize(mPrewSize.width,mPrewSize.height);
            mMediaRecorder.setVideoFrameRate(mParam.getFrameRate());
            mMediaRecorder.setOutputFile(getFileName());
            mMediaRecorder.prepare();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"fail to setUp MediaRecorder!");
            mMediaRecorder = null;
            stopCapture();
            return false;
        }
    }

    /**
     *  设置录屏预览
     */
    private void setRecordSurface(){
        if (mPreView == null){
            SurfaceTexture surfaceTexture = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
            mMediaRecorder.setPreviewDisplay(new Surface(surfaceTexture));
        }
        if (mPreView instanceof SurfaceView){
            SurfaceView surfaceView = (SurfaceView) mPreView;
            mMediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {

                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    stopVideo(false);
                }
            });
        }else if (mPreView instanceof TextureView){
            TextureView textureView = (TextureView) mPreView;
            mMediaRecorder.setPreviewDisplay(new Surface(textureView.getSurfaceTexture()));
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    stopVideo(false);
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });
        }
    }
    /**
     *  录像文件名称
     */
    private String getFileName(){
        mFile = new File(mParam.getVideoPath() + System.currentTimeMillis() + FORMAT);
        mFile.getParentFile().mkdirs();
        try {
            mFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mFile.getAbsolutePath();
    }
}
