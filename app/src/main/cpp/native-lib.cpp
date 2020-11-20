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
#include "opengl/log_util.h"
#include "opengl/MyLooper.h"

#define ARRAY_LEN(a) (sizeof(a) / sizeof(a[0]))

extern "C" {// 必须添加这个，否则会报很多undefined reference错误
//封装格式处理
#include <libavformat/avformat.h>
#include <android/native_window_jni.h>
#include <libavfilter/avfilter.h>
#include <libavcodec/avcodec.h>
#include <libavfilter/buffersrc.h>
#include <libavfilter/buffersink.h>
#include <libavutil/opt.h>
#include <libavutil/imgutils.h>
//封装格式处理
#include <libavformat/avformat.h>
//像素处理
#include <libswscale/swscale.h>
#include <unistd.h>
#include "sox/sox.h"
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

GLuint g_program = NULL;
GLint g_position = NULL;

MyLooper *mLooper = NULL;
ANativeWindow *mWindow = NULL;
long long fileLength;

AVFormatContext *fmt_ctx;
AVCodecContext *dec_ctx;
AVFilterContext *buffersink_ctx;
AVFilterContext *buffersrc_ctx;
AVFilterGraph *filter_graph;
//AVCodecContext *pCodecCtx;
int video_stream_index = -1;

/**
 * 打开输入文件
 * @param name 文件地址
 * @return
 */
int open_input_file(const char *input_file_path);

/**
 * 初始化filter
 * @param filter_descr filter描述文本
 * @return
 */
int init_filters(const char *filter_descr, const AVCodecContext *pAvCodecCtx);

/**
 * 保存YUV数据
 * @param filter_frame
 * @param out_file
 * @return
 */
int save_frame(AVFrame *filter_frame, FILE *out_file);

int callBack(sox_bool all_done, void *client_data) {
    LOGE("callback  : %d ", all_done)
    return 0;
}

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
Java_com_cain_videotemp_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
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
Java_com_cain_videotemp_pic_opengl_render_NativeRender_glDraw(JNIEnv *env, jobject thiz) {
    GLint vertexCount = 3;
    // OpenGL的世界坐标系是 [-1, -1, 1, 1]
    GLfloat vertices[] = {0.0f, 0.5f, 0.0f, // 第一个点（x, y, z）
                          -0.5f, -0.5f, 0.0f, // 第二个点（x, y, z）
                          0.5f, -0.5f, 0.0f // 第三个点（x, y, z）
    };
    // clear color buffer
    glClear(GL_COLOR_BUFFER_BIT);
    // 1. 选择使用的程序
    glUseProgram(g_program);
    // 2. 加载顶点数据
    glVertexAttribPointer(g_position, vertexCount, GL_FLOAT, GL_FALSE, 3 * 4, vertices);
    glEnableVertexAttribArray(g_position);
    // 3. 绘制
    glDrawArrays(GL_TRIANGLES, 0, vertexCount);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_pic_opengl_render_NativeRender_glResize(JNIEnv *env, jobject thiz, jint width, jint height) {
    // 设置视距窗口
    glViewport(0, 0, width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_pic_opengl_render_NativeRender_glInit(JNIEnv *env, jobject thiz, jobject asset_manager) {
    AAssetManager *am = AAssetManager_fromJava(env, asset_manager);
    char *vertexShaderSource = read_asset_file("vertex.vsh", am);
    char *fragmentShaderSource = read_asset_file("fragment.fsh", am);
    g_program = create_program(vertexShaderSource, fragmentShaderSource);
    if (g_program == GL_NONE) {
        LOGE("gl init failed!");
        return;
    }
    // vPosition 是在 'vertex.vsh' 文件中定义的
    g_position = glGetAttribLocation(g_program, "vPosition");
    LOGD("g_position: %d", g_position);
    // 背景颜色设置为黑色 RGBA (range: 0.0 ~ 1.0)
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_audio_OpenSLEsDelegate_pause(JNIEnv *env, jobject thiz) {
    if (fdPlayerPlay != NULL) {
        (*fdPlayerPlay)->SetPlayState(fdPlayerPlay, SL_PLAYSTATE_PAUSED);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_audio_OpenSLEsDelegate_release(JNIEnv *env, jobject thiz) {
    release();
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_pic_opengl_render_EGLRender_nativeInit(JNIEnv *env, jobject thiz) {
    mLooper = new MyLooper();
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_pic_opengl_render_EGLRender_nativeRelease(JNIEnv *env, jobject thiz) {
    if (mLooper != NULL) {
        mLooper->quit();
        delete mLooper;
        mLooper = NULL;
    }
    if (mWindow) {
        ANativeWindow_release(mWindow);
        mWindow = NULL;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_pic_opengl_render_EGLRender_onSurfaceCreated(JNIEnv *env, jobject thiz, jobject surface) {
    if (mWindow) {
        ANativeWindow_release(mWindow);
        mWindow = NULL;
    }
    mWindow = ANativeWindow_fromSurface(env, surface);
    if (mLooper) {
        mLooper->postMessage(kMsgSurfaceCreated, mWindow);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_pic_opengl_render_EGLRender_onSurfaceDestroyed(JNIEnv *env, jobject thiz) {
    if (mLooper) {
        mLooper->postMessage(kMsgSurfaceDestroyed);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_pic_opengl_render_EGLRender_onSurfaceChanged(JNIEnv *env, jobject thiz, jint width, jint height) {
    if (mLooper) {
        mLooper->postMessage(kMsgSurfaceChanged, width, height);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_cain_videotemp_audio_SoxUtils_soxAudio(JNIEnv *env, jobject thiz, jstring int_put_wav_path, jstring out_put_wav_path) {
    const char *inputPath = env->GetStringUTFChars(int_put_wav_path, 0);
    const char *outputPath = env->GetStringUTFChars(out_put_wav_path, 0);

    FILE *out_file = NULL;
    LOGE("打开文件")
    out_file = fopen(outputPath, "wb");
    if (!out_file) {
        LOGE("打开文件失败！")
        return;
    }
    LOGI("开始处理")
    int re = sox_init();
    if (re != SOX_SUCCESS) {
        LOGE("初始化 sox 失败")
        return;
    }
    //初始化输入文件
    sox_format_t *in;
    in = sox_open_read(inputPath, NULL, NULL, NULL);
    LOGE("length %lld", lsx_filelength(in))
    fileLength = in->signal.length;
    //初始化输出文件
    sox_format_t *out;
    int i = in->signal.channels;
    LOGE("%d", i);
    out = sox_open_write(outputPath, &in->signal, &in->encoding, NULL, NULL, NULL);
    //初始化效果器链
    sox_effects_chain_t *chain;
    chain = sox_create_effects_chain(&in->encoding, &out->encoding);
    //配置输入效果器
    sox_effect_t *inputEffect;
    inputEffect = sox_create_effect(sox_find_effect("input"));
    char *args[10];
    args[0] = (char *) in;
    sox_effect_options(inputEffect, 1, args);
    sox_add_effect(chain, inputEffect, &in->signal, &in->signal);
    delete (inputEffect);
    //混音处理器 效果器
    sox_effect_t *e;
    e = sox_create_effect(sox_find_effect("reverb"));
    char *reverbrance = "30";
    char *hfDamping = "30";
    char *roomScale = "30";

    char *stereoDepth = "30";
    char *preDelay = "30";
    char *wetGain = "0";
    char *args1[] = {reverbrance, hfDamping, roomScale, stereoDepth, preDelay, wetGain};
    int ref = sox_effect_options(e, 6, args1);
    sox_add_effect(chain, e, &in->signal, &in->signal);
    delete (e);

    //音量增强效果器
    sox_effect_t *volEffect;
    volEffect = sox_create_effect(sox_find_effect("vol"));
    args[0] = "5dB";
    sox_effect_options(volEffect, 1, args);
    sox_add_effect(chain, volEffect, &in->signal, &in->signal);
    delete (volEffect);
    //输出效果器
    sox_effect_t *outputEffect;
    outputEffect = sox_create_effect(sox_find_effect("output"));
    args[0] = (char *) out;
    sox_effect_options(outputEffect, 1, args);
    sox_add_effect(chain, outputEffect, &in->signal, &in->signal);
    delete (outputEffect);
    void *data;
    sox_flow_effects(chain, (sox_flow_effects_callback) (callBack), data);
    sox_delete_effects_chain(chain);
    sox_close(out);
    sox_close(in);
    sox_quit();
    LOGE("处理完成")
    env->ReleaseStringUTFChars(int_put_wav_path, inputPath);
    env->ReleaseStringUTFChars(out_put_wav_path, outputPath);
}


int init_filters(const char *filter_descr, const AVCodecContext *pAvCodecCtx) {
    char args[512];
    int ret;
    /**
     * 滤镜输入缓冲区
     * 解码器解码后的数据都会放到buffer中
     * 是一个特殊的filter
     */
    const AVFilter *buffersrc = avfilter_get_by_name("buffer");

    /**
     * 滤镜输出缓冲区
     * 滤镜处理完后输出的数据都会放在buffersink中
     * 是一个特殊的filter
     */
    const AVFilter *buffersink = avfilter_get_by_name("buffersink");
    AVFilterInOut *outputs = avfilter_inout_alloc();
    AVFilterInOut *inputs = avfilter_inout_alloc();
    enum AVPixelFormat pix_fmts[] = {AV_PIX_FMT_YUV420P, AV_PIX_FMT_GRAY8, AV_PIX_FMT_NONE};

    /**
     * 创建filter图
     * 会包含本次使用到的所有过滤器
     */
    filter_graph = avfilter_graph_alloc();
    if (!outputs || !inputs || !filter_graph) {
        ret = AVERROR(ENOMEM);
        goto end;
    }

    /**
     * buffer video source: the decoded frames from the decoder will be inserted here.
     */
    snprintf(args, sizeof(args),
             "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
             pAvCodecCtx->width, pAvCodecCtx->height, pAvCodecCtx->pix_fmt,
             pAvCodecCtx->time_base.num, pAvCodecCtx->time_base.den,
             pAvCodecCtx->sample_aspect_ratio.num, pAvCodecCtx->sample_aspect_ratio.den);

    /**
     * 创建过滤器实例,并将其添加到现有graph中
     */
    if ((ret = avfilter_graph_create_filter(&buffersrc_ctx, buffersrc, "in", args, NULL, filter_graph)) < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot create buffer source\n");
        goto end;
    }

    /**
     * 缓冲视频接收器,终止过滤器链
     */
    /* buffer video sink: to terminate the filter chain. */
    AVBufferSinkParams *buffersink_params;
    buffersink_params = av_buffersink_params_alloc();
    buffersink_params->pixel_fmts = pix_fmts;
    ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink, "out", NULL, buffersink_params, filter_graph);
    av_free(buffersink_params);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot create buffer sink\n");
        goto end;
    }


    /*
     * Set the endpoints for the filter graph. The filter_graph will
     * be linked to the graph described by filters_descr.
     *
     * The buffer source output must be connected to the input pad of
     * the first filter described by filters_descr; since the first
     * filter input label is not specified, it is set to "in" by
     * default.
     */
    outputs->name = av_strdup("in");
    outputs->filter_ctx = buffersrc_ctx;
    outputs->pad_idx = 0;
    outputs->next = NULL;

    /*
     * The buffer sink input must be connected to the output pad of
     * the last filter described by filters_descr; since the last
     * filter output label is not specified, it is set to "out" by
     * default.
     */
    inputs->name = av_strdup("out");
    inputs->filter_ctx = buffersink_ctx;
    inputs->pad_idx = 0;
    inputs->next = NULL;

    /**
     * 将由字符串描述的图形添加到图形中
     */
    if ((ret = avfilter_graph_parse_ptr(filter_graph, filter_descr, &inputs, &outputs, NULL)) < 0)
        goto end;

    if ((ret = avfilter_graph_config(filter_graph, NULL)) < 0)
        goto end;

    end:
    avfilter_inout_free(&inputs);
    avfilter_inout_free(&outputs);

    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_cain_videotemp_FfmpegFilterActivity_play(JNIEnv *env, jobject thiz, jstring input_file, jobject surface) {
    const char *input_file_path = env->GetStringUTFChars(input_file, JNI_FALSE);
    av_register_all();
    avfilter_register_all();
    AVFormatContext *pFormatCtx = avformat_alloc_context();
    // Open video file
    if (avformat_open_input(&pFormatCtx, input_file_path, NULL, NULL) != 0) {
        // Couldn't open file
        LOGD("Couldn't open file:%s\n", input_file_path);
        return -1;
    }
    // Retrieve stream information
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGD("Couldn't find stream information.");
        return -1;
    }
    // Find the first video stream
    int videoStream = -1;
    for (int i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO && videoStream < 0) {
            videoStream = i;
        }
    }
    if (videoStream == -1) {
        LOGD("Didn't find a video stream.");
        return -1; // Didn't find a video stream
    }
    // Get a pointer to the codec context for the video stream
    AVCodecContext *pCodecCtx = pFormatCtx->streams[videoStream]->codec;
    const char *filters_descr = "lutyuv='u=128:v=128'";
    if (init_filters(filters_descr, pCodecCtx) < 0) {
        LOGE("init filter fail!")
        return -1;
    }
    // Find the decoder for the video stream
    AVCodec *pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
    if (pCodec == NULL) {
        LOGD("Codec not found.")
        return -1; // Codec not found
    }

    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGD("Could not open codec.");
        return -1; // Could not open codec
    }

    // 获取native window
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);
    // 获取视频宽高
    int videoWidth = pCodecCtx->width;
    int videoHeight = pCodecCtx->height;
    // 设置native window的buffer大小,可自动拉伸
    ANativeWindow_setBuffersGeometry(nativeWindow, videoWidth, videoHeight, WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer windowBuffer;
    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGD("Could not open codec.");
        return -1; // Could not open codec
    }
    // Allocate video frame
    AVFrame *pFrame = av_frame_alloc();
    // 用于渲染
    AVFrame *pFrameRGBA = av_frame_alloc();
    if (pFrameRGBA == NULL || pFrame == NULL) {
        LOGD("Could not allocate video frame.")
        return -1;
    }
    // Determine required buffer size and allocate buffer
    // buffer中数据就是用于渲染的,且格式为RGBA
    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, pCodecCtx->width, pCodecCtx->height, 1);
    uint8_t *buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
    av_image_fill_arrays(pFrameRGBA->data, pFrameRGBA->linesize, buffer, AV_PIX_FMT_RGBA, pCodecCtx->width, pCodecCtx->height, 1);
    // 由于解码出来的帧格式不是RGBA的,在渲染之前需要进行格式转换
    struct SwsContext *sws_ctx = sws_getContext(pCodecCtx->width,
                                                pCodecCtx->height,
                                                pCodecCtx->pix_fmt,
                                                pCodecCtx->width,
                                                pCodecCtx->height,
                                                AV_PIX_FMT_RGBA,
                                                SWS_BILINEAR,
                                                NULL,
                                                NULL,
                                                NULL);
    int frameFinished;
    AVPacket packet;
    while (av_read_frame(pFormatCtx, &packet) >= 0) {
        // Is this a packet from the video stream?
        if (packet.stream_index == videoStream) {

            // Decode video frame
            avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);



            // 并不是decode一次就可解码出一帧
            if (frameFinished) {

                //added by ws for AVfilter start
                pFrame->pts = av_frame_get_best_effort_timestamp(pFrame);

                //* push the decoded frame into the filtergraph
                if (av_buffersrc_add_frame(buffersrc_ctx, pFrame) < 0) {
                    LOGD("Could not av_buffersrc_add_frame")
                    break;
                }

                if (av_buffersink_get_frame(buffersink_ctx, pFrame) < 0) {
                    LOGD("Could not av_buffersink_get_frame")
                    break;
                }
                //added by ws for AVfilter end

                // lock native window buffer
                ANativeWindow_lock(nativeWindow, &windowBuffer, 0);

                // 格式转换
                sws_scale(sws_ctx, (uint8_t const *const *) pFrame->data,
                          pFrame->linesize, 0, pCodecCtx->height,
                          pFrameRGBA->data, pFrameRGBA->linesize);

                // 获取stride
                uint8_t *dst = (uint8_t *) windowBuffer.bits;
                int dstStride = windowBuffer.stride * 4;
                uint8_t *src = (pFrameRGBA->data[0]);
                int srcStride = pFrameRGBA->linesize[0];

                // 由于window的stride和帧的stride不同,因此需要逐行复制
                int h;
                for (h = 0; h < videoHeight; h++) {
                    memcpy(dst + h * dstStride, src + h * srcStride, srcStride);
                }

                ANativeWindow_unlockAndPost(nativeWindow);
            }

        }
        av_packet_unref(&packet);
    }

    av_free(buffer);
    av_free(pFrameRGBA);

    // Free the YUV frame
    av_free(pFrame);

    avfilter_graph_free(&filter_graph); //added by ws for avfilter
    // Close the codecs
    avcodec_close(pCodecCtx);

    // Close the video file
    avformat_close_input(&pFormatCtx);
    return 0;
}