package hh.game.mgba_android.data

data class ShaderData(
    var shader: Shader,
    var fragmentShaderList : ArrayList<String>?= null,
    var vertexShaderList : ArrayList<String>?= null
    ) {
    data class Shader(
        var name: String? = "",
        var author: String? = "",
        var description: String? = "",
        var passes: Int = 0,
        var passDetailList: ArrayList<PassDetail>? = ArrayList()
    )

    data class PassDetail(
        var fragmentShader: String? = "",
        var vertexShader: String? = "",
        var integerScaling: Int = 0,
        var blend: Int = 0,
        var width: Int = 0,
        var height: Int = 0,
        var uniform: ArrayList<PassUniform>? = null
    )

    data class PassUniform(
        var type: String? = "float",
        var typeNum: Int? = 0,
        var readableName: String? = "",
        var default: ArrayList<String>? = ArrayList(),
        var min: Double = 0.0,
        var max: Double = 0.0
    )

}

