package com.cain.videotemp.video.listener

/**
 * @author : jiangyu
 * @date   : 2020/8/15
 * @desc   : 解码进度回调
 */
interface IDecoderProgress {
    /**
     * 视频宽高回调
     */
    fun videoSizeChange(width: Int, height: Int, rotationAngle: Int)

    /**
     * 视频播放进度回调
     */
    fun videoProgressChange(pos: Long)
}