package com.example.cameramaster.camera;

import android.content.Context;

import java.io.File;

public abstract class BaseCameraProxy {
    public Context mContext;
    public VideoCaptureParam mParam;
    public ICaptureDataCallback mCallback;
    public static final String FORMAT = ".mp4";
    public File mFile;
    // 设置参数
    public abstract void setParam(VideoCaptureParam param,ICaptureDataCallback callback);
    // 开始采集
    public abstract void startCapture();
    // 关闭采集
    public abstract void stopCapture();
    // 开始录像
    public abstract void startRecord() throws CameraException;
    // 关闭录像
    public abstract void stopRecord() throws CameraException;
}
