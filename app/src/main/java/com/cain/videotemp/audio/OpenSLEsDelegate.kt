package com.cain.videotemp.audio

import android.content.res.AssetManager

/**
 * @author : jiangyu
 * @date   : 2020/5/10
 * @desc   : xxx
 */
class OpenSLEsDelegate {
    /**
     * @param assetManager 资源管理器
     * @param fileName assets目录下的文件名
     */
    external fun playByAssets(assetManager: AssetManager, fileName: String)
}