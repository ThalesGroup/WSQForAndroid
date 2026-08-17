package com.gemalto.wsq;

import android.content.Context;
import android.graphics.Bitmap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import static junit.framework.Assert.assertNull;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class TestWSQDecoder {
    // Context of the app under test.
    private Context ctx;
    private Util util;

    @Before
    public void init() {
        ctx = InstrumentationRegistry.getTargetContext();
        util = new Util(ctx);
    }

    /*
      Decode several images, compare them with the expected results.
     */
    @Test
    public void testDecodeSimple() throws Exception {
        String[] wsqFiles = new String[] {"lena1.wsq", "lena2.wsq", "256x256.wsq", "1024x1024.wsq"};
        String[] expectedFiles = new String[] {"lena1.png", "lena2.png", "256x256.png", "1024x1024.png"};


        for (int i = 0; i < wsqFiles.length; i++) {
            Bitmap expected = util.loadAssetBitmap(expectedFiles[i]);

            //test decode from file
            File outFile = new File(ctx.getFilesDir(), "tmp.tmp");
            FileOutputStream out = new FileOutputStream(outFile);
            out.write(util.loadAssetFile(wsqFiles[i]));
            out.close();

            WSQDecoder.WSQDecodedImage decoded = WSQDecoder.decode(outFile.getPath());

            util.assertBitmapsEqual("decoded " + wsqFiles[i] + " is different from " + expectedFiles[i], decoded.getBitmap(), expected);
            outFile.delete();

            //test decode from stream
            try (InputStream in = ctx.getAssets().open(wsqFiles[i])) {
                decoded = WSQDecoder.decode(in);
                util.assertBitmapsEqual("decoded " + wsqFiles[i] + " is different from " + expectedFiles[i], decoded.getBitmap(), expected);
            }

            //test decode from byte array
            byte[] data = util.loadAssetFile(wsqFiles[i]);
            decoded = WSQDecoder.decode(data);
            util.assertBitmapsEqual("decoded " + wsqFiles[i] + " is different from " + expectedFiles[i], decoded.getBitmap(), expected);
        }
    }

    /*
      Test decoder wrong input.
     */
    @Test
    public void testDecodeError() throws Exception {
        assertNull(WSQDecoder.decode(util.loadAssetFile("lena1.png")));
        assertNull(WSQDecoder.decode((byte[])null));
        assertNull(WSQDecoder.decode(new byte[0]));
        assertNull(WSQDecoder.decode(new byte[1]));
        assertNull(WSQDecoder.decode(new byte[2]));
        assertNull(WSQDecoder.decode(new byte[16000000]));
        assertNull(WSQDecoder.decode((InputStream)null));
        assertNull(WSQDecoder.decode((String)null));

        //decode from wrong file
        File outFile = new File(ctx.getFilesDir(), "tmp.tmp");
        FileOutputStream out = new FileOutputStream(outFile);
        out.write(util.loadAssetFile("lena1.png"));
        out.close();
        assertNull(WSQDecoder.decode(outFile.getPath()));
        outFile.delete();
        //decode from non-existant file
        assertNull(WSQDecoder.decode(outFile.getPath()));
    }

    @Test
    public void testDecodeMultithreaded() throws Throwable {
        //test decoding in multiple (4) threads.
        //We load 4 different images, decode them repeatedly in 4 threads and check that we
        //always get the expected result.
        DecoderThread t1 = new DecoderThread("lena1.wsq", "lena1.png");
        DecoderThread t2 = new DecoderThread("lena2.wsq", "lena2.png");
        DecoderThread t3 = new DecoderThread("256x256.wsq", "256x256.png");
        DecoderThread t4 = new DecoderThread("1024x1024.wsq", "1024x1024.png");

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        while (!t1.finished || !t2.finished || !t3.finished || !t4.finished) {
            t1.checkError();
            t2.checkError();
            t3.checkError();
            t4.checkError();
            try {Thread.sleep(500);}catch (InterruptedException ignored){}
        }
        t1.checkError();
        t2.checkError();
        t3.checkError();
        t4.checkError();
    }

    class DecoderThread extends Thread {
        private static final int REPEATS = 5;
        String pngFile;
        String wsqFile;
        boolean finished = false;
        Throwable error = null;
        Bitmap expected;
        byte[] encoded;

        DecoderThread(final String wsqFile, final String pngFile) throws Exception {
            this.pngFile = pngFile;
            this.wsqFile = wsqFile;
            expected = util.loadAssetBitmap(pngFile);
            encoded = util.loadAssetFile(wsqFile);
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < REPEATS; i++) {
                    //test byte array
                    WSQDecoder.WSQDecodedImage decoded = WSQDecoder.decode(encoded);
                    util.assertBitmapsEqual("decoded " + wsqFile + " is different from " + pngFile, expected, decoded.getBitmap());

                    //test decode from file
                    File outFile = File.createTempFile("testjp2", "tmp", ctx.getFilesDir());
                    FileOutputStream out = new FileOutputStream(outFile);
                    out.write(encoded);
                    out.close();

                    decoded = WSQDecoder.decode(outFile.getPath());
                    util.assertBitmapsEqual("decoded " + wsqFile + " is different from " + pngFile, expected, decoded.getBitmap());
                    outFile.delete();

                    //test decode from stream
                    try (InputStream in = ctx.getAssets().open(wsqFile)) {
                        decoded = WSQDecoder.decode(in);
                        util.assertBitmapsEqual("decoded " + wsqFile + " is different from " + pngFile, expected, decoded.getBitmap());
                    }
                }
            } catch (Throwable e) {
                error = e;
            }
            finished = true;
        }

        void checkError() throws Throwable {
            if (error != null) throw error;
        }
    }
}
