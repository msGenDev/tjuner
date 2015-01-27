package si.fri.liis.tjuner;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

public class MainActivityWear extends Activity {

    private TextView mTextView;

    private final static String LOG_TAG = "tune loop";

    // audio input parameters
    private final static int RATE = 8000;
    private final static int CHANEL_MODE = AudioFormat.CHANNEL_IN_MONO;
    private final static int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    // audio input
    private AudioRecord input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity_wear);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
    }
}
