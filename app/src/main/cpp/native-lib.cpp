#include <jni.h>
#include <string>
#include "lame/lame.h"
#include "audio/mp3_encode.h"

extern "C" {// 必须添加这个，否则会报很多undefined reference错误
//封装格式处理
#include <libavformat/avformat.h>
#include <android/native_window_jni.h>
#include <libavfilter/avfilter.h>
#include <libavcodec/avcodec.h>
//封装格式处理
#include <libavformat/avformat.h>
//像素处理
#include <libswscale/swscale.h>
#include <unistd.h>

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
extern "C"
JNIEXPORT void JNICALL
Java_com_cain_videotemp_video_FFVideoPlayer_render(JNIEnv *env, jobject thiz, jstring play_url, jobject surface) {
    const char *url = env->GetStringUTFChars(play_url, 0);
    /**
     * 注册
     * 在4.0之后已经过期，可以直接忽略调用这个函数
     */
    //av_register_all();
    // 打开地址并获取里面的内容
    // avFormatContext是内容的一个上下文
    AVFormatContext *avFormatContext = avformat_alloc_context();
    avformat_open_input(&avFormatContext, url, NULL, NULL);
    avformat_find_stream_info(avFormatContext, NULL);

    // 找出视频流
    int video_index = -1;
    for (int i = 0; i < avFormatContext->nb_streams; ++i) {
        if (avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_index = i;
        }
    }

    /**
     * 下面将进行解码、转换、绘制
     */
    // 获取解码器上下文
    AVCodecContext *avCodecContext = avFormatContext->streams[video_index]->codec;
    // 获取解码器
    AVCodec *avCodec = avcodec_find_decoder(avCodecContext->codec_id);
    // 打开解码器
    if (avcodec_open2(avCodecContext, avCodec, NULL) < 0) { //打开失败直接返回
        return;
    }

    /**
     * 申请AVPacket和AVFrame
     * AVPacket的作用是保存解码之前的数据和一些附加信息，例如显示时间戳(pts)、解码时间戳(dts)、数据时长和所在媒体流的索引等
     * AVFrame的作用是存放解码过后的数据
     */
    AVPacket *avPacket = static_cast<AVPacket *>(av_malloc(sizeof(AVPacket)));
    av_init_packet(avPacket);
    /**
     * 分配一个AVFrame结构体
     * AVFrame结构体一般用于存储原始数,指向解码后的原始帧
     */
    AVFrame *avFrame = av_frame_alloc();
    /**
     * 分配一个AVFrame结构体
     * 指向存放转换成rgb后的帧
     */
    AVFrame *rgb_frame = av_frame_alloc();
    /**
     * rgb_frame是一个缓存区域，需要设置
     * 缓存区
     */
    uint8_t *out_buffer = static_cast<uint8_t *>(av_malloc(avpicture_get_size(AV_PIX_FMT_RGBA, avCodecContext->width, avCodecContext->height)));
    /**
     * 与缓存区关联
     * 设置rgb_frame缓存区
     */
    avpicture_fill((AVPicture *) rgb_frame, out_buffer, AV_PIX_FMT_RGBA, avCodecContext->width, avCodecContext->height);
    /**
     * 需要一个ANativeWindow来进行原生绘制
     */
    ANativeWindow *pANativeWindow = ANativeWindow_fromSurface(env, surface);
    if (pANativeWindow == 0) { // 获取native window失败直接返回
        return;
    }

    SwsContext *swsContext = sws_getContext(avCodecContext->width, avCodecContext->height,
                                            avCodecContext->pix_fmt, avCodecContext->width,
                                            avCodecContext->height, AV_PIX_FMT_RGBA, SWS_BICUBIC,
                                            NULL, NULL, NULL);
    // 视频缓冲区
    ANativeWindow_Buffer nativeWindow_outBuffer;

    /**
     * 开始解码
     */
    int frameCount;
    while (av_read_frame(avFormatContext, avPacket) >= 0) {
        if (avPacket->stream_index == video_index) {
            avcodec_decode_video2(avCodecContext, avFrame, &frameCount, avPacket);
            if (frameCount) {
                ANativeWindow_setBuffersGeometry(pANativeWindow, avCodecContext->width, avCodecContext->height, WINDOW_FORMAT_RGBA_8888);
                /**
                 * 上锁
                 */
                ANativeWindow_lock(pANativeWindow, &nativeWindow_outBuffer, NULL);
                /**
                 * 转换成RGB格式
                 */
                sws_scale(swsContext, (const uint8_t *const *) avFrame->data, avFrame->linesize, 0, avFrame->height, rgb_frame->data, rgb_frame->linesize);
                uint8_t *dst = static_cast<uint8_t *>(nativeWindow_outBuffer.bits);
                int destStride = nativeWindow_outBuffer.stride * 4;
                uint8_t *src = rgb_frame->data[0];
                int srcStride = rgb_frame->linesize[0];
                for (int i = 0; i < avCodecContext->height; i++) {
                    memcpy(dst + i * destStride, src + i * srcStride, srcStride);
                }
                ANativeWindow_unlockAndPost(pANativeWindow);

            }
        }
        av_free_packet(avPacket);
    }

    ANativeWindow_release(pANativeWindow);
    av_frame_free(&avFrame);
    av_frame_free(&rgb_frame);
    avcodec_close(avCodecContext);
    avformat_free_context(avFormatContext);

    env->ReleaseStringUTFChars(play_url, url);
}
}
