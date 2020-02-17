package com.example.cameramaster;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.cameramaster.camera.CameraException;
import com.example.cameramaster.camera.CameraUtil;
import com.example.cameramaster.camera.ICaptureDataCallback;
import com.example.cameramaster.camera.VideoCapture;
import com.example.cameramaster.camera.VideoCaptureParam;


public class TestActivity extends AppCompatActivity implements View.OnClickListener, ICaptureDataCallback {
    private Button mStart, mStop;
    private TextureView mTextureView;
    private VideoCapture mVideoCapture;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        mTextureView = findViewById(R.id.text_preview);
        mStart = findViewById(R.id.btn_start);
        mStop = findViewById(R.id.btn_stop);
        mStart.setOnClickListener(this);
        mStop.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(TestActivity.this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        || ActivityCompat.checkSelfPermission(TestActivity.this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        || ActivityCompat.checkSelfPermission(TestActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TestActivity.this, new String[]{Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA}, 1);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                if(mVideoCapture == null){
                    startPreview();
                }else{
                    startRecord();
                }
                break;
            case R.id.btn_stop:
//                stopPreview();
                stopRecord();
                break;
        }
    }

    private void startPreview() {
        if (mVideoCapture == null) {
            VideoCaptureParam.Builder builder = new VideoCaptureParam.Builder();
            int cameraId = 0;
            builder.setCameraId(cameraId)
                    .setPreview(mTextureView)
                    .setUseCamera2(true)
                    .setWidth(600)
                    .setHeight(800)
                    .setRecord(true)
                    .setVideoPath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/mp4/")
                    .setDegree(CameraUtil.getDegree(cameraId,TestActivity.this));
            VideoCaptureParam captureParam = builder.build();
            mVideoCapture = new VideoCapture(captureParam, getApplicationContext());
            mVideoCapture.setCallBack(this);
        }
        try {
            mVideoCapture.startPreview();
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        if (mVideoCapture != null) {
            mVideoCapture.stopPreview();
        }
    }

    @Override
    public void captureData(byte[] data) {
        Log.e("TAG", "采集到数据");
    }

    private void startRecord() {
        if (mVideoCapture == null) {
            VideoCaptureParam.Builder builder = new VideoCaptureParam.Builder();
            int cameraId = 0;
            builder.setCameraId(cameraId)
                    .setPreview(mTextureView)
                    .setUseCamera2(true)
                    .setRecord(true)
                    .setVideoPath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/mp4/")
                    .setFrameRate(30)
                    .setWidth(600)
                    .setHeight(800)
                    .setDegree(CameraUtil.getDegree(cameraId,TestActivity.this));
            VideoCaptureParam captureParam = builder.build();
            mVideoCapture = new VideoCapture(captureParam, getApplicationContext());
            mVideoCapture.setCallBack(this);
        }
        try {
            mVideoCapture.startRecordVideo();
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    private void stopRecord(){
        if (mVideoCapture != null){
            try {
                mVideoCapture.stopVideoRecord();
            } catch (CameraException e) {
                e.printStackTrace();
            }
        }
    }
}
