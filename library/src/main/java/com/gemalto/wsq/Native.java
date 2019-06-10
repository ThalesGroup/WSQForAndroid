package com.gemalto.wsq;

class Native {

    static {
        System.loadLibrary("wsq");
    }

    //NBIS code uses global variables and crashes if it's accessed from multiple threads.
    //That's why the methods are synchronized.
    static synchronized native int[] decodeWSQFile(String filename);
    static synchronized native int[] decodeWSQByteArray(byte[] data);
    static synchronized native byte[] encodeWSQByteArray(int[] pixels, int width, int height, float r_bitrate, int ppi, String comment);
}
