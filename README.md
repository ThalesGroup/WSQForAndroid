# WSQ for Android
---------------------------
An open-source WSQ image encoder/decoder for Android based on [NBIS](https://www.nist.gov/services-resources/software/nist-biometric-image-software-nbis) v5.0.0.

## Usage
Decoding an image:
```java
Bitmap bmp = WSQDecoder.decode(wsqData);
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

## Set up
Add dependency to your `build.gradle`
```groovy
implementation 'com.gemalto.wsq:wsq-android:1.0'
```
