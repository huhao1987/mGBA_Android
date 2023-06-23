package hh.game.mgba_android.utils

import com.blankj.utilcode.util.ResourceUtils
import hh.game.mgba_android.data.ShaderData
import hh.game.mgba_android.mGBAApplication
import java.io.BufferedReader
import java.io.InputStreamReader

class ShaderUtils {
    companion object {
        fun getShaderList() =
            mGBAApplication.context.assets.list("shaders")

        fun getShader(isAssets: Boolean = true) {
            if (isAssets) {
                var shaderPath = "lcd.shader"
                val reader = BufferedReader(
                    InputStreamReader(
                        mGBAApplication.context.assets.open(shaderPath + "/manifest.ini"),
                        "GB2312"
                    )
                )
                var shader = ShaderData.Shader()
                var passes = 0
                reader.readLines().forEach { line ->
                    when {
                        "name=" in line -> {
                            shader.name = line.replace("name=","")
                        }

                        "author=" in line -> {
                            shader.author = line.replace("author=","")
                        }

                        "description=" in line -> {
                            shader.description = line.replace("description=","")
                        }
                        "passes=" in line -> {
                            passes = line.replace("passes=","").toInt()
                            shader.passes = passes
                        }
                    }
                }
                var shaderData = ShaderData(shader)
            }
        }
    }
}