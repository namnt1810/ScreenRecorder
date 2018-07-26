package com.vng.screenrecorder;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.util.DisplayMetrics;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author namnt4
 * @since 7/25/2018
 */
public class ScreenRecorder {
    private static final int mDensity = DisplayMetrics.DENSITY_MEDIUM;
    private static final int mBps = 1000000;
    private static final int mFps = 30;
    private static final int mIFrameInterval = 5;
    private static final int mProfile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline;

    private ProjectionWrapper mProjection;
    private MuxerWrapper mMuxer;
    private ScreenVideoEncoder mVideoEncoder;
//    private ScreenAudioWrapper mAudioRecorder;
    private AtomicBoolean mIsRecording;

    private final Context mContext;
    private final int mResultCode;
    private final Intent mData;
    private final String mFilePath;
    private final int mWidth;
    private final int mHeight;

    public ScreenRecorder(Context context, int resultCode, Intent data, String filePath) {
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mIsRecording = new AtomicBoolean(false);
        mContext = context;
        mResultCode = resultCode;
        mData = data;
        mWidth = displayMetrics.widthPixels;
        mHeight = displayMetrics.heightPixels;
        mFilePath = filePath;

        mProjection = new ProjectionWrapper(mContext, mResultCode, mData);
        mMuxer = new MuxerWrapper(mFilePath);
//        mAudioRecorder = new ScreenAudioWrapper(mMuxer);
        mVideoEncoder = new ScreenVideoEncoder(mMuxer, mWidth, mHeight, mBps, mProfile, mFps, mIFrameInterval);
    }

    public void startRecording() {
        if (mIsRecording.compareAndSet(false, true)) {
//            mAudioRecorder.startRecording();
            mVideoEncoder.start();
            mProjection.startRecording(mVideoEncoder.getInputSurface(), mWidth, mHeight, mDensity);
        }
    }

    public void pause() {
        mProjection.stopRecording();
    }

    public void resume() {
        mProjection = new ProjectionWrapper(mContext, mResultCode, mData);
        mProjection.startRecording(mVideoEncoder.getInputSurface(), mWidth, mHeight, mDensity);
    }

    public void stopRecording() {
        if (mIsRecording.compareAndSet(true, false)) {
            mProjection.stopRecording();
            mVideoEncoder.release();
//            mAudioRecorder.stopRecording();
            mMuxer.releaseMuxer();
        }
    }

    public void release() {
        stopRecording();
        mMuxer = null;
        mProjection = null;
        mVideoEncoder = null;
//        mAudioRecorder = null;
    }
}
