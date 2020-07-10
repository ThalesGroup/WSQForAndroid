// based on imgtools/src/bin/dwsq/dwsq.c
#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>
#include <stdio.h>
#include <string.h>

#include <img_io.h>
#include <wsq.h>

#include <android/log.h>
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "libwsq",__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "libwsq",__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "libwsq",__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "libwsq",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "libwsq",__VA_ARGS__)

//this variable must be here - is referenced by some NBIS classes
int debug = 0;

//stores decoded image data
typedef struct image_data {
    jint width;
    jint height;
    jint ppi;
    int* pixels;
} image_data_t;

#define EXIT_SUCCESS 0
#define EXIT_FAILURE 1

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    // Get jclass with env->FindClass.
    // Register methods with env->RegisterNatives.

    return JNI_VERSION_1_6;
}

jint decodeWSQ(unsigned char *idata, int ilen, image_data_t *outImage) {
    unsigned char *odata;
    int width, height;             /* image parameters */
    int depth, ppi;
    int lossyflag;                 /* data loss flag */
    
    if((wsq_decode_mem(&odata, &width, &height, &depth, &ppi, &lossyflag, idata, ilen))){
        LOGE("Error decoding file");
        return EXIT_FAILURE;
    }

    outImage->height = height;
    outImage->width = width;
    outImage->ppi = ppi;
    int length = height * width;

    outImage->pixels = (int *) malloc(sizeof(int) * length);
    
    if (!outImage->pixels) {
        LOGE("Could not allocate %d bytes of memory.", length * sizeof(int));
        free(odata);
        return EXIT_FAILURE;
    }
    
    for (int i = 0; i < length; i++) {
        int color = odata[i];
        outImage->pixels[i] = (int)0xFF000000 | (color << 16) | (color << 8) | color;
    }
    
    free(odata);
	return EXIT_SUCCESS;
}

jintArray prepareReturnData(JNIEnv *env, image_data_t *outImage) {
    //prepare return data: first three integers in the array are width, height, isAlpha, then image pixels
    jintArray ret = env->NewIntArray(outImage->width * outImage->height + 3);
    env->SetIntArrayRegion(ret, 0, 3, (jint*)outImage);
    env->SetIntArrayRegion(ret, 3, outImage->width * outImage->height, outImage->pixels);
    free(outImage->pixels);
    outImage->pixels = NULL;
    return ret;
}

JNIEXPORT jintArray JNICALL Java_com_gemalto_wsq_Native_decodeWSQFile(JNIEnv *env, jclass thiz, jstring fileName) {
    int ilen;
    unsigned char *idata;
    char *ifile;
    image_data_t outImage;
    jintArray ret = NULL;

    //sanity check
    if (fileName == NULL) return NULL;

    const char *c_file = env->GetStringUTFChars(fileName, NULL);
    ifile = (char *) malloc((strlen(c_file) + 1) * sizeof(char));
    strcpy(ifile, c_file);
    env->ReleaseStringUTFChars(fileName, c_file);
    
    if((read_raw_from_filesize(ifile, &idata, &ilen))) {
        LOGE("Error reading file %s", ifile);
        free(ifile);
        return NULL;
    }
    
    if (decodeWSQ(idata, ilen, &outImage) == EXIT_SUCCESS) {
        ret = prepareReturnData(env, &outImage);
    }
    
    free(ifile);
    free(idata);

    return ret;
}

JNIEXPORT jintArray JNICALL Java_com_gemalto_wsq_Native_decodeWSQByteArray(JNIEnv *env, jclass thiz, jbyteArray data) {
    int ilen;
    unsigned char *idata;
    jbyte *bufferPtr;
    image_data_t outImage;
    jintArray ret = NULL;

    //sanity check
    if (data == NULL) return NULL;

    //copy bytes from java
    ilen = env->GetArrayLength(data);
    bufferPtr = env->GetByteArrayElements(data, NULL);
    idata = (unsigned char *)malloc(ilen * sizeof(char));
    memcpy(idata, bufferPtr, ilen);
    env->ReleaseByteArrayElements(data, bufferPtr, JNI_ABORT);
    
    if (decodeWSQ(idata, ilen, &outImage) == EXIT_SUCCESS) {
        ret = prepareReturnData(env, &outImage);
    }
    
    free(idata);

    return ret;
}

static const int MAX_COMMENT_LEN = (2 << 16) - 3;

JNIEXPORT jbyteArray JNICALL Java_com_gemalto_wsq_Native_encodeWSQByteArray(JNIEnv *env, jclass thiz, jintArray pixels, jint width, jint height, jfloat r_bitrate, jint ppi, jstring comment) {
    int i;
    unsigned char *idata;    /* Input RGB data */
    unsigned char *odata;    /* Encoded WSQ data */
    int olen;                /* Number of bytes in the WSQ data. */
    jint *bufferPtr;
    char *comment_text = NULL;      /* Comment text */
    jbyteArray ret;          /* Output data */
    size_t commentLen;
    
    //copy comment
    if (comment != NULL) {
        const char *tmp = env->GetStringUTFChars(comment, NULL);
        commentLen = strlen(tmp);

        //make sure we don't copy a comment longer, than the NBIS format supports
        if (commentLen > MAX_COMMENT_LEN) commentLen = MAX_COMMENT_LEN;
        comment_text = (char *) malloc((commentLen + 1) * sizeof(char));
        strncpy(comment_text, tmp, commentLen);
        comment_text[commentLen] = 0;

        env->ReleaseStringUTFChars(comment, tmp);
    }
    
    //copy pixels from java and convert to grey
    idata = (unsigned char *)malloc(width * height * sizeof(unsigned char));
    bufferPtr = env->GetIntArrayElements(pixels, NULL);
    for (i = 0; i < width * height; i++) {
        idata[i] = (
            ((bufferPtr[i] >> 16) & 0xFF) +     /* R */
            ((bufferPtr[i] >>  8) & 0xFF) +     /* G */
            ((bufferPtr[i]      ) & 0xFF)       /* B */
        ) / 3;
    }
    env->ReleaseIntArrayElements(pixels, bufferPtr, JNI_ABORT);
    
    /* Encode/compress the image pixmap. */
    if(wsq_encode_mem(&odata, &olen, r_bitrate,
                             idata, width, height, 8 /* bit depth */, ppi, comment_text)){
        free(idata);
        if(comment_text != NULL) {
            free(comment_text);
        }
        return NULL;
    }

    free(idata);
    if(comment_text != NULL) {
        free(comment_text);
    }
    
    ret = env->NewByteArray(olen);
    env->SetByteArrayRegion(ret, 0, olen, (jbyte *)odata);
    free(odata);
    return ret;
}

#ifdef __cplusplus
}
#endif