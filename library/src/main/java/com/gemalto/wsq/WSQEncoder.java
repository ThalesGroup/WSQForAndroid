package com.gemalto.wsq;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class encodes bitmaps into WSQ file format. It uses the NBIS code produced by NIST. This code has some
 * peculiarities. For example, it strictly refuses to create WSQ if the resulting file should be bigger than
 * the raw input image data (i.e. bigger than {@code image_width * image_height} bytes). Keep that in mind when
 * using the {@link #setBitrate(float)} method.<br/><br/>
 *
 * The NBIS encoder adds a special "NISTCOM" comment to the resulting file encoding the image width,
 * height, PPI and some other image attributes as described for example in
 * <a href="https://www.nist.gov/system/files/documents/srd/Spec-db-14.pdf">this file</a>. It's not
 * a mandatory part of a WSQ image, but it doesn't seem to hurt anything, so I leave it there.
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
    
    private final Bitmap bmp;
    private float bitrate = BITRATE_5_TO_1;
    private int ppi = UNKNOWN_PPI;

    private final List<byte[]> comments = new ArrayList<>();

    public WSQEncoder(Bitmap bmp) {
        if (bmp == null) throw new IllegalArgumentException("Bitmap must not be null!");
        this.bmp = bmp;
    }

    /**
     * Set the bit rate. This influences the compression ratio. Technically you can use any positive number - higher bitrate means
     * higher quality and lower compression ratio. However, in practise you should use either {@link #BITRATE_5_TO_1},
     * or {@link #BITRATE_15_TO_1}. These values are specified and tested by NIST, and they produce the expected results.
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
     * Adds a comment that will be stored in the WSQ file. Maximum comment length is 65533 bytes.
     * <br/><br/>
     * The encoded file will always contain a special "NISTCOM" comment containing various attributes
     * of the image in text form. This method lets you add comments in addition to the NISTCOM.
     * <br/><br/>
     * Keep in mind that while you can technically store arbitrary data, the reference NBIS code
     * will break on comments containing the {@code 0x00} byte as it assumes a null-terminated string.
     * It can be assumed that a lot of code based on the NBIS implementation might break as well.
     * @param comment the comment
     * @return this {@code WSQEncoder} instance
     */
    public WSQEncoder addComment(final byte[] comment) {
        if (comment == null) {
            throw new IllegalArgumentException("Comment cannot be null");
        }
        if (comment.length > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Maximum comment length is " + MAX_COMMENT_LENGTH + " bytes");
        }
        this.comments.add(Arrays.copyOf(comment, comment.length));
        return this;
    }

    /**
     * A convenience function which converts the input String to its UTF-8 byte array representation
     * and adds it to comments.
     * @param comment the String comment
     * @return this {@code WSQEncoder} instance
     */
    public WSQEncoder addComment(final String comment) {
        return this.addComment(comment.getBytes());
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
        try (FileOutputStream out = new FileOutputStream(fileName)) {
            out.write(data);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing WSQ into " + fileName, e);
            return false;
        }

    }

    private byte[] encodeInternal() {
        int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
        byte[] wsqData = Native.encodeWSQByteArray(pixels, bmp.getWidth(), bmp.getHeight(), bitrate, ppi);
        if (wsqData != null && !comments.isEmpty()) {
            try {
                wsqData = new CommentCodec().writeComments(wsqData, comments);
            } catch (CommentCodec.CommentCodecException e) {
                Log.e(TAG, "Error encoding comments to the file", e);
                return null;
            }
        }
        return wsqData;
    }
}
