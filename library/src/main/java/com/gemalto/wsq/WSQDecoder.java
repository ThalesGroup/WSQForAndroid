package com.gemalto.wsq;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class decodes WSQ files into a bitmap.
 * @author mdvorak
 *
 */
public class WSQDecoder {
    /**
     * The output of the WSQ decoding process. Contains the decoded bitmap and the pixels-per-inch density information.
     */
    public static class WSQDecodedImage {
        private Bitmap bitmap;
        private int ppi;

        private WSQDecodedImage(Bitmap bitmap, int ppi) {
            this.bitmap = bitmap;
            this.ppi = ppi;
        }

        /**
         * @return the decoded fingerprint image
         */
        public Bitmap getBitmap() {
            return bitmap;
        }

        /**
         * @return image density (pixels per inch)
         */
        public int getPpi() {
            return ppi;
        }
    }

    /**
     * Decode a WSQ-encoded file. If the specified file name is null,
     * or cannot be decoded, the function returns null.
     * @param filename complete path name for the file to be decoded.
     * @return The decoded image, or null if the image data could not be decoded.
     */
    public static WSQDecodedImage decode(String filename) {
        int[] res = Native.decodeWSQFile(filename);
        return nativeToImageData(res);
    }
    
    /**
     * Decode a WSQ image from a byte array. If the byte array cannot
     * be decoded, the function returns null.
     * @param data WSQ-encoded data
     * @return The decoded image, or null if the image data could not be decoded.
     */
    public static WSQDecodedImage decode(byte[] data) {
        int[] res = Native.decodeWSQByteArray(data);
        return nativeToImageData(res);
    }

    /**
     * Reads all data from an {@link InputStream} and tries to decode it as WSQ.
     * @param in an input stream containing WSQ-encoded data. <strong>Warning: all available data from the stream will be read! The end of the WSQ data will not be detected!</strong>
     * @return The decoded image, or {@code null} if the image data could not be decoded.
     */
    public static WSQDecodedImage decode(InputStream in) {
        if (in == null) return null;
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream(in.available());
            byte[] buffer = new byte[16 * 1024];
            int bytesRead = in.read(buffer);
            while (bytesRead >= 0) {
                out.write(buffer, 0, bytesRead);
                bytesRead = in.read(buffer);
            }
            return decode(out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
        Get the decoded data from the native code and create a Bitmap object.
     */
    private static WSQDecodedImage nativeToImageData(int[] data) {
        if (data == null || data.length < 3) return null;
        int width = data[0];
        int height = data[1];
        int ppi = data[2];
        Bitmap bmp = Bitmap.createBitmap(data, 3, width, width, height, Config.ARGB_8888);
        bmp.setHasAlpha(false);
        return new WSQDecodedImage(bmp, ppi);
    }

}
