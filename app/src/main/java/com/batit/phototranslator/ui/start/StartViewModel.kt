package com.batit.phototranslator.ui.start

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.batit.phototranslator.core.util.Language
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.mlkit.nl.translate.TranslateLanguage

class StartViewModel : ViewModel() {

    private val availableLanguages: List<Language> = TranslateLanguage.getAllLanguages()
        .map { Language(it) }
    fun getLanguages() = availableLanguages

    private val detector = FirebaseVision.getInstance().cloudTextRecognizer
    private lateinit var image: FirebaseVisionImage

    fun detectText(bitmap: Bitmap, callback: (FirebaseVisionText) -> Unit) {
        image = FirebaseVisionImage.fromBitmap(bitmap)
        detector.processImage(image).addOnSuccessListener { firebaseVisionText ->
            callback(firebaseVisionText)
        }.addOnFailureListener { e ->
            e.printStackTrace()
        }
    }

    fun translateText(firebaseVisionText: FirebaseVisionText){

    }

}