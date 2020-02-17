package com.example.cameramaster.camera;

public class CameraException extends Exception{
    // 参数异常
    public static final int PARAM_EXCEPTION = 0;
    // 录像功能未打开
    public static final int RECORD_UNAVIABLE_EXCEPTION = 1;
    // 录像路径异常
    public static final int VIDEO_PATH_EXCEPTION = 2;
    // 录像时间过短异常
    public static final int TIME_SHORT_EXCEPTION = 3;

    private int errCode;

    public CameraException(int errCode) {
        this.errCode = errCode;
    }

    public CameraException(String message, int errCode) {
        super(message);
        this.errCode = errCode;
    }

    public CameraException(String message, Throwable cause, int errCode) {
        super(message, cause);
        this.errCode = errCode;
    }

    public CameraException(Throwable cause, int errCode) {
        super(cause);
        this.errCode = errCode;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }
}
