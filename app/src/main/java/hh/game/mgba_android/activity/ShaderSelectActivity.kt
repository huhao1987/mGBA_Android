package hh.game.mgba_android.activity

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import hh.game.mgba_android.R
import hh.game.mgba_android.utils.ShaderUtils

class ShaderSelectActivity : AppCompatActivity() {
    private var sharepreferences: SharedPreferences? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shader_select)
        sharepreferences = getSharedPreferences("mGBA", Context.MODE_PRIVATE)
        sharepreferences?.getString("shaderpath","")
        ShaderUtils.getShaderList()?.toList()?.forEach {
            Log.d("theshader::",it)
        }
    }
}