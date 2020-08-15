package com.cain.videotemp.video

import android.media.MediaCodec
import java.nio.ByteBuffer

/**
 * @author : jiangyu
 * @date   : 2020/8/15
 * @desc   : 一帧数据
 */
class Frame {
    var buffer: ByteBuffer? = null
    var bufferInfo = MediaCodec.BufferInfo()
        private set

    fun setBufferInfo(info: MediaCodec.BufferInfo) {
        bufferInfo.set(info.offset, info.size, info.presentationTimeUs, info.flags)
    }
}