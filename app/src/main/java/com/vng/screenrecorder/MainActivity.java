package com.vng.screenrecorder;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_MEDIA_PROJECTION = 9001;

    private Button mRecord;
    private boolean mIsRecording = false;
    private boolean mIsPaused = false;
    private ScreenRecorder mRecorder;
    private File mOutputFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecord = findViewById(R.id.record);
        mRecord.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        if (mIsRecording && mRecorder != null) {
            mRecorder.pause();
            mIsPaused = true;
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mIsPaused && mRecorder != null) {
            mRecorder.resume();
            mIsPaused = false;
            mIsRecording = true;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.record:
                if (!mIsRecording) {
                    startRecording();
                } else {
                    stopRecording();
                    mRecord.setText(R.string.start);
                    mIsRecording = false;
                }
                break;
        }
    }

    private void startRecording() {
        final MediaProjectionManager mediaProjection = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjection.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.release();
        }
        mIsRecording = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (REQUEST_MEDIA_PROJECTION == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                setupRecorder(resultCode, data);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setupRecorder(int resultCode, Intent data) {
        try {
            createRecordFile();
            mRecorder = new ScreenRecorder(this, resultCode, data, mOutputFile.getPath());
            mRecorder.startRecording();
            mRecord.setText(R.string.stop);
            mIsRecording = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createRecordFile() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss", Locale.getDefault());
        String time = sdf.format(new Date());
        String fileName = String.format(Locale.US, "%s/ScreenRecord/rec_%s.mp4", Environment.getExternalStorageDirectory().getPath(), time);
        mOutputFile = new File(fileName);
        if (!mOutputFile.exists()) {
            mOutputFile.getParentFile().mkdirs();
            mOutputFile.createNewFile();
        }
    }
}
