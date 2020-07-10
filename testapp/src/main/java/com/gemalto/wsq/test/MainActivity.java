package com.gemalto.wsq.test;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.gemalto.wsq.WSQDecoder;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new AsyncTask<Void, Void, Bitmap>(){
            @Override
            protected Bitmap doInBackground(final Void... voids) {
                Bitmap ret = null;
                InputStream in = null;
                ByteArrayOutputStream out = null;
                try {
                    in = getAssets().open("test.wsq");
                    out = new ByteArrayOutputStream(in.available());
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) >= 0) {
                        out.write(buffer, 0, count);
                    }
                    ret = WSQDecoder.decode(out.toByteArray()).getBitmap();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(in);
                }
                return ret;
            }

            @Override
            protected void onPostExecute(final Bitmap bitmap) {
                if (bitmap != null) {
                    ((ImageView)findViewById(R.id.image)).setImageBitmap(bitmap);
                }
            }
        }.execute();
    }

    private void close(Closeable obj) {
        try {
            if (obj != null) obj.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
