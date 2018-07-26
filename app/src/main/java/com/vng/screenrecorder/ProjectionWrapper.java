package com.vng.screenrecorder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.view.Surface;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;

/**
 * Created by taitt on 30/03/2017.
 */
@TargetApi(21)
public class ProjectionWrapper {

    private static final String DISPLAY_NAME = "screenrecording";

    private MediaProjection mProjection;

    private VirtualDisplay mVirtualDisplay;

    public ProjectionWrapper(Context context, int resultCode, Intent data) {
        MediaProjectionManager projectionManager = (MediaProjectionManager) context.getSystemService(MEDIA_PROJECTION_SERVICE);
        mProjection = projectionManager.getMediaProjection(resultCode, data);
    }

    public void startRecording(Surface surface, int width, int height, int density) {
        mVirtualDisplay = mProjection.createVirtualDisplay(DISPLAY_NAME,
                width, height, density,
                VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                surface, null, null);
    }

    public void stopRecording() {
        mVirtualDisplay.release();
        mProjection.stop();
    }
}
