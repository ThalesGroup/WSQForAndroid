package com.gemalto.wsq.test;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gemalto.wsq.WSQDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //support the edge-to-edge mode, which is mandatory since api 35. We only do this
            //on api 21 and above; it doesn't work well on api 16, not sure about the versions
            //between them.
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //decode the WSQ in a background thread
        executorService.execute(() -> {
            try (InputStream in = getAssets().open("test.wsq")) {
                WSQDecoder.WSQDecodedImage result = WSQDecoder.decode(in);

                //show the result on the main thread
                mainHandler.post(() -> {
                    if (result != null) {
                        ((ImageView) findViewById(R.id.image)).setImageBitmap(result.getBitmap());
                    } else {
                        Toast.makeText(MainActivity.this, "error decoding image", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Error decoding wsq image", e);
            }
        });
    }
}
