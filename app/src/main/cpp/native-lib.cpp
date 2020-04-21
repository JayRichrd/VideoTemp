#include <jni.h>
#include <string>
#include "lame/lame.h"
#include "audio/mp3_encode.h"

Mp3Encoder *mp3_encoder = NULL;

extern "C" JNIEXPORT jstring JNICALL
Java_com_cain_videotemp_MainActivity_stringFromJNI(JNIEnv *env, jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(get_lame_version());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_cain_videotemp_audio_Mp3Encoder_encode(JNIEnv *env, jobject thiz) {
    mp3_encoder->encode_data();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_cain_videotemp_audio_Mp3Encoder_destroy(JNIEnv *env, jobject thiz) {
    mp3_encoder->destory();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_cain_videotemp_audio_Mp3Encoder_initEcoder(JNIEnv *env, jobject thiz, jstring pcmPath,
                                                    jint audioChanel, jint byteRate,
                                                    jint sampleRate, jstring mp3Path) {
    const char *pcm_path = env->GetStringUTFChars(pcmPath, NULL);
    const char *mp3_path = env->GetStringUTFChars(mp3Path, NULL);
    mp3_encoder = new Mp3Encoder();
    int ret = mp3_encoder->init_encoder(pcm_path, mp3_path, sampleRate, audioChanel, byteRate);
    env->ReleaseStringUTFChars(pcmPath, pcm_path);
    env->ReleaseStringUTFChars(mp3Path, mp3_path);
    return ret;
}