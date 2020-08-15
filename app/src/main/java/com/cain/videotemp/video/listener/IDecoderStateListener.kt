package com.cain.videotemp.video.listener

import com.cain.videotemp.video.Frame
import com.cain.videotemp.video.decoder.BaseDecoder

/**
 * @author : jiangyu
 * @date   : 2020/8/15
 * @desc   : 解码状态回调接口
 */
interface IDecoderStateListener {
    fun decoderPrepare(decodeJob: BaseDecoder?)
    fun decoderReady(decodeJob: BaseDecoder?)
    fun decoderRunning(decodeJob: BaseDecoder?)
    fun decoderPause(decodeJob: BaseDecoder?)
    fun decodeOneFrame(decodeJob: BaseDecoder?, frame: Frame)
    fun decoderFinish(decodeJob: BaseDecoder?)
    fun decoderDestroy(decodeJob: BaseDecoder?)
    fun decoderError(decodeJob: BaseDecoder?, msg: String)
}