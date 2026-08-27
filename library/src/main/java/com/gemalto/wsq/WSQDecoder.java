package com.gemalto.wsq;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class decodes WSQ files into a bitmap.
 */
public class WSQDecoder {
    private static final String TAG = "WSQDecoder";

    /**
     * The output of the WSQ decoding process. Contains the decoded bitmap and the pixels-per-inch density information.
     */
    public static class WSQDecodedImage {
        private final Bitmap bitmap;
        private final int ppi;
        private final List<byte[]> comments;

        private WSQDecodedImage(Bitmap bitmap, int ppi) {
            this.bitmap = bitmap;
            this.ppi = ppi;
            this.comments = new ArrayList<>();
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

        public List<byte[]> getComments() {
            return comments;
        }
    }

    /**
     * Decode a WSQ-encoded file. If the specified file name is null,
     * or cannot be decoded, the function returns null.
     * @param filename complete path name for the file to be decoded.
     * @return The decoded image, or null if the image data could not be decoded.
     */
    public static WSQDecodedImage decode(String filename) {
        try {
            if (filename != null) return decode(new FileInputStream(filename));
        } catch (FileNotFoundException e) {
            //do nothing, we just return null
        }
        return null;
    }
    
    /**
     * Decode a WSQ image from a byte array. If the byte array cannot
     * be decoded, the function returns null.
     * @param data WSQ-encoded data
     * @return The decoded image, or null if the image data could not be decoded.
     */
    public static WSQDecodedImage decode(byte[] data) {
        int[] res = Native.decodeWSQByteArray(data);
        WSQDecodedImage ret = nativeToImageData(res);
        if (ret != null) {
            try {
                ret.comments.addAll(new CommentCodec().readComments(data));
            } catch (CommentCodec.CommentCodecException e) {
                Log.e(TAG, "error loading comments from wsq data", e);
            }
        }
        return ret;
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
            Log.e(TAG, "Error reading WSQ data", e);
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
