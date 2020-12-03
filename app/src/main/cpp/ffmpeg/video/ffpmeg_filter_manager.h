//
// Created by cainjiang on 2020/11/25.
//

// 必须添加这个，否则会报很多undefined reference错误
extern "C"
{
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
}

#ifndef VIDEOTEMP_FFPMEG_FILTER_MANAGER_H
#define VIDEOTEMP_FFPMEG_FILTER_MANAGER_H

/**
 * 初始化filter
 * @param filter_des filter描述文本
 * @return
 */
int init_filters(const char *filter_des, const AVCodecContext *p_codec_ctx);

#endif //VIDEOTEMP_FFPMEG_FILTER_MANAGER_H
