//
// Created by cainjiang on 2020/11/25.
//

#include <jni.h>
#include "ffpmeg_filter_manager.h"
#include "../../opengl/log_util.h"

AVFilterContext *buffersrc_ctx;
AVFilterContext *buffersink_ctx;
AVFilterGraph *filter_graph;

int init_filters(const char *filter_des, const AVCodecContext *p_codec_ctx) {
    char args[512];
    int ret;
    /**
     * 滤镜输入缓冲区
     * 解码器解码后的数据都会放到buffer中
     * 这是一个特殊的filter
     */
    const AVFilter *buffer_filter = avfilter_get_by_name("buffer");
    /**
     * 滤镜输出缓冲区
     * 滤镜处理完后输出的数据都会放在buffersink中
     * 是一个特殊的filter
     */
    const AVFilter *buffersink_filter = avfilter_get_by_name("buffersink");

    AVFilterInOut *outputs = avfilter_inout_alloc();
    AVFilterInOut *inputs = avfilter_inout_alloc();
    AVBufferSinkParams *buffersink_params = av_buffersink_params_alloc();

    enum AVPixelFormat pix_fmts[] = {AV_PIX_FMT_YUV420P, AV_PIX_FMT_GRAY8, AV_PIX_FMT_NONE};

    /**
     * 创建filter图
     * 会包含本次使用到的所有过滤器
     */
    filter_graph = avfilter_graph_alloc();

    if (!filter_graph) {
        ret = AVERROR(ENOMEM);
        LOGE("cannot create filter graph!")
        goto end;
    }

    /**
     * buffer video source: the decoded frames from the decoder will be inserted here.
     */
    snprintf(args, sizeof(args),
             "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
             p_codec_ctx->width, p_codec_ctx->height,
             p_codec_ctx->pix_fmt,
             p_codec_ctx->time_base.num, p_codec_ctx->time_base.den,
             p_codec_ctx->sample_aspect_ratio.num, p_codec_ctx->sample_aspect_ratio.den);

    /**
     * 创建过滤器实例,并将其添加到现有graph中
     */
    if ((ret = avfilter_graph_create_filter(&buffersrc_ctx, buffer_filter, "in", args, nullptr, filter_graph)) < 0) {
        av_log(nullptr, AV_LOG_ERROR, "cannot create buffer source\n");
        LOGE("cannot create buffer source!")
        goto end;
    }

    /**
     * 缓冲视频接收器,终止过滤器链
     */
    /* buffer video sink: to terminate the filter chain. */
    buffersink_params->pixel_fmts = pix_fmts;
    if ((ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink_filter, "out", nullptr, buffersink_params, filter_graph)) < 0) {
        av_log(nullptr, AV_LOG_ERROR, "cannot create buffer sink\n");
        LOGE("cannot create buffer sink!")
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
    outputs->next = nullptr;

    /*
     * The buffer sink input must be connected to the output pad of
     * the last filter described by filters_descr; since the last
     * filter output label is not specified, it is set to "out" by
     * default.
     */
    inputs->name = av_strdup("out");
    inputs->filter_ctx = buffersink_ctx;
    inputs->pad_idx = 0;
    inputs->next = nullptr;

    /**
     * 将由字符串描述的图形添加到图形中
     */
    if ((ret = avfilter_graph_parse_ptr(filter_graph, filter_des, &inputs, &outputs, nullptr)) < 0) {
        LOGE("cannot parse ptr!")
        goto end;
    }

    if ((ret = avfilter_graph_config(filter_graph, nullptr)) < 0) {
        LOGE("cannot config graph!")
        goto end;
    }

    end:
    if (inputs) {
        avfilter_inout_free(&inputs);
    }
    if (outputs) {
        avfilter_inout_free(&outputs);
    }
    if (buffersink_params) {
        av_free(buffersink_params);
    }

    return ret;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_cain_videotemp_ffmpeg_FfmpegFilter_play(JNIEnv *env, jobject thiz, jstring input_file, jstring filter_desc_str, jobject surface) {
    LOGI("native filter play.")

    int rel = 0;
    AVFormatContext *p_format_ctx = nullptr;
    // 输入文件
    const char *input_file_path;
    // 滤镜的描述
    const char *filter_desc = nullptr;
    // 视频流索引
    int video_stream_index = -1;
    // 解码器上下文
    AVCodecContext *p_codec_ctx = nullptr;
    // 视频解码器
    AVCodec *p_codec;

    ANativeWindow *native_window;
    ANativeWindow_Buffer window_buffer;

    AVFrame *p_frame = nullptr;
    AVFrame *p_frame_RGBA = nullptr;

    AVPacket packet;
    struct SwsContext *sws_ctx;
    uint8_t *buffer = nullptr;
    int bytes_num;
    // 视频宽高
    int video_height;
    int video_width;

    /**
     * 1.初始化注册相关组件
     */
    av_register_all();
    avfilter_register_all();

    p_format_ctx = avformat_alloc_context();

    // 从jstring中获取char，注意检查char，注意调用ReleaseStringUTFChars()释放内存
    input_file_path = env->GetStringUTFChars(input_file, JNI_FALSE);
    if (input_file_path == nullptr) {
        LOGE("retrieve input file path fail!")
        rel = -1;
        goto end;
    }

    // 打开文件，注意调用avformat_close_input()关闭文件
    if (avformat_open_input(&p_format_ctx, input_file_path, nullptr, nullptr) != 0) {
        LOGE("couldn't open file: %s\n", input_file_path)
        rel = -1;
        goto end;
    }

    // 读取一部分视音频数据并且获得一些相关的信息
    // 主要用于给每个媒体流（音频/视频）的AVStream结构体赋值
    if (avformat_find_stream_info(p_format_ctx, nullptr) < 0) {
        LOGE("couldn't find stream information.")
        rel = -1;
        goto end;
    }

    // 寻找出视频流索引
    for (int i = 0; i < p_format_ctx->nb_streams; i++) {
        if (p_format_ctx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_index = i;
            break;
        }
    }
    LOGD("video_stream_index = %d", video_stream_index)
    if (video_stream_index == -1) {
        LOGE("didn't find a video stream.")
        rel = -1;
        goto end;
    }

    // 获取视频流的codec context
    p_codec_ctx = p_format_ctx->streams[video_stream_index]->codec;

    // 初始化滤镜
    filter_desc = env->GetStringUTFChars(filter_desc_str, JNI_FALSE);
    if (init_filters(filter_desc, p_codec_ctx) < 0) {
        LOGE("init filter fail!")
        rel = -1;
        goto end;
    }

    // 寻找视频的解码器
    p_codec = avcodec_find_decoder(p_codec_ctx->codec_id);
    if (p_codec == nullptr) {
        LOGE("couldn't find video codec.")
        rel = -1;
        goto end;
    }

    // 初始化一个视音频编解码器的AVCodecContext
    if (avcodec_open2(p_codec_ctx, p_codec, nullptr) < 0) {
        LOGE("couldn't open codec.")
        rel = -1;
        goto end;
    }

    // 获取native window
    native_window = ANativeWindow_fromSurface(env, surface);

    // 获取视频宽高
    video_width = p_codec_ctx->width;
    video_height = p_codec_ctx->height;
    LOGD("video width = %d, video height = %d", video_width, video_height)

    // 设置native window的buffer大小,可自动拉伸
    ANativeWindow_setBuffersGeometry(native_window, video_width, video_height, WINDOW_FORMAT_RGBA_8888);
    // todo 为何要再次打开解码器呢？
    if (avcodec_open2(p_codec_ctx, p_codec, nullptr) < 0) {
        LOGE("couldn't open codec.")
        rel = -1;
        goto end;
    }

    // 分配视频帧结构体
    // 只是分配AVFrame结构体，data指向的内存并没有分配，需要单独指定
    p_frame = av_frame_alloc();
    p_frame_RGBA = av_frame_alloc();
    if (p_frame_RGBA == nullptr || p_frame == nullptr) {
        LOGE("couldn't allocate video frame.")
        rel = -1;
        goto end;
    }

    // Determine required buffer size and allocate buffer
    // buffer中数据就是用于渲染的,且格式为RGBA
    // 通过指定像素格式、图像宽、图像高来计算所需的内存大小
    bytes_num = av_image_get_buffer_size(AV_PIX_FMT_RGBA, video_width, video_height, 1);
    buffer = (uint8_t *) av_malloc(bytes_num * sizeof(uint8_t));
    av_image_fill_arrays(p_frame_RGBA->data, p_frame_RGBA->linesize, buffer, AV_PIX_FMT_RGBA, video_width, video_height, 1);

    // 由于解码出来的帧格式不是RGBA的,在渲染之前需要进行格式转换
    sws_ctx = sws_getContext(video_width, video_height, p_codec_ctx->pix_fmt,
                             video_width, video_height, AV_PIX_FMT_RGBA,
                             SWS_BILINEAR, nullptr, nullptr, nullptr);

    // 解帧
    int is_decode_frame_finished;
    while (av_read_frame(p_format_ctx, &packet) >= 0) {
        // Is this a packet from the video stream?
        if (packet.stream_index == video_stream_index) {
            // Decode video frame
            // 解码一帧视频数据
            avcodec_decode_video2(p_codec_ctx, p_frame, &is_decode_frame_finished, &packet);
            // 并不是decode一次就可解码出一帧
            if (is_decode_frame_finished) {

                //added by ws for AVfilter start=====================
                // 获取PTS
                p_frame->pts = av_frame_get_best_effort_timestamp(p_frame);

                // push the decoded frame into the filtergraph
                // 向FilterGraph中加入一个AVFrame
                if (av_buffersrc_add_frame(buffersrc_ctx, p_frame) < 0) {
                    LOGE("couldn't av_buffersrc_add_frame")
                    rel = -1;
                    break;
                }

                //从sink filter中获取一个AVFrame
                if (av_buffersink_get_frame(buffersink_ctx, p_frame) < 0) {
                    LOGE("couldn't av_buffersink_get_frame")
                    rel = -1;
                    break;
                }
                //added by ws for AVfilter end=====================

                // lock native window buffer
                ANativeWindow_lock(native_window, &window_buffer, nullptr);

                // 格式转换
                // 用于转换像素
                sws_scale(sws_ctx, (uint8_t const *const *) p_frame->data,
                          p_frame->linesize, 0, p_codec_ctx->height,
                          p_frame_RGBA->data, p_frame_RGBA->linesize);

                // 获取stride
                auto *dst = (uint8_t *) window_buffer.bits;
                int dstStride = window_buffer.stride * 4;
                uint8_t *src = (p_frame_RGBA->data[0]);
                int srcStride = p_frame_RGBA->linesize[0];

                // 由于window的stride和帧的stride不同,因此需要逐行复制
                for (int h = 0; h < video_height; h++) {
                    memcpy(dst + h * dstStride, src + h * srcStride, srcStride);
                }

                ANativeWindow_unlockAndPost(native_window);
            }

        }
        av_packet_unref(&packet);
    }

    end:
    if (input_file_path) {
        env->ReleaseStringUTFChars(input_file, input_file_path);
    }
    if (filter_desc) {
        env->ReleaseStringUTFChars(filter_desc_str, filter_desc);
    }
    if (buffer) {
        av_free(buffer);
    }
    if (p_frame_RGBA) {
        av_frame_free(&p_frame_RGBA);
    }
    if (p_frame) {
        av_frame_free(&p_frame);
    }
    if (filter_graph) {
        avfilter_graph_free(&filter_graph);
    }
    if (p_codec_ctx) {
        avcodec_close(p_codec_ctx);
    }
    if (p_format_ctx) {
        avformat_close_input(&p_format_ctx);
    }
    return rel;
}
