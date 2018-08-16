package com.excellence.exec.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.excellence.exec.Command.CommandTask;
import com.excellence.exec.Commander;
import com.excellence.exec.IListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Commander.init();
        final CommandTask task = Commander.addTask("ls", new IListener() {
            @Override
            public void onPre(String command) {
                Log.i(TAG, "onPre: " + command);
            }

            @Override
            public void onProgress(String message) {
                Log.i(TAG, "onProgress: " + message);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onSuccess(String message) {
                Log.i(TAG, "onSuccess: " + message);
            }
        });
        // task.discard();
    }
}
