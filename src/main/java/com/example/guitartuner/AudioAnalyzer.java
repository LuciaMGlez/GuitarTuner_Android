package com.example.guitartuner;

import android.Manifest;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;

import androidx.annotation.RequiresPermission;

/**
 * @author Lucía Miguel González
 * TFG 24/25
 */

public class AudioAnalyzer implements Runnable {

    private final int[] freq_samples = {8000, 22050, 32000, 44100, 48000};
    private AudioRecord microAudio;
    private static final int BUFFER = 8192;
    private static final int FFT_SIZE = 8192;
    private static final float SMOOTHING_FACTOR = 0.3f;
    private static final double MIN_INTENSITY = 40.0;
    private static final float MIN_FREQUENCY = 50.0f;
    private static final float MAX_FREQUENCY = 400.0f;
    private float lastFrequency = 0;

    public interface PitchListener {
        void onPitch(float frequency, double intensity);
    }
    private PitchListener microPitchListener;
    private boolean stop = false;

    public void setPitchListener(PitchListener pitchListener){
        microPitchListener = pitchListener;
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public void launch() {
        int samples = freq_samples.length;
        int sampleRate;
        int i = 0;

        do {
           sampleRate = freq_samples[i];
           int minBufferS = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
           if(minBufferS != AudioRecord.ERROR && minBufferS != AudioRecord.ERROR_BAD_VALUE) {
               microAudio = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(BUFFER, minBufferS * 2));
           }
           i++;
        } while (i < samples && (microAudio == null || microAudio.getState() != AudioRecord.STATE_INITIALIZED));
    }

    public void stop() {
        stop = true;
        if (microAudio != null) {
            if (microAudio.getState() == AudioRecord.STATE_INITIALIZED) {
                microAudio.stop();
            }
            microAudio.release();
        }
    }
    private double calculateIntensity(short[]data, int sampleCount) {
        double total = 0;
        for (int i = 0; i < sampleCount; i++) {
            total += Math.abs(data[i]);
        }
        return total / sampleCount;
    }

    private float detectPitch(short[] audioData, int readData, int sampleRate) {
        if (readData < FFT_SIZE) return -1;

        double[] real = new double[FFT_SIZE];
        double[] imag = new double[FFT_SIZE];

        for (int i = 0; i < FFT_SIZE; i++) {
            real[i] = (audioData[i] / 32768.0);
            imag[i] = 0.0;
        }

        double[] fftResult = FFT.fft(real, imag, true);
        if (fftResult == null) return -1;

        double maxMagnitude = 0;
        int fundamentalIndex = -1;

        for (int i = 1; i < FFT_SIZE / 2; i++) {
            double rl = fftResult[2 * i];
            double im = fftResult[2 * i + 1];
            double magnitude = Math.sqrt(rl * rl + im * im);

            float freq = (float) i * sampleRate / FFT_SIZE;

            if (freq >= MIN_FREQUENCY && freq <= MAX_FREQUENCY && magnitude > maxMagnitude) {
                maxMagnitude = magnitude;
                fundamentalIndex = i;
            }
        }

        if (fundamentalIndex != -1) {
            return (float) fundamentalIndex * sampleRate / FFT_SIZE;
        } else {
            return -1;
        }
    }

    @Override
    public void run() {
        if (microAudio == null || microAudio.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalStateException("El micrófono no está inicializado. Llama primero a launch().");
        }

        microAudio.startRecording();
        short[] audioBuffer = new short[BUFFER];
        int sampleRate = microAudio.getSampleRate();

        while (!stop) {
            int readSamples = microAudio.read(audioBuffer, 0, BUFFER);

            if (readSamples > 0) {
                double intensity = calculateIntensity(audioBuffer, readSamples);

                //Minimum intensity filter
                if (intensity >= MIN_INTENSITY) {
                    float frequency = detectPitch(audioBuffer, readSamples, sampleRate);

                    //correction for frequencies in the second octave of the low E string which are sometimes dtected an octave too high
                    if (frequency > 150 && frequency < 170) {
                        frequency /= 2;
                    }

                    //frequency range filter takes only those between 50 and 400Hz
                    if (frequency >= MIN_FREQUENCY && frequency <= MAX_FREQUENCY) {
                        //exponential smoothing
                        float smoothedFreq = (lastFrequency == 0) ? frequency : (SMOOTHING_FACTOR * frequency + (1 - SMOOTHING_FACTOR) * lastFrequency);

                        if (Math.abs(smoothedFreq - lastFrequency) < 5.0f) {
                            if (microPitchListener != null) {
                                microPitchListener.onPitch(smoothedFreq, intensity);
                            }
                        }

                        lastFrequency = smoothedFreq;
                    }
                }
            }
        }
    }
}
