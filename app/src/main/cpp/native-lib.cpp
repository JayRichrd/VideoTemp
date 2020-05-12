#include <jni.h>
#include <string>
#include "lame/lame.h"
#include "audio/mp3_encode.h"
// opensl
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
// native asset manager
#include <sys/types.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
// openGL
#include <GLES3/gl3.h>
#include "opengl/shader_utils.h"

#define ARRAY_LEN(a) (sizeof(a) / sizeof(a[0]))

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
}

Mp3Encoder *mp3_encoder = NULL;
// 引擎对象
SLObjectItf engineObject = NULL;
// 引擎方法接口
SLEngineItf engineEngine = NULL;
//混音器
SLObjectItf outputMixObject = NULL;
SLEnvironmentalReverbItf outputMixEnvironmentalReverb = NULL;
SLEnvironmentalReverbSettings reverbSettings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;
//assets播放器
SLObjectItf fdPlayerObject = NULL;
SLPlayItf fdPlayerPlay = NULL;
//声音控制接口
SLVolumeItf fdPlayerVolume = NULL;

AAssetManager *g_pAssetManager = NULL;
GLuint g_program = NULL;
GLint g_position_handle = NULL;

void release();

void createEngine();

void release() {
    // destroy file descriptor audio player object, and invalidate all associated interfaces
    if (fdPlayerObject != NULL) {
        (*fdPlayerObject)->Destroy(fdPlayerObject);
        fdPlayerObject = NULL;
        fdPlayerPlay = NULL;
        fdPlayerVolume = NULL;
    }

    // destroy output mix object, and invalidate all associated interfaces
    if (outputMixObject != NULL) {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = NULL;
        outputMixEnvironmentalReverb = NULL;
    }

    // destroy engine object, and invalidate all associated interfaces
    if (engineObject != NULL) {
        (*engineObject)->Destroy(engineObject);
        engineObject = NULL;
        engineEngine = NULL;
    }
}

