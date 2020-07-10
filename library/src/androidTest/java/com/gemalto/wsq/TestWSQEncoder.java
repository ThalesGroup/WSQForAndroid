package com.gemalto.wsq;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TestWSQEncoder {
    // Context of the app under test.
    private Context ctx;
    private Util util;

    private static final int maxCommentLength = (1 << 16) - 3;

    @Before
    public void init() {
        ctx = InstrumentationRegistry.getTargetContext();
        util = new Util(ctx);
    }

    /*
      Encode several normal images, decode them, compare them to the originals.
     */
    @Test
    public void testEncodeSimple() throws Exception {
        String[] images = new String[] {"lena1.png", "lena2.png", "256x256.png", "1024x1024.png"};

        for (int i = 0; i < images.length; i++) {
            Bitmap expected = util.loadAssetBitmap(images[i]);

            //test encode to file
            File outFile = new File(ctx.getFilesDir(), "tmp.tmp");
            assertTrue(new WSQEncoder(expected).encode(outFile.getPath()));
            byte[] encoded = util.loadFile(outFile.getPath());
            assertNotNull(encoded);
            Bitmap decoded = WSQDecoder.decode(encoded).getBitmap();
            assertFalse(decoded.hasAlpha());
            assertSimilar(String.format("encoded %s is too different from the original", images[i]), expected, decoded);
            outFile.delete();

            //test encode to stream
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertTrue(new WSQEncoder(expected).encode(out) > 0);
            encoded = out.toByteArray();
            assertNotNull(encoded);
            decoded = WSQDecoder.decode(encoded).getBitmap();
            assertFalse(decoded.hasAlpha());
            assertSimilar(String.format("encoded %s is too different from the original", images[i]), expected, decoded);

            //test encode to byte array
            encoded = new WSQEncoder(expected).encode();
            assertNotNull(encoded);
            decoded = WSQDecoder.decode(encoded).getBitmap();
            assertFalse(decoded.hasAlpha());
            assertSimilar(String.format("encoded %s is too different from the original", images[i]), expected, decoded);
        }
    }

    /*
      Encode an image with several PPIs, decode them, check PPI.
     */
    @Test
    public void testEncodePpi() throws Exception {
        int[] ppis = new int[] {-1, 500, 200, 0, 65536, Integer.MAX_VALUE};

        Bitmap bmp = util.loadAssetBitmap("lena1.png");
        for (int i = 0; i < ppis.length; i++) {

            //test encode to byte array
            byte[] encoded = new WSQEncoder(bmp).setPpi(ppis[i]).encode();
            assertNotNull(encoded);
            WSQDecoder.WSQDecodedImage decoded = WSQDecoder.decode(encoded);
            assertEquals(String.format("Wrong PPI! Expected %d, got %d", ppis[i], decoded.getPpi()), ppis[i], decoded.getPpi());
            assertSimilar("encoded image is too different from the original", bmp, decoded.getBitmap());
        }

        //test invalid ppis
        ppis = new int[] {-2, -10, -150, Integer.MIN_VALUE};
        for (int ppi : ppis) {
            try {
                byte[] encoded = new WSQEncoder(bmp).setPpi(ppi).encode();
                fail("PPI " + ppi + " should be rejected!");
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static final double SIMILAR_PSNR_THRESHOLD = 30;

    private void assertSimilar(String message, Bitmap expected, Bitmap actual) {
        if (expected == null && actual == null) return;
        if (expected == null) fail(message + "; expected image is not null, actual image is null");
        if (actual == null) fail(message + "; expected image is null, actual image is not null");

        assertEquals(message + "; bitmaps have different width", expected.getWidth(), actual.getWidth());
        assertEquals(message + "; bitmaps have different height", expected.getHeight(), actual.getHeight());

        double psnr = util.psnr(expected, actual);
        if (psnr < SIMILAR_PSNR_THRESHOLD) {
            fail(String.format("%s; PSNR %f is under the threshold %f", message, psnr, SIMILAR_PSNR_THRESHOLD));
        }
    }

    @Test
    public void testBitrate() throws Exception {
        Bitmap orig = util.loadAssetBitmap("256x256.png");
        //use a few normal bitrates in increasing order, check that the file size is increasing
        int lastSize = Integer.MIN_VALUE;
        float lastBitrate = 0;
        for (float bitrate : new float[]{0.1f, 0.75f, 1f, 2.25f, 5.3f, 7f}) {
            byte[] encoded = new WSQEncoder(orig).setBitrate(bitrate).encode();
            assertTrue(String.format("bitrate %f should produces larger size (%d bytes) than bitrate %f (%d bytes)", bitrate, encoded.length, lastBitrate, lastSize),
                        encoded.length > lastSize);
            lastSize = encoded.length;
            lastBitrate = bitrate;
        }
    }

    private static final String asciiChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789`~!@#$%^&*()_+-=/.,<>?;'\"[]{}";
    private static final String specialChars = "ěščřžýáíéóůďťňテカキクケコ\t\n普通话ХѠЦЧШЩЪقصفعسن";

    @Test
    public void testComment() throws Exception {
        Bitmap orig = util.loadAssetBitmap("1024x1024.png");
        //ascii characters - test minimum, normal and maximum sizes
        for (int commentLength : new int[]{0, 1, 50, maxCommentLength - 1, maxCommentLength}) {
            StringBuilder str = new StringBuilder();
            while (str.length() < commentLength) str.append(asciiChars);
            String comment = commentLength == 0 ? "" : str.substring(0, commentLength);

            byte[] encoded = new WSQEncoder(orig).setBitrate(0.01f).setComment(comment).encode();
            assertNotNull("error encoding image with comment (length \" + commentLength + \" bytes)", encoded);
            assertTrue("comment (length " + commentLength + " bytes) not found in wsq data", findWsqComment(encoded, comment));
        }

        //special characters
        byte[] encoded = new WSQEncoder(orig).setBitrate(0.01f).setComment(specialChars).encode();
        assertNotNull("error encoding image with comment with special characters (length \" + commentLength + \" bytes)", encoded);
        assertTrue("comment with special characters not found in wsq data", findWsqComment(encoded, specialChars));
    }

    //look for comment header + data in the wsq
    private boolean findWsqComment(byte[] data, String comment) {
        //the comment block looks like FFA8 <length> <comment data>
        //<length> is coded on two bytes and these two bytes are included in the length (i.e. length of an empty comment is 2)
        byte[] stringData = comment.getBytes();
        byte[] stringHeader = new byte[4];
        stringHeader[0] = (byte)0xFF;
        stringHeader[1] = (byte)0xA8;
        stringHeader[2] = (byte)((stringData.length + 2) >> 8);
        stringHeader[3] = (byte)(stringData.length + 2);

        for (int i = 0; i <= data.length - stringData.length - stringHeader.length; i++) {
            boolean headerFound = true;
            for (int j = 0; j < stringHeader.length && headerFound; j++) {
                if (data[i + j] != stringHeader[j]) headerFound = false;
            }
            if (!headerFound) continue;
            boolean dataFound = true;
            for (int j = 0; j < stringData.length && dataFound; j++) {
                if (data[i + stringHeader.length + j] != stringData[j]) dataFound = false;
            }
            if (dataFound) {
                return true;
            }
        }
        return false;
    }


    /*
        Check that an exception is thrown when bad parameters are used.
     */
    @Test
    public void testEncodeParamsErrors() throws Exception {
        try {
            //null bitmap
            new WSQEncoder(null);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {}

        Bitmap expected = util.loadAssetBitmap("lena1.png");

        for (float bitrate : new float[]{0, -1, -1 * Float.MAX_VALUE,  -1 * Float.MIN_VALUE})
        try {
            //undefined output format
            new WSQEncoder(expected).setBitrate(bitrate);
            fail("Exception should have been thrown for invalid bitrate " + bitrate);
        } catch (IllegalArgumentException ignored) {}

        //too long comment - ascii characters
        try {
            byte[] data = new byte[maxCommentLength + 1];
            Arrays.fill(data, (byte)'A');
            new WSQEncoder(expected).setComment(new String(data));
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {}

        //too long comment - non-ascii characters
        try {
            char[] data = new char[(maxCommentLength / 2) + 1];
            Arrays.fill(data, 'ě');
            new WSQEncoder(expected).setComment(new String(data));
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {}

        //too long comment - maximum comment length in characters, but the last character is non-ascii
        try {
            char[] data = new char[maxCommentLength];
            Arrays.fill(data, 'A');
            data[data.length - 1] = 'š';
            new WSQEncoder(expected).setComment(new String(data));
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException ignored) {}
    }


    @Test
    public void testEncodeMultithreaded() throws Throwable {
        //test encoding in multiple (4) threads.
        //We load 4 different images, encode them repeatedly in 4 threads and check that we
        //always get the expected result.
        EncoderThread t1 = new EncoderThread("lena1.png");
        EncoderThread t2 = new EncoderThread("lena2.png");
        EncoderThread t3 = new EncoderThread("256x256.png");
        EncoderThread t4 = new EncoderThread("1024x1024.png");

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

    class EncoderThread extends Thread {
        private static final int REPEATS = 5;
        String pngFile;
        boolean finished = false;
        Throwable error = null;
        Bitmap expected;

        EncoderThread(final String pngFile) throws Exception {
            this.pngFile = pngFile;
            expected = util.loadAssetBitmap(pngFile);
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < REPEATS; i++) {
                    //test byte array
                    byte[] encoded = new WSQEncoder(expected).encode();
                    Bitmap decoded = WSQDecoder.decode(encoded).getBitmap();
                    assertSimilar(String.format("encoded %s is too different from the original", pngFile), expected, decoded);

                    //test encode to file
                    File outFile = File.createTempFile("testwsq", "tmp", ctx.getFilesDir());
                    assertTrue(new WSQEncoder(expected).encode(outFile.getPath()));
                    encoded = util.loadFile(outFile.getPath());
                    decoded = WSQDecoder.decode(encoded).getBitmap();
                    assertSimilar(String.format("encoded %s is too different from the original", pngFile), expected, decoded);
                    outFile.delete();

                    //test encode into stream
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    assertTrue(new WSQEncoder(expected).encode(out) > 0);
                    decoded = WSQDecoder.decode(out.toByteArray()).getBitmap();
                    assertSimilar(String.format("encoded %s is too different from the original", pngFile), expected, decoded);
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
