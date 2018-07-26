package com.vng.screenrecorder;

import android.media.MediaCodec;

/**
 * @author namnt4
 * @since 7/25/2018
 */
public final class Utils {
    public static boolean isSoftwareCodec(MediaCodec codec) {
        String codecName = codec.getCodecInfo().getName();

        return ("OMX.google.h264.encoder".equals(codecName));
    }
}
