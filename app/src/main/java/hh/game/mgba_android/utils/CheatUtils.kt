package hh.game.mgba_android.utils

import android.content.Context
import android.util.Log
import com.blankj.utilcode.util.FileIOUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class CheatUtils {
    companion object {
        fun generateCheat(
            context: Context,
            gameNum: String?,
            externalcheatfile: String? = null
        ): Boolean {
            gameNum?.apply {
                Log.d("gamecode::",gameNum)
                var internalCheatFile =
                    context.getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cht"
                if (!File(internalCheatFile).exists()) {
                    try {
                        if (externalcheatfile == null) {
                            var cheatfromFile = context.assets.open("gbacheats/$gameNum.cht")
                            var cheat =
                                CheatUtils().convertECcodestoVba(cheatfromFile, false)
                                    .toString()
                            saveCheatToFile(context, gameNum, cheat)
                            Log.d("thecheat:::", cheat)
                            return true
                        } else
                            saveCheatToFile(
                                context,
                                gameNum,
                                File(externalcheatfile).inputStream().convertToString()
                            )
                    } catch (e: IOException) {
                        return false
                    }
                } else return true
            }
            return false
        }

        fun FileInputStream.convertToString(): String {
            val bufferedReader = BufferedReader(InputStreamReader(this))
            val stringBuilder = StringBuilder()

            var line: String? = bufferedReader.readLine()
            while (line != null) {
                stringBuilder.append(line + "\n")
                line = bufferedReader.readLine()
            }
            return stringBuilder.toString()
        }

        fun saveCheatToFile(context: Context, gameNum: String, str: String) {
            FileIOUtils.writeFileFromString(
                context.getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cht",
                str
            )
            FileIOUtils.writeFileFromString(
                context.getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats",
                str
            )
        }

        external fun memorySearch(searchValue:Int)

    }

    fun convertECcodestoVba(input: InputStream, allEnable: Boolean): GBACheat {
        val reader = BufferedReader(InputStreamReader(input, "GB2312"))
        var line: String?
        var currentTitle: String? = null
        var currentSubtitle: String? = null
        var gbaCheat = GBACheat()
        do {
            var cheat = Cheat()
            line = reader.readLine()
            if (line != null && !line.equals("")) {
                if (line.contains("[") && line.contains("]") && !line.contains("GameInfo",ignoreCase = true)) {
                    currentTitle = line.replace("[", "").replace("]", "")
                } else if (line.contains("=")) {
                    val parts = line.split("=")
                    currentSubtitle = parts[0]
                    when (currentSubtitle.toLowerCase()) {
                        "name" -> gbaCheat.gameTitle = parts[1]
                        "system" -> gbaCheat.gameSystem = when (parts[1].toLowerCase()) {
                            "gba" -> Gametype.GBA
                            "gbc" -> Gametype.GBC
                            else -> Gametype.GB
                        }

                        "text" -> gbaCheat.gameDes = parts[1]
                        else -> {
                            var cachecode = parts[1]
                            while (cachecode.endsWith(",")) {
                                cachecode += reader.readLine()
                            }
                            val data = convertEccodeToVba(cachecode)
                            cheat.isSelect = allEnable
                            cheat.cheatTitle = "# $currentTitle" + if (!currentSubtitle.equals(
                                    "ON",
                                    true
                                )
                            ) " $currentSubtitle"
                            else ""
                            cheat.cheatCode = data
                            gbaCheat.cheatlist?.add(cheat)
                        }
                    }
                }
            }
        } while (line != null)
        return gbaCheat
    }


    /**
     * Reference from ReGBA金手指编码转换器 V1.1 By Cynric
     */
    fun convertEccodeToVba(eccode: String): String {
        val lines = eccode.split("\n")
        var eccodeLine = lines[0]
        var retstr = ""
        var baseaddr = 0x2000000
        var codes = eccodeLine.split(";")
        for (code in codes) {
            retstr += convertoneCodeToVba(code)
        }
        return retstr
    }

    fun List<String>.splitToPairs(): List<Pair<List<String>, List<String>>> {
        return if (size < 2) emptyList()
        else zipWithNext()
            .map { (prev, curr) -> prev.split(",").toList() to curr.split(",").toList() }
    }

    fun convertoneCodeToVba(code: String): String {
        var baseaddr = 0x2000000
        val values = code.split(",")
        var baseAddress = 0
        var address = ""
        var result = ""
        if (values[0].isNotBlank()) {
            var offset = values[0].toInt(16)
            var valueList = values.drop(1).chunked(4)
            valueList.forEachIndexed { index, strings ->
                var firstbit: Int
                val valStr = strings[0].trim()
                when {
                    valStr.length <= 2 -> firstbit = 0
                    valStr.length <= 4 -> firstbit = 0x10000000
                    valStr.length <= 6 -> firstbit = 0x20000000
                    else -> firstbit = 0x30000000
                }
                baseAddress = (firstbit or baseaddr or offset) + (index * 4)
                address = baseAddress.toString(16).padStart(8, '0').toUpperCase()
                result += "$address:${
                    strings.reversed().joinToString("").let {
                        when {
                            it.length <= 2 -> it.padStart(2, '0')
                            it.length in 3..4 -> it.padStart(4, '0')
                            it.length in 5..8 -> it.padStart(8, '0')
                            else -> it 
                        }

                    }

                }\n"
            }
        }
        return result
    }

}