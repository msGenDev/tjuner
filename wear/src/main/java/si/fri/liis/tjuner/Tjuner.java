package si.fri.liis.tjuner;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * Created by Jan on 27-Jan-15.
 */
public class Tjuner {

    private final static String LOG_TAG = "Tjuner";

    // pitch string
    private TjunerUIListener listener;

    // audio input parameters
    private final int SAMPLE_RATE = 16000;
    private final int CHANEL_MODE = AudioFormat.CHANNEL_IN_MONO;
    private final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    // audio input
    private AudioRecord input;

    // fft window size
    private final int WINDOW_SIZE_IN_SAMPLES = SAMPLE_RATE;//8192;
    private final int WINDOW_SIZE_IN_BYTES = 2 * WINDOW_SIZE_IN_SAMPLES;
    private final int WINDOW_SIZE_IN_MS = 1000 * WINDOW_SIZE_IN_SAMPLES / SAMPLE_RATE; // 1000 ms
    private final int WINDOW_SIZE_IN_SHORTS = SAMPLE_RATE * WINDOW_SIZE_IN_MS / 1000;

    // min and max tuning frequencies
    private final int MIN_TUNING_FREQUENCY = 90;
    private final int MAX_TUNING_FREQUENCY = 1000;

    // pitch frequency
    private int pitch;

    // control tuning loop
    private boolean doTune;

    // Notes
    private final String[] NOTES = {"A", "A#", "H", "C", "C#", "D", "D#", "E", "F", "F#", "G"};
    private final double[] FREQUENCIES = {110.00, 116.54, 123.47, 130.81, 138.59, 146.83, 155.56,
            164.81, 174.61, 185.00, 196.00,220.00, 233.08, 246.94, 261.63, 277.18, 293.66, 311.13,
            329.63, 349.23, 369.99, 392.00, 440.00, 466.16, 493.88, 523.25, 554.37, 587.33, 622.25,
            659.26, 698.46, 739.99, 783.99};

    public Tjuner(TjunerUIListener listener){
        this.doTune = false;
        this.pitch = 0;
        this.listener = listener;
    }

    public void tune(){

        doTune = true;

        new AsyncTask<Void, Void, Void>(){

            @Override
            protected Void doInBackground(Void... params){

                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                // initialize input
                input = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANEL_MODE,
                        ENCODING, WINDOW_SIZE_IN_BYTES);

                // check, if input is initialized correctly
                if (input.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(LOG_TAG, "Audio input not initialized.");
                    return null;
                }

                // array holds input signal
                short[] input_signal = new short[WINDOW_SIZE_IN_SAMPLES];

                // array holds fft transformed signal
                double[] fft_signal = new double[2*WINDOW_SIZE_IN_SAMPLES];

                // init fft class
                DoubleFFT_1D fft = new DoubleFFT_1D(WINDOW_SIZE_IN_SAMPLES);

                // start recording
                input.startRecording();

                while(doTune){

                    input.read(input_signal, 0, WINDOW_SIZE_IN_SHORTS);

                    for (int i=0; i < WINDOW_SIZE_IN_SAMPLES; i++){
                        fft_signal[2*i] = input_signal[i];
                        fft_signal[2*i+1] = 0;
                    }

                    fft.realForward(fft_signal);

                    // apply low pass filter
                    fft_signal = lowPassFilter(fft_signal, 10);

                    // find maximum frequency
                    Map peaksMap = findPeaks(fft_signal, MIN_TUNING_FREQUENCY,
                            MAX_TUNING_FREQUENCY, 2, 90);
                    List<Integer> peaks = new ArrayList();
                    peaks.addAll(peaksMap.values());
                    Collections.reverse(peaks);

                    // leading frequency
                    double leadFreq = peaks.get(0);

                    // find corresponding note and difference between recorded frequency
                    // and correct frequency
                    double diff = Double.MAX_VALUE;
                    int closest = 0;
                    for (int i=0; i<FREQUENCIES.length; i++){
                        double tempDiff = FREQUENCIES[i] - leadFreq;
                        if (Math.abs(tempDiff) < Math.abs(diff)){
                            closest = i;
                            diff = tempDiff;
                        }
                    }
                    String note = NOTES[closest % NOTES.length];

                    // logging for debuging
                    // site that generates test samples http://onlinetonegenerator.com
                    Log.e(LOG_TAG, "Pitch is " + peaks);
                    Log.e(LOG_TAG, "Note is " + note);

                    // call listener, to display data
                    listener.onPitch("" + note, String.format("%.2f", diff));

                }

                if (input != null){
                    input.stop();
                    input.release();
                }

                return null;
            }

        }.execute();
    }

    // listener used to send data to UI
    interface TjunerUIListener {
        void onPitch(String note, String diff);
    }

    // function for finding peaks in signal
    private Map<Double, Integer> findPeaks(double[] signal, int min, int max, int n, int width){

        // signal = imput signal
        // min = minimal frequency to use
        // max = maximal frequency to use
        // n = number of peaks we want to find
        // width = width of peak
        // returns map (amplitude, frequency)

        Map<Double, Integer> peaks = new TreeMap<>();
        int peak = min;
        for (int i=min; i<=max; i++){
            if (signal[i] > signal[peak]){
                peak = i;
            }
        }
        peaks.put(signal[peak], peak);
        if (n > 1){
            peaks.putAll(findPeaks(signal, peak + width, max, n - 1, width));
        }

        return peaks;

    }

    // simple low pass filter
    private double[] lowPassFilter(double[] signal, int smoothing){
        double value = signal[0];
        for (int i=0; i<signal.length; i++){
            double curr = signal[i];
            value += (curr - value) / smoothing;
            signal[i] = value;
        }
        return signal;
    }

}
