package si.fri.liis.tjuner;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

public class MainActivityWear extends Activity {

    private TextView mTextViewNote;
    private TextView mTextViewDiff;
    private Tjuner tjuner;
    private Handler handler;

    Tjuner.TjunerUIListener listener = new Tjuner.TjunerUIListener(){
        @Override
        public void onPitch(String s){
            displayData(s);
        }
    };

    private void displayData(final String s){
        handler.post(new Runnable() {
            @Override
            public void run() {
                mTextViewNote.setText(s);
                mTextViewDiff.setText("JAN");
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity_wear);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                handler = new Handler(Looper.getMainLooper());

                tjuner = new Tjuner(listener);
                tjuner.tune();

                mTextViewNote = (TextView) stub.findViewById(R.id.textViewNote);
                mTextViewDiff = (TextView) stub.findViewById(R.id.textViewDiff);



            }
        });

    }

}
