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
     * Decode a WSQ-encoded file into a bitmap. If the specified file name is null,
     * or cannot be decoded into a bitmap, the function returns null.
     * @param filename complete path name for the file to be decoded.
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap decode(String filename) {
        int[] res = Native.decodeWSQFile(filename);
        return nativeToBitmap(res);
    }
    
    /**
     * Decode a WSQ image from a byte array into a bitmap. If the byte array cannot
     * be decoded into a bitmap, the function returns null.
     * @param data WSQ-encoded data
     * @return The decoded bitmap, or null if the image data could not be decoded.
     */
    public static Bitmap decode(byte[] data) {
        int[] res = Native.decodeWSQByteArray(data);
        return nativeToBitmap(res);
    }

    /**
     * Reads all data from an {@link InputStream} and tries to decode it as WSQ.
     * @param in an input stream containing WSQ-encoded data. <strong>Warning: all available data from the stream will be read! The end of the WSQ data will not be detected!</strong>
     * @return The decoded bitmap, or {@code null} if the image data could not be decoded.
     */
    public static Bitmap decode(InputStream in) {
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
    private static Bitmap nativeToBitmap(int[] data) {
        if (data == null || data.length < 2) return null;
        int width = data[0];
        int height = data[1];
        Bitmap bmp = Bitmap.createBitmap(data, 2, width, width, height, Config.ARGB_8888);
        bmp.setHasAlpha(false);
        return bmp;
    }

}
