package com.example.cameramaster.camera;

import android.view.View;

public class VideoCaptureParam {
    // 摄像头Id
    private int mCameraId;
    // 帧率
    private int mFrameRate;
    // 宽
    private int mWidth;
    // 高
    private int mHeight;
    // 是否自动聚焦
    private boolean mAutoFocus;
    // 是否使用Camera2的API
    private boolean mUseCamera2;
    // 预览View
    private View mPreview;
    // 旋转角度
    private int mDegree;
    // 是否启动录制功能
    private boolean mIsRecord;
    // 最短录制时间
    private int mMinRecordTime;
    // 最长录制时间
    private int mMaxRecordTime;
    // 录制文件保存地址
    private String mVideoPath;
    public VideoCaptureParam(Builder builder){
        this.mCameraId = builder.cameraId;
        this.mFrameRate = builder.frameRate;
        this.mWidth = builder.width;
        this.mHeight = builder.height;
        this.mAutoFocus = builder.autoFocus;
        this.mUseCamera2 = builder.useCamera2;
        this.mPreview = builder.preview;
        this.mIsRecord = builder.isRecord;
        this.mMinRecordTime = builder.minRecordTime;
        this.mVideoPath = builder.videoPath;
        this.mDegree = builder.degree;
        this.mMaxRecordTime = builder.maxRecordTime;
    }

    public int getCameraId() {
        return mCameraId;
    }

    public int getFrameRate() {
        return mFrameRate;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public boolean isAutoFocus() {
        return mAutoFocus;
    }

    public boolean isUseCamera2() {
        return mUseCamera2;
    }

    public View getPreview() {
        return mPreview;
    }

    public boolean isIsRecord() {
        return mIsRecord;
    }

    public int getMinRecordTime() {
        return mMinRecordTime;
    }

    public String getVideoPath() {
        return mVideoPath;
    }

    public int getDegree() {
        return mDegree;
    }

    public int getMaxRecordTime() {
        return mMaxRecordTime;
    }

    public static class Builder{
        // 默认后置
        private int cameraId = 0;
        // 默认帧率
        private int frameRate = 15;
        private int width;
        private int height;
        private boolean autoFocus = false;
        private boolean useCamera2 = false;
        private View preview;
        // 旋转角度
        private int degree;
        // 默认关闭录像功能
        private boolean isRecord = false;
        // 最短时间默认5 单位秒
        private int minRecordTime = 5;
        // 录制最长时间
        private int maxRecordTime = 30;
        private String videoPath;

        public Builder setCameraId(int cameraId){
            this.cameraId = cameraId;
            return this;
        }
        public Builder setFrameRate(int frameRate){
            this.frameRate = frameRate;
            return this;
        }
        public Builder setWidth(int width){
            this.width = width;
            return this;
        }
        public Builder setHeight(int height){
            this.height = height;
            return this;
        }
        public Builder setAutoFocus(boolean autoFocus){
            this.autoFocus = autoFocus;
            return this;
        }
        public Builder setUseCamera2(boolean useCamera2){
            this.useCamera2 = useCamera2;
            return this;
        }
        public Builder setPreview(View preview){
            this.preview = preview;
            return this;
        }
        public Builder setRecord(boolean record) {
            this.isRecord = record;
            return this;
        }
        public Builder setMinRecordTime(int minRecordTime) {
            this.minRecordTime = minRecordTime;
            return this;
        }
        public Builder setVideoPath(String videoPath){
            this.videoPath = videoPath;
            return this;
        }
        public Builder setDegree(int degree){
            this.degree = degree;
            return this;
        }
        public Builder setMaxRecordTime(int maxRecordTime){
            this.maxRecordTime = maxRecordTime;
            return this;
        }
        public VideoCaptureParam build(){
            return new VideoCaptureParam(this);
        }
    }
}
