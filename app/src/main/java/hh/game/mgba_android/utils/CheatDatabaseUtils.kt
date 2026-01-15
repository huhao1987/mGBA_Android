package hh.game.mgba_android.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class CheatDbEntry(
    val desc: String,
    val code: String,
    val enabled: Boolean = false
)

object CheatDatabaseUtils {
    private const val DB_NAME = "gbaCheat.dat"
    
    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {
        private val dbContext = context

        override fun onCreate(db: SQLiteDatabase?) {
            // DB is pre-packaged, onCreate shouldn't strictly be called if we copy successfully, 
            // but empty impl is standard for packaged DBs usually unless we create schema here.
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            // detailed upgrade logic if needed
        }
        
        fun prepareDatabase() {
            val dbFile = dbContext.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                try {
                    val dbDir = dbFile.parentFile
                    if (!dbDir.exists()) {
                        dbDir.mkdirs()
                    }
                    
                    val inputStream = dbContext.assets.open(DB_NAME)
                    val outputStream = FileOutputStream(dbFile)
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    Log.d("CheatDatabaseUtils", "Database copied from assets to ${dbFile.absolutePath}")
                } catch (e: IOException) {
                    Log.e("CheatDatabaseUtils", "Error copying database", e)
                }
            }
        }
    }

    private var dbHelper: DatabaseHelper? = null

    @Synchronized
    private fun getDb(context: Context): SQLiteDatabase? {
        try {
            val externalDb = File(context.getExternalFilesDir(null), DB_NAME)
            if (externalDb.exists()) {
                Log.d("CheatDatabaseUtils", "Loading cheats from external file: ${externalDb.absolutePath}")
                return SQLiteDatabase.openDatabase(externalDb.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            }
        } catch (e: Exception) {
            Log.e("CheatDatabaseUtils", "Error checking external DB", e)
        }

        if (dbHelper == null) {
            dbHelper = DatabaseHelper(context)
            dbHelper?.prepareDatabase()
        }
        return try {
            dbHelper?.readableDatabase
        } catch (e: Exception) {
            Log.e("CheatDatabaseUtils", "Error opening database", e)
            null
        }
    }

    fun getCheatsForGame(context: Context, gameNum: String, searchDir: File? = null): List<CheatDbEntry>? {
        var db: SQLiteDatabase? = null
        var isCustomDb = false

        // 1. Try Custom Directory (e.g. Game ROM folder)
        if (searchDir != null) {
            try {
                val customEntry = File(searchDir, DB_NAME)
                if (customEntry.exists()) {
                     Log.d("CheatDatabaseUtils", "Found DB in game dir: ${customEntry.absolutePath}")
                     db = SQLiteDatabase.openDatabase(customEntry.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                     isCustomDb = true
                }
            } catch (e: Exception) {
                Log.e("CheatDatabaseUtils", "Error checking custom DB path", e)
            }
        }

        // 2. Fallback to internal/assets
        if (db == null) {
             db = getDb(context)
        }
        
        if (db == null) return null
        
        var gameId: String? = null
        
        try {
            
            // 1. Direct game_id check
            db.rawQuery("SELECT game_id FROM games WHERE game_id = ?", arrayOf(gameNum)).use { cursor ->
                if (cursor.moveToFirst()) {
                    gameId = cursor.getString(0)
                }
            }
            
            gameId?.let { return queryCheats(db, it) }
            
            // 2 & 3. Serial lookup
            gameId = resolveSerialToId(db, gameNum)
            gameId?.let { return queryCheats(db, it) }
        } catch (e: Exception) {
             Log.e("CheatDatabaseUtils", "Error querying cheats for $gameNum", e)
        } finally {
            if (isCustomDb) {
                try { db.close() } catch (e: Exception) {}
            }
        }

        return null
    }

    private fun resolveSerialToId(db: SQLiteDatabase, serial: String): String? {
        val keysToTry = listOf(
            serial,
            serial.toUpperCase(),
            "AGB-${serial.toUpperCase()}-JPN",
            "AGB-${serial.toUpperCase()}-USA",
            "AGB-${serial.toUpperCase()}-EUR"
        )
        
        for (key in keysToTry) {
            try {
                db.rawQuery("SELECT game_id FROM mappings WHERE serial = ?", arrayOf(key)).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getString(0)
                    }
                }
            } catch (e: Exception) {
                // Continue to next key
            }
        }
        return null
    }
    
    private fun queryCheats(db: SQLiteDatabase, gameId: String): List<CheatDbEntry> {
        val list = ArrayList<CheatDbEntry>()
        try {
            db.rawQuery("SELECT desc, code, enabled FROM cheats WHERE game_id = ?", arrayOf(gameId)).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        CheatDbEntry(
                            desc = cursor.getString(0),
                            code = cursor.getString(1),
                            enabled = cursor.getInt(2) == 1
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("CheatDatabaseUtils", "Error processing cheats query", e)
        }
        return list
    }

    fun getGameIdForSerial(context: Context, serial: String): String? {
        val db = getDb(context) ?: return null
        return resolveSerialToId(db, serial)
    }

    fun getGameName(context: Context, gameNum: String): String? {
        val db = getDb(context) ?: return null
        
        try {
            // Direct ID check first
            db.rawQuery("SELECT name FROM games WHERE game_id = ?", arrayOf(gameNum)).use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
            
            // Try mapped serial
            val mappedId = resolveSerialToId(db, gameNum)
            if (mappedId != null) {
                db.rawQuery("SELECT name FROM games WHERE game_id = ?", arrayOf(mappedId)).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getString(0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CheatDatabaseUtils", "Error getting game name", e)
        }
        
        return null
    }
    
    fun convertToCheatList(dbEntries: List<CheatDbEntry>): ArrayList<Cheat> {
        val list = ArrayList<Cheat>()
        dbEntries.forEach { 
            val cheat = Cheat()
            cheat.cheatTitle = it.desc
            cheat.cheatCode = it.code
            cheat.isSelect = it.enabled
            list.add(cheat)
        }
        return list
    }
}
