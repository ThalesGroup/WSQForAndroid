# WSQ for Android
---------------------------
An open-source WSQ image encoder/decoder for Android based on [NBIS](https://www.nist.gov/services-resources/software/nist-biometric-image-software-nbis) v5.0.0.

## Set up
Add dependency to your `build.gradle`
```groovy
implementation 'io.github.michaldvorak-gemalto:wsq-android:1.3.0'
```

## Basic Usage
Decoding an image:
```java
WSQDecodedImage decoded = WSQDecoder.decode(wsqData).getBitmap();
Bitmap bmp = decoded.getBitmap();
imgView.setImageBitmap(bmp);
```
Encoding an image:
```java
Bitmap bmp = ...;
//higher-quality encode
byte[] wsqData = new WSQEncoder(bmp)
                     .setBitrate(WSQEncoder.BITRATE_5_TO_1)
                     .encode();
//lower-quality encode
byte[] wsqData = new WSQEncoder(bmp)
                     .setBitrate(WSQEncoder.BITRATE_15_TO_1)
                     .encode();
```

## Advanced Usage
### Comments
A WSQ file can contain multiple comments. A comment is a byte array whose interpretation is up to the application. Please note that the reference [NBIS](https://www.nist.gov/services-resources/software/nist-biometric-image-software-nbis) code is not capable to properly process comments containing a `0x00` byte. The **WSQForAndroid** library uses its own code to handle comments, therefore you can use it to store and load binary data (or text data which includes `0x00` bytes, like UCS-2 encoded text) to and from WSQ comments. It may, however, introduce compatibility issues with any software based on the NBIS code.

In addition to any comments provided by you, each produced WSQ image will also include a so-called NISTCOM comment containing image metadata in text form. The NISTCOM comment format is described [here](https://www.nist.gov/system/files/documents/srd/Spec-db-14.pdf).

Encoding comments:
```java
Bitmap bmp = ...;
String textComment = ...;
byte[] binaryComment = ...;

byte[] wsqData = new WSQEncoder(bmp)
                     .setBitrate(WSQEncoder.BITRATE_5_TO_1)
                     .addComment(textComment) //will be encoded as UTF-8
                     .addComment(binaryComment)
                     .encode();
```

Decoding comments:
```java
WSQDecodedImage decoded = WSQDecoder.decode(wsqData).getBitmap();
List<byte[]> comments = decoded.getComments();
for (byte[] comment : comments) {
  //process the comment
}
```

### PPI
One of the values contained in the NISTCOM comment is PPI (pixels per inch, integer value). If you know it, you can provide this value when encoding an image:
```java
byte[] wsqData = new WSQEncoder(bmp)
                     .setBitrate(WSQEncoder.BITRATE_5_TO_1)
                     .setPpi(500)
                     .encode();
```

The value can then be decoded:
```java
WSQDecodedImage decoded = WSQDecoder.decode(wsqData).getBitmap();
int ppi = decoded.getPpi();
if (ppi != WSQEncoder.UNKNOWN_PPI) {
  //do something with the ppi
}
```
