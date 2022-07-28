package com.chkmx.phototranslator.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PhotoItem::class], version = 1)
abstract class TranslatorDatabase: RoomDatabase() {

    abstract fun getPhotoDao(): PhotoDao

    companion object{

        lateinit var instance: TranslatorDatabase

        fun initPhotoDB(context: Context){
            instance = Room.databaseBuilder(
                context,
                TranslatorDatabase::class.java,
                "Photos"
            ).build()
        }
    }

}