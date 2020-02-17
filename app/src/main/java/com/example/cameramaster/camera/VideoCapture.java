package com.example.cameramaster.camera;

import android.content.Context;
import android.os.Build;

public class VideoCapture {
    private VideoCaptureParam mCaptureParam;
    private Context context;
    private ICaptureDataCallback mCallBack;
    private BaseCameraProxy baseCameraProxy;

    public VideoCapture(VideoCaptureParam mCaptureParam, Context context) {
        this.mCaptureParam = mCaptureParam;
        this.context = context;
    }

    public void setCaptureParam(VideoCaptureParam videoCaptureParam){
        this.mCaptureParam = videoCaptureParam;
    }

    public void setCallBack(ICaptureDataCallback mCallBack) {
        this.mCallBack = mCallBack;
    }

    /**
     * 开启预览
     */
    public void startPreview() throws CameraException {
        if (mCaptureParam == null) throw new CameraException(CameraException.PARAM_EXCEPTION);
        getProxy();
        baseCameraProxy.startCapture();
    }

    /**
     * 关闭预览
     */
    public void stopPreview(){
        if (mCaptureParam == null || baseCameraProxy == null) return;
        baseCameraProxy.stopCapture();
    }

    /**
     * 开始录制
     */
    public void startRecordVideo() throws CameraException {
        if (mCaptureParam == null) throw new CameraException(CameraException.PARAM_EXCEPTION);
        getProxy();
        baseCameraProxy.startRecord();
    }

    /**
     * 关闭预览
     */
    public void stopVideoRecord() throws CameraException {
        if (mCaptureParam == null || baseCameraProxy == null) return;
        baseCameraProxy.stopRecord();
    }

    /**
     * 根据参数构造实例
     */
    private void getProxy(){
        if (baseCameraProxy == null){
            if (mCaptureParam.isUseCamera2()){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // 5.0以上才有
                    baseCameraProxy = new Camera2Proxy(context);
                }
            }else{
                baseCameraProxy = new CameraProxy(context);
            }
        }
        baseCameraProxy.setParam(mCaptureParam,mCallBack);
    }
}
