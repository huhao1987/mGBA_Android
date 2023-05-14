package hh.game.mgba_android.utils

import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

class GBAcheatUtils {
    fun convertECcodestoVba(input: InputStream): GBACheat {
        val reader = BufferedReader(InputStreamReader(input, "GB2312"))
        var line: String?
        var currentTitle: String? = null
        var currentSubtitle: String? = null
        var gbaCheat = GBACheat()
        do {
            var cheat = Cheat()
            line = reader.readLine()
            if (line != null) {
                if (line.startsWith("[") && line.endsWith("]") && !line.contains("GameInfo")) {
                    currentTitle = line.substring(1, line.length - 1)
                } else if (line.contains("=")) {
                    val parts = line.split("=")
                    currentSubtitle = parts[0]
                    when (currentSubtitle) {
                        "Name" -> gbaCheat.gameTitle = parts[1]
                        "System" -> gbaCheat.gameSystem = when (parts[1]) {
                            "GBA" -> Gametype.GBA
                            else -> Gametype.GB
                        }

                        "Text" -> gbaCheat.gameDes = parts[1]
                        else -> {
                            val data = convertEccodeToVba(parts[1])
                            cheat.cheatTitle = "#$currentTitle" + if (!currentSubtitle.equals(
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
     * Covert from ReGBA金手指编码转换器 V1.1 By Cynric
     */
    fun convertEccodeToVba(eccode: String): String {
        val lines = eccode.split("\n")
        var eccodeLine = lines[0]
        var retstr = ""
        var baseaddr = 0x2000000
        var codes = eccodeLine.split(";")
        for (code in codes) {
            if (code.startsWith("[")) {
                retstr += code + "\n"
                continue
            }
            val values = code.split(",")
            if (values[0].isNotBlank()) {
                var offset = values[0].toInt(16)
                for (j in 1 until values.size) {
                    var firstbit: Int
                    val valStr = values[j]
                    val valInt = valStr.toInt(16)
                    when {
                        valStr.length <= 2 -> firstbit = 0
                        valStr.length <= 4 -> firstbit = 0x10000000
                        valStr.length <= 6 -> firstbit = 0x20000000
                        else -> firstbit = 0x30000000
                    }
                    val address = firstbit or baseaddr or offset
                    retstr += address.toString(16).padStart(8, '0').toUpperCase() + " " +
                            Integer.toHexString(valInt).padStart(8, '0').toUpperCase() + "\n"
                    if (valStr.length <= 2) {
                        offset += 1
                    } else if (valStr.length <= 4) {
                        offset += 2
                    } else if (valStr.length <= 6) {
                        offset += 3
                    } else if (valStr.length <= 8) {
                        offset += 4
                    }
                }
            }
        }
        return retstr
    }

}