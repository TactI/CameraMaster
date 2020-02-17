package com.example.cameramaster.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Proxy extends BaseCameraProxy {
    public static final String TAG = "Camera2Proxy";
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private CameraCharacteristics cameraCharacteristics;
    private View mPreView;
    private Size mPreviewSize;
    private Surface surface;
    private ImageReader mImageReader;
    private MediaRecorder mMediaRecorder;
    private CaptureRequest.Builder mPreviewBuilder;
    // 是否是录像
    private boolean isRecording = false;
    private long startTime,endTime;
    // 支持的fps范围
    private static Range<Integer>[] fpsRanges;

    public Camera2Proxy(Context mContext) {
        this.mContext = mContext;
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public void setParam(VideoCaptureParam param,ICaptureDataCallback callback){
        this.mParam = param;
        this.mCallback = callback;
        this.mPreView = mParam.getPreview();
    }

    /**
     * 相机状态监听
     */
    private class CameraStateCallBack extends CameraDevice.StateCallback {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            List<Surface> surfaces = new ArrayList<>();
            getPreViewSize();
            getSurface();
            if (surface != null){
                surfaces.add(surface);
            }
            setUpImageReader();
            surfaces.add(mImageReader.getSurface());
            try {
                mCameraDevice.createCaptureSession(surfaces,new Camera2Proxy.CaptureSessionCallBack() ,mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to create capture session. " + e);
                return;
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (mCameraDevice != null){
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (mCameraDevice != null){
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }
    }
    /**
     * 捕捉会话监听
     */
    private class CaptureSessionCallBack extends CameraCaptureSession.StateCallback{

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "Camera capture session configured.");
            mCaptureSession = session;
            try {
                getPreviewBuilder();
                mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(),new CaptureCallBack(),mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to start capture request. " + e);
                stopCapture();
                return;
            }

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            session.close();
            if (mCameraDevice != null){
                mCameraDevice.close();
                mCameraDevice = null;
            }
            Log.e(TAG,"Failed to configure capture session.");
        }
    }

    /**
     * 捕捉结果监听
     */
    private class CaptureCallBack extends CameraCaptureSession.CaptureCallback{
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {

        }
    }

    @Override
    public void startCapture(){
        if (mCameraDevice != null){
            Log.e(TAG,"camera already open!");
            return;
        }
        try {
            openCamera();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopCapture(){
        if (mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mCaptureSession != null){
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mImageReader != null){
            mImageReader.close();
            mImageReader = null;
        }
        if (mMediaRecorder != null){
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        isRecording = false;
        stopBackgroundThread();
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
        isRecording = true;
        if (mCameraDevice == null){
            Log.e(TAG,"please first open camera!");
            return;
        }
        if (mMediaRecorder == null){
            mMediaRecorder = new MediaRecorder();
        }
        prepareMediaRecorder();
        startTime = System.currentTimeMillis();
        mMediaRecorder.start();
    }

    @Override
    public void stopRecord() throws CameraException{
        endTime = System.currentTimeMillis();
        if ((endTime-startTime) < mParam.getMinRecordTime() * 1000){
            stopCapture();
            if (mFile != null){
                mFile.delete();
            }
            throw new CameraException(CameraException.TIME_SHORT_EXCEPTION);
        }else {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            stopCapture();
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() throws CameraAccessException {
        startBackgroundThread();
        cameraCharacteristics = mCameraManager.getCameraCharacteristics(mParam.getCameraId()+"");
        mCameraManager.openCamera(mParam.getCameraId()+"",new CameraStateCallBack(),mHandler);
    }

    //清除Session
    private void closeSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }
    private void startBackgroundThread() {
        if (mHandlerThread == null || mHandler == null) {
            Log.v(TAG, "startBackgroundThread");
            mHandlerThread = new HandlerThread("Camera2Background");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
        }
    }
    private void stopBackgroundThread() {
        Log.v(TAG, "stopBackgroundThread");
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置ImageReader
     */
    private void setUpImageReader() {
        //前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 1);
        //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                if(image == null){
                    return;
                }
                int w = image.getWidth(), h = image.getHeight();
                // size是宽乘高的1.5倍 可以通过ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)得到
                int i420Size = w * h * 3 / 2;

                Image.Plane[] planes = image.getPlanes();
                //remaining0 = rowStride*(h-1)+w => 27632= 192*143+176 Y分量byte数组的size
                int remaining0 = planes[0].getBuffer().remaining();
                int remaining1 = planes[1].getBuffer().remaining();
                //remaining2 = rowStride*(h/2-1)+w-1 =>  13807=  192*71+176-1 V分量byte数组的size
                int remaining2 = planes[2].getBuffer().remaining();
                //获取pixelStride，可能跟width相等，可能不相等
                int pixelStride = planes[2].getPixelStride();
                int rowOffest = planes[2].getRowStride();
                byte[] nv21 = new byte[i420Size];
                //分别准备三个数组接收YUV分量。
                byte[] yRawSrcBytes = new byte[remaining0];
                byte[] uRawSrcBytes = new byte[remaining1];
                byte[] vRawSrcBytes = new byte[remaining2];
                planes[0].getBuffer().get(yRawSrcBytes);
                planes[1].getBuffer().get(uRawSrcBytes);
                planes[2].getBuffer().get(vRawSrcBytes);
                if (pixelStride == w) {
                    //两者相等，说明每个YUV块紧密相连，可以直接拷贝
                    System.arraycopy(yRawSrcBytes, 0, nv21, 0, rowOffest * h);
                    System.arraycopy(vRawSrcBytes, 0, nv21, rowOffest * h, rowOffest * h / 2 - 1);
                } else {
                    //根据每个分量的size先生成byte数组
                    byte[] ySrcBytes = new byte[w * h];
                    byte[] uSrcBytes = new byte[w * h / 2 - 1];
                    byte[] vSrcBytes = new byte[w * h / 2 - 1];
                    for (int row = 0; row < h; row++) {
                        //源数组每隔 rowOffest 个bytes 拷贝 w 个bytes到目标数组
                        System.arraycopy(yRawSrcBytes, rowOffest * row, ySrcBytes, w * row, w);
                        //y执行两次，uv执行一次
                        if (row % 2 == 0) {
                            //最后一行需要减一
                            if (row == h - 2) {
                                System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w - 1);
                            } else {
                                System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w);
                            }
                        }
                    }
                    //yuv拷贝到一个数组里面
                    System.arraycopy(ySrcBytes, 0, nv21, 0, w * h);
                    System.arraycopy(vSrcBytes, 0, nv21, w * h, w * h / 2 - 1);
                }
                image.close();
                // 图像数据
                if (mCallback != null){
                    mCallback.captureData(nv21);
                }
            }
        }, null);
    }

    /**
     * 设置请求参数
     */
    private void getPreviewBuilder() throws CameraAccessException {
        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        // 该相机的FPS范围
        fpsRanges = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (fpsRanges != null){
            // 设置预览画面的帧率 视实际情况而定选择一个帧率范围
//            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRanges[0]);
        }
        if (surface != null){
            mPreviewBuilder.addTarget(surface);
        }
        mPreviewBuilder.addTarget(mImageReader.getSurface());
    }

    /**
     * 设置合理的预览尺寸
     */
    private void getPreViewSize(){
        // 获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mPreviewSize = Camera2Util.getOptimalSize(map.getOutputSizes(SurfaceTexture.class), mParam.getWidth(), mParam.getHeight());
    }
    /**
     * 获取表层输出目标
     */
    private void getSurface(){
        if (mPreView == null){
            return;
        }
        if (mPreView instanceof TextureView){
            TextureView textureView = (TextureView) mPreView;
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mParam.getWidth(),mParam.getHeight());
            surface = new Surface(surfaceTexture);
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    stopCapture();
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });
        }else if (mPreView instanceof SurfaceView){
            SurfaceView surfaceView = (SurfaceView) mPreView;
            surface = surfaceView.getHolder().getSurface();
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {

                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    stopCapture();
                }
            });
        }
    }

    /**
     *  预览录像
     */
    private void prepareMediaRecorder() {
        try {
            closeSession();
            Log.e(TAG, "prepare MediaRecorder");
            setUpMediaRecorder();
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();
            if (surface != null){
                surfaces.add(surface);
                mPreviewBuilder.addTarget(surface);
            }

            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        mCaptureSession = session;
                        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                        mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "捕获的异常：" + e.toString());
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 录像配置
     */
    private void setUpMediaRecorder() {
        try {
            Log.e(TAG, "setUpMediaRecorder");
            mMediaRecorder.reset();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            // 录制出来10S的视频，大概1.2M，清晰度不错，而且避免了因为手动设置参数导致无法录制的情况
            // 手机一般都有这个格式CamcorderProfile.QUALITY_480P,因为单单录制480P的视频还是很大的，所以我们在手动根据预览尺寸配置一下videoBitRate,值越高越大
            // QUALITY_QVGA清晰度一般，不过视频很小，一般10S才几百K
            // 判断有没有这个手机有没有这个参数
            if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
                CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
                profile.videoBitRate = mPreviewSize.getWidth() * mPreviewSize.getHeight();
                mMediaRecorder.setProfile(profile);
                mMediaRecorder.setPreviewDisplay(surface);
            } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
                CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
                profile.videoBitRate = mPreviewSize.getWidth() * mPreviewSize.getHeight();
                mMediaRecorder.setProfile(profile);
                mMediaRecorder.setPreviewDisplay(surface);
            } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_QVGA)) {
                mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA));
                mMediaRecorder.setPreviewDisplay(surface);
            } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_CIF)) {
                mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_CIF));
                mMediaRecorder.setPreviewDisplay(surface);
            } else {
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mMediaRecorder.setVideoFrameRate(mParam.getFrameRate());
                mMediaRecorder.setVideoEncodingBitRate(2500000);
                mMediaRecorder.setVideoSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            }
            mMediaRecorder.setMaxDuration(mParam.getMaxRecordTime() * 1000);
            mMediaRecorder.setOutputFile(getFileName());
            mMediaRecorder.setOrientationHint(mParam.getDegree());
            mMediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"fail to setUp mediaRecorder!");
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
