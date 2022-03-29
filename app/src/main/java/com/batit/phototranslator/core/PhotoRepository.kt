package com.batit.phototranslator.core

import com.batit.phototranslator.core.db.PhotoItem
import com.batit.phototranslator.core.db.TranslatorDatabase

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