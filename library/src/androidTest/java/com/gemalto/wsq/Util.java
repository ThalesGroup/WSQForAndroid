package com.gemalto.wsq;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

public class Util {
    private Context ctx;

    public Util(final Context base) {
        this.ctx = base;
    }

    public void assertBitmapsEqual(Bitmap expected, Bitmap actual) {
        assertBitmapsEqual(null, expected, actual);
    }

    public void assertBitmapsEqual(String message, Bitmap expected, Bitmap actual) {
        /**
         * Android has an unfortunate tendency to load images into bitmaps with reduced precision. For example
         * 8-bit PNG is loaded with some colors off by 1 on my Nexus 5X, Android 8.1.1. On the other hand GIF,
         * 24-bit PNG and WEBP are fine.
         * If you get errors from this assertion, especially if the difference is 1, first check if this
         * might be the cause before you go hunting for bugs in the WSQ library.
         *
         * (Transparent bitmaps are even worse, the loss of precision can be even more severe there. Don't use this
         * for transparent bitmaps.)
         */
        assertEquals(message, expected.getWidth(), actual.getWidth());
        assertEquals(message, expected.getHeight(), actual.getHeight());
        int[] pixels1 = new int[expected.getWidth() * expected.getHeight()];
        int[] pixels2 = new int[actual.getWidth() * actual.getHeight()];
        expected.getPixels(pixels1, 0, expected.getWidth(), 0, 0, expected.getWidth(), expected.getHeight());
        actual.getPixels(pixels2, 0, actual.getWidth(), 0, 0, actual.getWidth(), actual.getHeight());
        for (int i = 0; i < pixels1.length; i++) {
            if (pixels1[i] != pixels2[i]) {
                fail((message != null ? message + "; " : "") + String.format("pixel %d different - expected %08X, got %08X", i, pixels1[i], pixels2[i]));
            }
        }
    }

    //only grayscale bitmaps supported
    public void assertBitmapsDifferenceMax(String message, Bitmap expected, Bitmap actual, int maxDiff) {
        assertEquals(message, expected.getWidth(), actual.getWidth());
        assertEquals(message, expected.getHeight(), actual.getHeight());
        int[] pixels1 = new int[expected.getWidth() * expected.getHeight()];
        int[] pixels2 = new int[actual.getWidth() * actual.getHeight()];
        expected.getPixels(pixels1, 0, expected.getWidth(), 0, 0, expected.getWidth(), expected.getHeight());
        actual.getPixels(pixels2, 0, actual.getWidth(), 0, 0, actual.getWidth(), actual.getHeight());
        for (int i = 0; i < pixels1.length; i++) {
            //check only the blue channel - we only check grayscale bitmaps, so the other channels will be the same
            if (Math.abs((pixels1[i] & 0xFF) - (pixels2[i] & 0xFF)) > maxDiff) {
                fail((message != null ? message + "; " : "") + String
                        .format("pixel %d too different - expected %08X, got %08X, max allowed difference is %d", i, pixels1[i], pixels2[i],
                                maxDiff));
            }
        }
    }

    public byte[] loadAssetFile(String name) throws Exception {
        try (InputStream is = ctx.getResources().getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(is.available());
            byte[] buffer = new byte[8192];
            int count;
            while ((count = is.read(buffer)) >= 0) {
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        }
    }

    public byte[] loadFile(String name) throws Exception {
        try (InputStream is = new FileInputStream(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(is.available());
            byte[] buffer = new byte[8192];
            int count;
            while ((count = is.read(buffer)) >= 0) {
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        }
    }

    public Bitmap loadAssetBitmap(String name) throws Exception {
        try (InputStream is = ctx.getResources().getAssets().open(name)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opts.inPremultiplied = false;
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            if (bmp.getConfig() != Bitmap.Config.ARGB_8888) {
                //convert to ARGB_8888 for pixel comparison purposes
                int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
                bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
                bmp = Bitmap.createBitmap(pixels, bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
            }
            return bmp;
        }
    }

    public Bitmap loadAssetRawBitmap(String name, int width, int height) throws Exception {
        //raw bitmaps are 8-bit
        byte[] data = null;
        try (InputStream is = ctx.getResources().getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(is.available());
            byte[] buffer = new byte[8192];
            int count;
            while ((count = is.read(buffer)) >= 0) {
                out.write(buffer, 0, count);
            }
            data = out.toByteArray();
        }

        int length = width * height;
        int[] pixels = new int[length];
        for (int i = 0; i < length; i++) {
            int c = data[i] & 0xFF;
            pixels[i] = 0xFF000000 | (c << 16) | (c << 8) | c;
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);

    }

    double meanSquareError(int[] pixels1, int[] pixels2) {
        long acc = 0;
        for (int i = 0; i < pixels1.length; i++) {
            //check only the lower byte - all the images are greyscale anyway
            int diff = (pixels1[i] & 0xFF) - (pixels2[i] & 0xFF);
            acc += diff * diff;
        }
        return acc * 1.0 / pixels1.length;
    }

    double psnr(int[] pixels1, int[] pixels2) {
        double mse = meanSquareError(pixels1, pixels2);
        return 20 * Math.log10(255) - 10 * Math.log10(mse);
    }

    double psnr(Bitmap bmp1, Bitmap bmp2) {
        int[] pixels1 = new int[bmp1.getWidth() * bmp1.getHeight()];
        int[] pixels2 = new int[bmp2.getWidth() * bmp2.getHeight()];
        bmp1.getPixels(pixels1, 0, bmp1.getWidth(), 0, 0, bmp1.getWidth(), bmp1.getHeight());
        bmp2.getPixels(pixels2, 0, bmp2.getWidth(), 0, 0, bmp2.getWidth(), bmp2.getHeight());
        return psnr(pixels1, pixels2);
    }
}
