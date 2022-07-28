package com.chkmx.phototranslator.core

import com.chkmx.phototranslator.core.db.PhotoItem
import com.chkmx.phototranslator.core.db.TranslatorDatabase

object PhotoRepository {

    private val dao = TranslatorDatabase.instance.getPhotoDao()

    suspend fun insertPhoto(photoItem: PhotoItem){
        dao.insertPhoto(photoItem)
    }

    suspend fun deletePhoto(photoItem: PhotoItem){
        dao.deletePhoto(photoItem)
    }

    fun getPhotos() = dao.getPhotos()

}