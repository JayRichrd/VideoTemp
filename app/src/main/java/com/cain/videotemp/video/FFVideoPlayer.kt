package com.cain.videotemp.video

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import com.cain.videotemp.R

/**
 * @author cainjiang
 * @date 2020/5/6
 */
class FFVideoPlayer(ctx: Context, attrs: AttributeSet?, defStyle: Int) : SurfaceView(ctx, attrs, defStyle) {
    companion object {
        const val TAG = "FFVideoPlayer"
    }

    var playUrl: String = ""

    constructor(ctx: Context) : this(ctx, null)
    constructor(ctx: Context, attrs: AttributeSet?) : this(ctx, attrs, 0)

    init {
        initView(ctx, attrs)
    }

    private fun initView(ctx: Context, attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = ctx.obtainStyledAttributes(attrs, R.styleable.FFVideoPlayer)
            playUrl = typedArray.getString(R.styleable.FFVideoPlayer_play_url) ?: ""
            typedArray.recycle()
        }
    }

    fun play() {
        Thread(Runnable {
            Log.d(TAG, "play# playUrl: $playUrl")
            if (playUrl.isNotEmpty()) {
                render(playUrl, this@FFVideoPlayer.holder.surface)
            }
        }).start()
    }

    /**
     * 渲染
     */
    private external fun render(playUrl: String, surfaceView: Surface)
}