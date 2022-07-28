package com.chkmx.phototranslator.core.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PhotoDao {

    @Insert
    suspend fun insertPhoto(photoItem: PhotoItem)

    @Delete
    suspend fun deletePhoto(photoItem: PhotoItem)

    @Query("SELECT * FROM photoitem")
    fun getPhotos(): LiveData<List<PhotoItem>>

}