package com.cain.videotemp.audio

/**
 * @author cainjiang
 * @date 2020/4/20
 */
class Mp3Encoder {
    /**
     * 初始化
     * @param pcmPath pcm文件路径
     * @param audioChanel 声道
     * @param byteRate 码率
     * @param sampleRate 采样率
     * @param mp3Path mp3文件路径
     */
    external fun initEcoder(pcmPath: String, audioChanel: Int, byteRate: Int, sampleRate: Int, mp3Path: String): Int

    /**
     * 转码
     */
    external fun encode()

    /**
     * 销毁实例
     */
    external fun destroy()
}