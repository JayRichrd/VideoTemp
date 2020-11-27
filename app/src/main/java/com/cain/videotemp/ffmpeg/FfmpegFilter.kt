package com.cain.videotemp.ffmpeg

import android.view.Surface

/**
 * @author cainjiang
 * @date 2020/11/26
 */
class FfmpegFilter {
    /**
     * 添加滤镜播放
     * @param inputFile 输入文件地址
     * @param filterDesStr filter滤镜描述
     * @return 返回结果 0:成功，-1:失败
     */
    external fun play(inputFile: String, filterDesStr: String, surface: Surface): Int
}