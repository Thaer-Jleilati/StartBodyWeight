package com.epsilon.startbodyweight.data

class DBHelperUtil {
    companion object {

        fun nukeDatabase(db: RoomDB?) {
            db?.Dao()?.nukeTable()
        }

        fun isDbInitialized(db: RoomDB?) : Boolean {
            return db?.Dao()?.numRows() ?: 0 > 0
        }
    }
}