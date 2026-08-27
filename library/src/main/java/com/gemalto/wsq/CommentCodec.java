package com.gemalto.wsq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
    This is a private class used for writing and reading comments to and from WSQ files.
    There is code to do that inside NBIS, but it can't handle comments with byte 0x00 properly.
    I consider this to be a bug because there is nothing in the WSQ specification that would
    forbid the inclusion binary data in the comments (at least I wasn't able to find anything).

    Also, the file format is so simple that it was almost easier to write my own Java code
    than figuring out the NBIS C functions extending the JNI interface:
     - the file starts with the SOI_WSQ marker (0xFFA0) - 2 bytes
     - then there are the comment blocks: each one consists of:
       - the COM_WSQ marker (0xFFA8) - 2 bytes
       - the comment length - 2 bytes (the length of the comment plus the two bytes of the length
         themselves, i.e. the length of an empty comment is 2)
       - the comment itself (byte array)
     - the SOB_WSQ marker indicates the start of image data; this is where our processing ends -
       no further comments exist after this marker
 */
class CommentCodec {
    private static final short SOI_WSQ = (short) 0xFFA0;
    private static final short SOB_WSQ = (short) 0xFFA3;
    private static final short COM_WSQ = (short) 0xFFA8;

    CommentCodec() {
    }

    List<byte[]> readComments(byte[] wsqData) throws CommentCodecException {
        List<byte[]> ret = new ArrayList<>();
        int offset = 0;
        short marker = getShort(wsqData, offset);
        if (marker != SOI_WSQ) throw new CommentCodecException("start-of-image marker not found");
        offset += 2;
        do {
            marker = getShort(wsqData, offset);

            if (marker == SOB_WSQ) break;

            offset += 2;
            int length = (getShort(wsqData, offset) & 0xFFFF) - 2; //the length value includes the two bytes used to encode the length itself
            offset += 2;

            if (marker == COM_WSQ) {
                //comment found
                ret.add(Arrays.copyOfRange(wsqData, offset, offset + length));
            }
            offset += length;
        } while (offset < wsqData.length);
        return ret;
    }

    byte[] writeComments(byte[] wsqData, List<byte[]> comments) throws CommentCodecException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        short marker = getShort(wsqData, offset);
        if (marker != SOI_WSQ) throw new CommentCodecException("start-of-image marker not found");
        offset += 2;
        //skip existing comments
        do {
            marker = getShort(wsqData, offset);

            if (marker != COM_WSQ) break;

            offset += 2;
            int length = (getShort(wsqData, offset) & 0xFFFF);
            offset += length;
        } while (offset < wsqData.length);

        if (offset >= wsqData.length) throw new CommentCodecException("incorrect wsq data format");

        out.write(wsqData, 0, offset);

        for (byte[] comment : comments) {
            try {
                putShort(out, COM_WSQ);
                putShort(out, (short) (comment.length + 2)); //the length value includes the two bytes used to encode the length
                out.write(comment);
            } catch (IOException e) {
                throw new CommentCodecException("error writing comment", e);
            }
        }

        out.write(wsqData, offset, wsqData.length - offset);

        return out.toByteArray();
    }

    private short getShort(byte[] data, int offset) throws CommentCodecException {
        try {
            return (short) (((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new CommentCodecException("error reading marker", e);
        }
    }

    private void putShort(OutputStream out, short value) throws IOException {
        out.write(value >> 8);
        out.write(value);
    }

    static class CommentCodecException extends Exception {
        public CommentCodecException(String message) {
            super(message);
        }

        public CommentCodecException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
