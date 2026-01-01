package com.ediapp.dhammapada

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "tools.db"
        private const val DATABASE_VERSION = 1

        // tb_MEMOS 테이블
        const val TABLE_TOOLS = "tb_tools"
        const val TOOLS_COL_ID = "_id"
        const val TOOLS_COL_CATEGORY = "category"
        const val TOOLS_COL_WORD = "word"
        const val TOOLS_COL_MEANING = "meaning"
        const val TOOLS_COL_TIMESTAMP = "created_at"
        const val TOOLS_COL_REG_DATE = "reg_date"
        const val TOOLS_COL_URL = "url"
        const val TOOLS_COL_LAT = "lat"
        const val TOOLS_COL_LON = "lon"
        const val TOOLS_COL_ADDRESS = "address"
        const val TOOLS_COL_SIDO = "sido"
        const val TOOLS_COL_SIGUNGU = "sigungu"
        const val TOOLS_COL_EUPMYEONDONG = "eupmyeondong"

        private const val CREATE_TABLE_TOOLS =
            "CREATE TABLE $TABLE_TOOLS (" +
                    "$TOOLS_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$TOOLS_COL_CATEGORY TEXT," +
                    "$TOOLS_COL_WORD TEXT," +
                    "$TOOLS_COL_MEANING TEXT," +
                    "$TOOLS_COL_TIMESTAMP INTEGER," +
                    "$TOOLS_COL_REG_DATE INTEGER," +
                    "$TOOLS_COL_URL TEXT," +
                    "$TOOLS_COL_LAT REAL," +
                    "$TOOLS_COL_LON REAL," +
                    "$TOOLS_COL_ADDRESS TEXT," +
                    "$TOOLS_COL_SIDO TEXT," +
                    "$TOOLS_COL_SIGUNGU TEXT," +
                    "$TOOLS_COL_EUPMYEONDONG TEXT" +
                    ")"

        // tb_MEMOS 테이블
        const val TABLE_KEYWORDS = "tb_keywords"
        const val KEYWORDS_COL_ID = "_id"
        const val KEYWORDS_COL_KEYWORD = "keyword"
        const val TOOLS_COL_MYWORD_ID = "tools_id"

        private const val CREATE_TABLE_KEYWORDS =
            "CREATE TABLE $TABLE_KEYWORDS (" +
                    "$KEYWORDS_COL_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$KEYWORDS_COL_KEYWORD TEXT," +
                    "$TOOLS_COL_MYWORD_ID INTEGER," +
                    "FOREIGN KEY($TOOLS_COL_MYWORD_ID) REFERENCES $TABLE_TOOLS($TOOLS_COL_ID)" +
                    ")"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_TOOLS)
        db.execSQL(CREATE_TABLE_KEYWORDS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
//            db.execSQL("ALTER TABLE $TABLE_TOOLS ADD COLUMN $TOOLS_COL_REG_DATE INTEGER")
        }
        // if (oldVersion < 3) {
        //     db.execSQL("ALTER TABLE $TABLE_TOOLS ADD COLUMN new_column_2 INTEGER DEFAULT 0")
        // }
    }
}
