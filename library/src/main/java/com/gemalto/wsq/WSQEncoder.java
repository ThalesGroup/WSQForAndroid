package com.gemalto.wsq;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class encodes bitmaps into WSQ file format. It uses the NBIS code produced by NIST. This code has some
 * peculiarities. For example it strictly refuses to create WSQ if the resulting file should be bigger than
 * the raw input image data (i.e. bigger than {@code image_width * image_height} bytes). Keep that in mind when
 * using the {@link #setBitrate(float)} or {@link #setComment(String)} methods.
 * @author mdvorak
 *
 */
public class WSQEncoder {
    private static final String TAG = "WSQEncoder";

    private static final int MAX_COMMENT_LENGTH = (1 << 16) - 3;

    /**
     * Unknown PPI value
     */
    public static final int UNKNOWN_PPI = -1;
    /**
     * Bitrate of 2.25 yields around 5:1 compression. This is the default value.
     */
    public static final float BITRATE_5_TO_1 = (float)2.25;
    /**
     * Bitrate of 0.75 yields around 15:1 compression.
     */
    public static final float BITRATE_15_TO_1 = (float)0.75;
    
    private Bitmap bmp;
    private float bitrate = BITRATE_5_TO_1;
    private int ppi = UNKNOWN_PPI;
    private String comment = null;

    public WSQEncoder(Bitmap bmp) {
        if (bmp == null) throw new IllegalArgumentException("Bitmap must not be null!");
        this.bmp = bmp;
    }

    /**
     * Set the bit rate. This influences the compression ratio. Technically you can use any positive number - higher bitrate means
     * higher quality and lower compression ratio. However, in practise you should use either {@link #BITRATE_5_TO_1},
     * or {@link #BITRATE_15_TO_1}. These values are specified and tested by NIST and they produce the expected results.
     * If you use other values, you might get weird results or no results at all.<br><br>
     *
     * Default value: {@link #BITRATE_5_TO_1}
     * @param bitrate the bit rate to use
     * @return this {@code WSQEncoder} instance
     */
    public WSQEncoder setBitrate(final float bitrate) {
        if (bitrate <= 0) throw new IllegalArgumentException("Bitrate must be a positive number");
        this.bitrate = bitrate;
        return this;
    }

    /**
     * Sets the image resolution (pixels per inch). Default value: {@link #UNKNOWN_PPI}.
     * @param ppi the image resolution to use
     * @return this {@code WSQEncoder} instance
     * @throws IllegalArgumentException if {@code ppi &lt; -1}
     */
    public WSQEncoder setPpi(final int ppi) {
        if (ppi < -1) throw new IllegalArgumentException("PPI must be positive or -1");
        this.ppi = ppi;
        return this;
    }

    /**
     * Sets a text comment that will be stored in the WSQ file. Maximum comment length is 65533 bytes.
     * (But keep in mind that the NBIS code will throw an error if the WSQ data length + comment length
     * should be longer than the original image uncompressed data length.)
     * @param comment the comment
     * @return this {@code WSQEncoder} instance
     */
    public WSQEncoder setComment(final String comment) {
        if (comment != null && comment.getBytes().length > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Maximum comment length is " + MAX_COMMENT_LENGTH + " bytes");
        }
        this.comment = comment;
        return this;
    }

    /**
     * Encode to WSQ, return the result as a byte array.
     * @return the WSQ-compressed bitmap, or null in case of compression error
     */
    public byte[] encode() {
        return encodeInternal();
    }

    /**
     * Encode to WSQ, write the result into an {@link OutputStream}.
     * @param out the stream into which the result will be written
     * @return the number of bytes written; 0 in case of a conversion error
     * @throws IOException if there's an error writing the result into the output stream
     */
    public int encode(OutputStream out) throws IOException {
        byte[] data = encodeInternal();
        if (data == null) return 0;
        out.write(data);
        return data.length;
    }

    /**
     * Encode to WSQ, store the result into a file.
     * @param fileName the name of the output file
     * @return {@code true} if the image was successfully converted and stored; {@code false} otherwise
     */
    public boolean encode(String fileName) {
        byte[] data = encodeInternal();
        if (data == null || data.length == 0) return false;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(fileName);
            out.write(data);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing WSQ into " + fileName, e);
            return false;
        } finally {
            try {if (out != null) out.close();} catch (IOException ignored){}
        }

    }

    private byte[] encodeInternal() {
        if (bmp == null) return null;
        int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        return Native.encodeWSQByteArray(pixels, bmp.getWidth(), bmp.getHeight(), bitrate, ppi, comment);
    }
}
