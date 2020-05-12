package com.cain.videotemp

import android.app.ActivityManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cain.videotemp.pic.opengl.BitmapDrawer
import com.cain.videotemp.pic.opengl.SimpleRender
import kotlinx.android.synthetic.main.activity_simple_render.*

class SimpleRenderActivity : AppCompatActivity() {
    companion object {
        const val TAG = "SimpleRenderActivity"
    }

    private lateinit var drawer: BitmapDrawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_render)
        (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.let {
            Toast.makeText(this, "OpenGL ${it.deviceConfigurationInfo.glEsVersion}", Toast.LENGTH_LONG).show()
        }
        openGLPic()
    }

    override fun onDestroy() {
        super.onDestroy()
        drawer?.let { it.release() }
    }

    private fun openGLPic() {
        Log.i(TAG, "openGLPic###")
        drawer = BitmapDrawer(BitmapFactory.decodeResource(resources, R.mipmap.cover))
        initRender(drawer)
    }

    private fun initRender(drawer: BitmapDrawer) {
        gl_surface_view.setEGLContextClientVersion(2)
        val render = SimpleRender()
        render.addDrawer(drawer)
        gl_surface_view.setRenderer(render)
    }
}
