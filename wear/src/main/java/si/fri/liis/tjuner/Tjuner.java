package si.fri.liis.tjuner;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Log;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * Created by Jan on 27-Jan-15.
 */
public class Tjuner {

    private final static String LOG_TAG = "Tjuner";

    // audio input parameters
    private final int SAMPLE_RATE = 8000;
    private final int CHANEL_MODE = AudioFormat.CHANNEL_IN_MONO;
    private final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    // audio input
    private AudioRecord input;

    // fft window size
    private final int WINDOW_SIZE_IN_SAMPLES = 8192;
    private final int WINDOW_SIZE_IN_BYTES = 2 * WINDOW_SIZE_IN_SAMPLES;
    private final int WINDOW_SIZE_IN_MS = 1000 * WINDOW_SIZE_IN_SAMPLES / SAMPLE_RATE; // 1000 ms
    private final int WINDOW_SIZE_IN_SHORTS = SAMPLE_RATE * WINDOW_SIZE_IN_MS / 1000;

    // min and max tuning frequencies
    private final int MIN_TUNING_FREQUENCY = 100;
    private final int MAX_TUNING_FREQUENCY = 1000;

    // pitch frequency
    private int pitch;

    // control tuning loop
    private boolean doTune;

    public Tjuner(){
        this.doTune = false;
        this.pitch = 0;
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


                    // find maximum frequency
                    pitch = 0;
                    for (int i=MIN_TUNING_FREQUENCY; i<=MAX_TUNING_FREQUENCY; i++){
                        if (fft_signal[i] > fft_signal[pitch]){
                            pitch = i;
                        }
                    }

                    Log.e(LOG_TAG, "Pitch is " + pitch);

                }

                if (input != null){
                    input.stop();
                    input.release();
                }

                return null;
            }
        }.execute();
    }
}
