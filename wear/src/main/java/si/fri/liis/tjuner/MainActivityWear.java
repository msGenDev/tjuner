package si.fri.liis.tjuner;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

public class MainActivityWear extends Activity {

    private TextView mTextView;
    private Tjuner tjuner;
    private Handler handler;

    Tjuner.TjunerUIListener listener = new Tjuner.TjunerUIListener(){
        @Override
        public void onPitch(String s){
            //mTextView.setText(s);

            displayData(s);

            Log.e("MAIN", s);
        }
    };

    private void displayData(final String s){
        handler.post(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(s);
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
                mTextView = (TextView) stub.findViewById(R.id.text);



            }
        });

        //mTextView.setText("JAN");






    }

}