void createEngine() {
    // 创建引擎
    SLEngineOption engine_options[] = {{(SLuint32) SL_ENGINEOPTION_THREADSAFE, (SLuint32) SL_BOOLEAN_TRUE}};
    slCreateEngine(&engineObject, ARRAY_LEN(engine_options), engine_options, 0, NULL, NULL);
    // 初始化引擎
    (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    // 获取这个引擎对象的方法接口
    (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_cain_videotemp_MainActivity_stringFromJNI(JNIEnv *env, jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(get_lame_version());
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_audio_Mp3Encoder_encode(JNIEnv *env, jobject thiz) {
    mp3_encoder->encode_data();
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_audio_Mp3Encoder_destroy(JNIEnv *env, jobject thiz) {
    mp3_encoder->destory();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_cain_videotemp_audio_Mp3Encoder_initEcoder(JNIEnv *env, jobject thiz, jstring pcmPath, jint audioChanel, jint byteRate, jint sampleRate, jstring mp3Path) {
    const char *pcm_path = env->GetStringUTFChars(pcmPath, NULL);
    const char *mp3_path = env->GetStringUTFChars(mp3Path, NULL);
    mp3_encoder = new Mp3Encoder();
    int ret = mp3_encoder->init_encoder(pcm_path, mp3_path, sampleRate, audioChanel, byteRate);
    env->ReleaseStringUTFChars(pcmPath, pcm_path);
    env->ReleaseStringUTFChars(mp3Path, mp3_path);
    return ret;
}

extern "C" JNIEXPORT void JNICALL
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

    SwsContext *swsContext = sws_getContext(avCodecContext->width, avCodecContext->height, avCodecContext->pix_fmt, avCodecContext->width, avCodecContext->height, AV_PIX_FMT_RGBA,
                                            SWS_BICUBIC, NULL, NULL, NULL);
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

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_audio_OpenSLEsDelegate_playByAssets(JNIEnv *env, jobject thiz, jobject asset_manager, jstring file_name) {
    // 先释放资源
    release();
    const char *utf8 = env->GetStringUTFChars(file_name, NULL);
    // use asset manager to open asset by filename
    AAssetManager *mgr = AAssetManager_fromJava(env, asset_manager);
    AAsset *asset = AAssetManager_open(mgr, utf8, AASSET_MODE_UNKNOWN);
    env->ReleaseStringUTFChars(file_name, utf8);
    // open asset as file descriptor
    off_t start, length;
    int fd = AAsset_openFileDescriptor(asset, &start, &length);
    AAsset_close(asset);
    //第一步，创建引擎，并初始化获取接口方法
    createEngine();
    //第二步，创建混音器，并初始化获取接口方法
    const SLInterfaceID mids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean mreq[1] = {SL_BOOLEAN_FALSE};
    (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 1, mids, mreq);
    (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    SLresult result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB, &outputMixEnvironmentalReverb);
    if (SL_RESULT_SUCCESS == result) {
        (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(outputMixEnvironmentalReverb, &reverbSettings);
    }
    //第三步，设置播放器参数和创建播放器
    // 1、配置 audio source
    SLDataLocator_AndroidFD loc_fd = {SL_DATALOCATOR_ANDROIDFD, fd, start, length};
    SLDataFormat_MIME format_mime = {SL_DATAFORMAT_MIME, NULL, SL_CONTAINERTYPE_UNSPECIFIED};
    SLDataSource audioSrc = {&loc_fd, &format_mime};
    // 2、 配置 audio sink
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};
    // 创建播放器
    const SLInterfaceID ids[3] = {SL_IID_SEEK, SL_IID_MUTESOLO, SL_IID_VOLUME};
    const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    (*engineEngine)->CreateAudioPlayer(engineEngine, &fdPlayerObject, &audioSrc, &audioSnk, 3, ids, req);
    // 实现播放器
    (*fdPlayerObject)->Realize(fdPlayerObject, SL_BOOLEAN_FALSE);
    // 得到播放器接口
    (*fdPlayerObject)->GetInterface(fdPlayerObject, SL_IID_PLAY, &fdPlayerPlay);
    // 得到声音控制接口
    (*fdPlayerObject)->GetInterface(fdPlayerObject, SL_IID_VOLUME, &fdPlayerVolume);
    // 设置播放状态
    if (NULL != fdPlayerPlay) {
        (*fdPlayerPlay)->SetPlayState(fdPlayerPlay, SL_PLAYSTATE_PLAYING);
    }
    //设置播放音量 （100 * -50：静音 ）
    (*fdPlayerVolume)->SetVolumeLevel(fdPlayerVolume, 20 * -50);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_pic_opengl_NativeRender_glDraw(JNIEnv *env, jobject thiz) {
    GLint vertexCount = 3;
    // OpenGL的世界坐标系是 [-1, -1, 1, 1]
    GLfloat vertices[] = {0.0f, 0.5f, 0.0f, // 第一个点（x, y, z）
                          -0.5f, -0.5f, 0.0f, // 第二个点（x, y, z）
                          0.5f, -0.5f, 0.0f};
    glClear(GL_COLOR_BUFFER_BIT); // clear color buffer
    // 1. 选择使用的程序
    glUseProgram(g_program);
    // 2. 加载顶点数据
    glVertexAttribPointer(g_position_handle, vertexCount, GL_FLOAT, GL_FALSE, 3 * 4, vertices);
    glEnableVertexAttribArray(g_position_handle);
    // 3. 绘制
    glDrawArrays(GL_TRIANGLES, 0, vertexCount);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_pic_opengl_NativeRender_glResize(JNIEnv *env, jobject thiz, jint width, jint height) {
    glViewport(0, 0, width, height); // 设置视距窗口
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_pic_opengl_NativeRender_glInit(JNIEnv *env, jobject thiz) {
    char *vertexShaderSource = readAssetFile("vertex.vsh", g_pAssetManager);
    char *fragmentShaderSource = readAssetFile("fragment.fsh", g_pAssetManager);
    g_program = CreateProgram(vertexShaderSource, fragmentShaderSource);
    if (g_program == GL_NONE) {}
    // vPosition 是在 'vertex.vsh' 文件中定义的
    g_position_handle = glGetAttribLocation(g_program, "vPosition");
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // 背景颜色设置为黑色 RGBA (range: 0.0 ~ 1.0)
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_pic_opengl_NativeRender_registerAssetManager(JNIEnv *env, jobject thiz, jobject asset_manager) {
    if (asset_manager) {
        g_pAssetManager = AAssetManager_fromJava(env, asset_manager);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_cain_videotemp_audio_OpenSLEsDelegate_pause(JNIEnv *env, jobject thiz) {
    if (fdPlayerPlay != NULL) {
        (*fdPlayerPlay)->SetPlayState(fdPlayerPlay, SL_PLAYSTATE_PAUSED);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_cain_videotemp_audio_OpenSLEsDelegate_release(JNIEnv *env, jobject thiz) {
    release();
}