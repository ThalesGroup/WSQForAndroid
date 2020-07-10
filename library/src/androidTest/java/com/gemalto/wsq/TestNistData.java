package com.gemalto.wsq;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/*
 * Here we check the encoder and decoder against NIST reference data downloaded from
 * http://nigos.nist.gov:8080/wsq/reference_images_v2.0_raw.tar. (The reference RAW images
 * were encoded to lossless WEBP to save space.)
 */
@RunWith(AndroidJUnit4.class)
public class TestNistData {
    // Context of the app under test.
    private Context ctx;
    private Util util;

    @Before
    public void init() {
        ctx = InstrumentationRegistry.getTargetContext();
        util = new Util(ctx);
    }

    @Test
    public void testDecode() throws Exception {
        List<String> wsqFiles = new ArrayList<>();
        for (String prefix : new String[]{"nist/wsq/225", "nist/wsq/75", "nist/wsq/not_7_9"}) {
            String[] files = ctx.getAssets().list(prefix);
            for (String file : files) {
                wsqFiles.add(prefix + '/' + file);
            }
        }

        for (String wsqFile : wsqFiles) {
            String expectedFile = wsqFile.replace("nist/wsq", "nist/wsq_decoded").replace(".wsq", ".webp");
            Bitmap expected = util.loadAssetBitmap(expectedFile);
            Bitmap decoded = WSQDecoder.decode(util.loadAssetFile(wsqFile)).getBitmap();
            util.assertBitmapsEqual("decoded " + wsqFile + " is different from " + expectedFile, expected, decoded);
        }
    }

    @Test
    public void testEncode() throws Exception {
        List<String> inputFiles = new ArrayList<>();
        String inputFolder = "nist/input";

        String[] files = ctx.getAssets().list(inputFolder);
        for (String file : files) {
            inputFiles.add(inputFolder + '/' + file);
        }

        for (float bitrate : new float[]{WSQEncoder.BITRATE_5_TO_1, WSQEncoder.BITRATE_15_TO_1}) {
            String referenceWsqFolder = "nist/wsq/" + (bitrate == WSQEncoder.BITRATE_5_TO_1 ? "225" : "75");
            String referenceWsqDecodedFolder = "nist/wsq_decoded/" + (bitrate == WSQEncoder.BITRATE_5_TO_1 ? "225" : "75");
            for (String inputFile : inputFiles) {
                //reference WSQ file. Only the size of this data will be used to compare to the encoder file size.
                String referenceEncodedFile = inputFile.replace(inputFolder + "/", referenceWsqFolder + "/").replace(".webp", ".wsq");
                //reference WSQ file, decoded. Will be used for the PSNR computation to verify the encoder output image quality
                String referenceDecodedFile = inputFile.replace(inputFolder + "/", referenceWsqDecodedFolder + "/");
                Bitmap input = util.loadAssetBitmap(inputFile);
                byte[] encoded = new WSQEncoder(input).setBitrate(bitrate).encode();

                /* We won't get the exact same data as the reference. I think it's because the reference files
                   were produced with older version of NBIS or something. I get different data even when I compress
                   and decompress using the NBIS command line utilities.

                   So instead of expecting the images to be exactly equal, we will measure PSNR between the two images
                   and check whether it's high enough for the images to be very similar. Also we check whether the file
                   size is similar.

                 */
                double minPsnrAllowed = 70; //70db - this is very high, only negligible differences allowed
                double maxSizeDifference = 0.01; //1% size difference allowed

                //check file size
                int referenceSize = util.loadAssetFile(referenceEncodedFile).length;
                double sizeDiff = Math.abs(1 - (encoded.length * 1.0 / referenceSize));
                assertTrue(String.format("Encoded %s size (%d bytes) too different from reference %s (%d bytes). Difference is %d %%, max allowed is %d %%.",
                                         inputFile, encoded.length, referenceEncodedFile, referenceSize, (int)(sizeDiff * 100), (int)(maxSizeDifference * 100)),
                           sizeDiff <= maxSizeDifference);


                //check image similarity
                Bitmap decoded = WSQDecoder.decode(encoded).getBitmap();
                Bitmap reference = util.loadAssetBitmap(referenceDecodedFile);

                double psnr = util.psnr(decoded, reference);
                assertTrue(String.format("Encoded %s has PSNR %f compared to %s. Minimum allowed PSNR is %f.", inputFile, psnr, referenceDecodedFile, minPsnrAllowed),
                           psnr >= minPsnrAllowed);
            }
        }
    }
}