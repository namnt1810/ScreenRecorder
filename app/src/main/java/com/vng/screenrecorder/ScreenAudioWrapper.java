package com.vng.screenrecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.SystemClock;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by taitt on 15/02/2017.
 */

public class ScreenAudioWrapper extends ZBaseMediaEncoder {

    private static final String AUDIO_FORMAT = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int SAMPLE_RATE = 44100;
    private AudioRecord audioRecord;
    private MuxerWrapper muxer;
    private ZQuitJoinThread recordThread;
    private ZQuitJoinThread drainThread;
    private BlockingQueue<byte[]> audioBuffers;

    public ScreenAudioWrapper(MuxerWrapper muxer) {
        super("ZAudioEncoder");
        this.muxer = muxer;
        try {
            encoder = MediaCodec.createEncoderByType(AUDIO_FORMAT);
            encoder.configure(getFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
        } catch (IOException e) {
            throw new RuntimeException("can not create audio encoder ", e);
        }
        this.setup();
    }

    @Override
    protected MediaFormat getFormat() {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, AUDIO_FORMAT);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 32 * 1024);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, SAMPLE_RATE);
        return format;
    }

    @Override
    protected void onOutputFormatChanged(MediaFormat outputFormat) {
        muxer.addAudioTrack(outputFormat);
    }

    @Override
    protected void onOutputData(ByteBuffer realData, MediaCodec.BufferInfo bufferInfo) {
        muxer.writeAudioData(realData, bufferInfo);
    }

    private void setup() {
        final int audioRecoderSampleRate = SAMPLE_RATE;
        final int audioRecoderSliceSize = audioRecoderSampleRate / 10;
        final int audioRecoderSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        final int audioRecoderChannelConfig = AudioFormat.CHANNEL_IN_MONO;
        final int audioRecoderFormat = AudioFormat.ENCODING_PCM_16BIT;

        int minBufferSize = AudioRecord.getMinBufferSize(audioRecoderSampleRate,
                audioRecoderChannelConfig,
                audioRecoderFormat);
        this.audioRecord = new AudioRecord(audioRecoderSource,
                audioRecoderSampleRate,
                audioRecoderChannelConfig,
                audioRecoderFormat,
                minBufferSize * 5);
        if (AudioRecord.STATE_INITIALIZED != audioRecord.getState())
            throw new RuntimeException("audioRecord.getState()!=AudioRecord.STATE_INITIALIZED!");
        if (AudioRecord.SUCCESS != audioRecord.setPositionNotificationPeriod(audioRecoderSliceSize))
            throw new RuntimeException("AudioRecord.SUCCESS != audioRecord.setPositionNotificationPeriod");
    }

    public void startRecording() {
        start();

        audioRecord.startRecording();
        audioBuffers = new ArrayBlockingQueue<>(1000);

        final int audioRecoderBufferSize = 2048;
        (recordThread = new ZQuitJoinThread("AudioRecordThread") {

            @Override
            public void run0() throws Exception {
                byte[] audioBuffer = new byte[audioRecoderBufferSize];
                audioRecord.read(audioBuffer, 0, audioBuffer.length);
                audioBuffers.put(audioBuffer);
            }
        }).start();
        (drainThread = new ZQuitJoinThread("AudioDrainThread") {
            @Override
            public void run0() throws Exception {
                byte[] mic = audioBuffers.take();
                process(AudioHelper.mono2Stereo(mic, mic), SystemClock.uptimeMillis());
            }
        }).start();
    }

    public void stopRecording() {
        if (recordThread != null) recordThread.quitThenJoin();
        if (drainThread != null) drainThread.quitThenJoin();
        recordThread = null;
        drainThread = null;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (audioBuffers != null) audioBuffers.clear();

        release();
    }
}
