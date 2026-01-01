package com.ediapp.dhammapada

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ediapp.dhammapada.data.DhammapadaItem
import org.json.JSONArray
import java.io.IOException

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "myapp2.db"
        private const val DATABASE_VERSION = 2

        // tb_MEMOS 테이블
        const val TABLE_LISTS = "tb_lists"
        const val LISTS_COL_ID = "_id"
        const val LISTS_COL_CATEGORY = "category"
        const val LISTS_COL_TITLE = "title"
        const val LISTS_COL_CONTENT = "content"
        const val LISTS_COL_TIMESTAMP = "created_at"
        const val LISTS_COL_REG_DATE = "reg_date"
        const val LISTS_COL_WRITE_DATE = "write_date"
        const val LISTS_COL_URL = "url"

        const val LISTS_COL_READ_COUNT = "read_count"
        const val LISTS_COL_READ_TIME = "read_time"
        const val LISTS_COL_STATUS = "status"

        private const val CREATE_TABLE_LISTS =
            "CREATE TABLE $TABLE_LISTS (" +
                    "$LISTS_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$LISTS_COL_CATEGORY TEXT," +
                    "$LISTS_COL_TITLE TEXT," +
                    "$LISTS_COL_CONTENT TEXT," +
                    "$LISTS_COL_TIMESTAMP INTEGER," +
                    "$LISTS_COL_REG_DATE INTEGER," +
                    "$LISTS_COL_WRITE_DATE INTEGER DEFAULT 0," +
                    "$LISTS_COL_URL TEXT," +
                    "$LISTS_COL_READ_COUNT INT," +
                    "$LISTS_COL_READ_TIME INTEGER," +
                    "$LISTS_COL_STATUS TEXT " +
                    ")"

    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_LISTS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_LISTS ADD COLUMN $LISTS_COL_WRITE_DATE INTEGER DEFAULT 0")
        }
    }

    fun insertInitialData(context: Context) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            val jsonString = context.assets.open("dhammapada_data.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val values = ContentValues().apply {
                    put(LISTS_COL_CATEGORY, "법구경")
                    put(LISTS_COL_TITLE, jsonObject.getString("title"))
                    put(LISTS_COL_CONTENT, jsonObject.getString("body"))
                    put(LISTS_COL_URL, jsonObject.optString("url", ""))
                    put(LISTS_COL_TIMESTAMP, System.currentTimeMillis())
                    put(LISTS_COL_REG_DATE, System.currentTimeMillis())
                    put(LISTS_COL_WRITE_DATE, 0)
                    put(LISTS_COL_READ_COUNT, 0)
                    put(LISTS_COL_READ_TIME, 0)
                    put(LISTS_COL_STATUS, "unread")
                }
                db.insert(TABLE_LISTS, null, values)
            }
            db.setTransactionSuccessful()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun getNextItem(readIndex: Int): DhammapadaItem? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_LISTS WHERE $LISTS_COL_ID > ? ORDER BY $LISTS_COL_ID ASC LIMIT 1", arrayOf(readIndex.toString()))
        var item: DhammapadaItem? = null
        if (cursor.moveToFirst()) {
            item = DhammapadaItem(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(LISTS_COL_ID)),
                category = cursor.getString(cursor.getColumnIndexOrThrow(LISTS_COL_CATEGORY)),
                title = cursor.getString(cursor.getColumnIndexOrThrow(LISTS_COL_TITLE)),
                content = cursor.getString(cursor.getColumnIndexOrThrow(LISTS_COL_CONTENT)),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(LISTS_COL_TIMESTAMP)),
                regDate = cursor.getLong(cursor.getColumnIndexOrThrow(LISTS_COL_REG_DATE)),
                writeDate = cursor.getLong(cursor.getColumnIndexOrThrow(LISTS_COL_WRITE_DATE)),
                url = cursor.getString(cursor.getColumnIndexOrThrow(LISTS_COL_URL)),
                readCount = cursor.getInt(cursor.getColumnIndexOrThrow(LISTS_COL_READ_COUNT)),
                readTime = cursor.getLong(cursor.getColumnIndexOrThrow(LISTS_COL_READ_TIME)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(LISTS_COL_STATUS))
            )
        }
        cursor.close()
        return item
    }

    fun updateReadStatus(id: Long?) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(LISTS_COL_READ_TIME, System.currentTimeMillis())
        }
        db.execSQL("UPDATE $TABLE_LISTS SET $LISTS_COL_READ_COUNT = $LISTS_COL_READ_COUNT + 1, $LISTS_COL_READ_TIME = ? WHERE $LISTS_COL_ID = ?", arrayOf(System.currentTimeMillis().toString(), id.toString()))
    }


    fun getAllLists(): List<DhammapadaItem> {
        val items = mutableListOf<DhammapadaItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_LISTS", null)

        if (cursor.moveToFirst()) {
            do {
                val item = DhammapadaItem(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(LISTS_COL_ID)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(LISTS_COL_CATEGORY)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(LISTS_COL_TITLE)),
                    content = cursor.getString(cursor.getColumnIndexOrThrow(LISTS_COL_CONTENT)),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(LISTS_COL_TIMESTAMP)),
                    regDate = cursor.getLong(cursor.getColumnIndexOrThrow(LISTS_COL_REG_DATE)),
                    writeDate = cursor.getLong(cursor.getColumnIndexOrThrow(LISTS_COL_WRITE_DATE)),
                    url = cursor.getString(cursor.getColumnIndexOrThrow(LISTS_COL_URL)),
                    readCount = cursor.getInt(cursor.getColumnIndexOrThrow(LISTS_COL_READ_COUNT)),
                    readTime = cursor.getLong(cursor.getColumnIndexOrThrow(LISTS_COL_READ_TIME)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(LISTS_COL_STATUS))
                )
                items.add(item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return items
    }
}
