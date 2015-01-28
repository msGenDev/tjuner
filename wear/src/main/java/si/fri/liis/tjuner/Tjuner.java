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
    private final int MIN_TUNING_FREQUENCY = 100;
    private final int MAX_TUNING_FREQUENCY = 3000;

    // pitch frequency
    private int pitch;

    // control tuning loop
    private boolean doTune;

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


                    //fft_signal = lowPassFilter(fft_signal, 10);

                    // find maximum frequency
                    Map peaksMap = findPeaks(fft_signal, MIN_TUNING_FREQUENCY, MAX_TUNING_FREQUENCY, 2, 90);
                    List peaks = new ArrayList();
                    peaks.addAll(peaksMap.values());
                    Collections.reverse(peaks);




                    /*
                    List<Integer> ext = new ArrayList<Integer>();
                    for (int i = MIN_TUNING_FREQUENCY; i<MAX_TUNING_FREQUENCY; i++) {
                        if ((fft_signal[i+1]-fft_signal[i])*(fft_signal[i+2]-fft_signal[i+1]) <= 0) { // changed sign?
                            ext.add(i+1);
                        }
                    }*/

                    // test http://onlinetonegenerator.c
                    Log.e(LOG_TAG, "Pitch is " + peaks);
                    listener.onPitch("" + peaks.get(0));

                }

                if (input != null){
                    input.stop();
                    input.release();
                }

                return null;
            }



        }.execute();
    }

    private Map<Double, Integer> findPeaks(double[] signal, int min, int max, int n, int k){

        Map<Double, Integer> peaks = new TreeMap<>();
        int peak = min;
        for (int i=min; i<=max; i++){
            if (signal[i] > signal[peak]){
                peak = i;
            }
        }
        peaks.put(signal[peak], peak);
        if (n > 1){
            //if (peak - min > k)
            //    peaks.putAll(findPeaks(signal, min, peak - k, n - 2, k));
            peaks.putAll(findPeaks(signal, peak + k, max, n - 1, k));
        }


        return peaks;

    }

    private double[] lowPassFilter(double[] signal, int smoothing){
        double value = signal[0];
        for (int i=0; i<signal.length; i++){
            double curr = signal[i];
            value += (curr - value) / smoothing;
            signal[i] = value;
        }
        return signal;
    }

    interface TjunerUIListener {
        void onPitch(String s);
    }
}
