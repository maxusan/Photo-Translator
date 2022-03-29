package com.batit.phototranslator.core.db

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.batit.phototranslator.BR
import java.util.*

@Entity
data class PhotoItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val photoName: String = "",
    val photoUri: String = "",
    val dateAdded: String = ""
): BaseObservable() {

    @Ignore
    @Bindable
    var photoPicked: Boolean = false
    fun setPhotoPicked(){
        photoPicked = !photoPicked
        notifyPropertyChanged(BR.photoPicked)
    }

}