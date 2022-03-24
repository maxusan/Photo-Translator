package com.batit.phototranslator.ui.start

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.LruCache
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.batit.phototranslator.core.TranslatedText
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.core.data.LanguageProvider
import com.batit.phototranslator.core.util.SmoothedMutableLiveData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class StartViewModel : ViewModel() {

    private val availableLanguages: List<Language> = LanguageProvider.getLanguages()
    fun getLanguages() = availableLanguages

    private val primaryLanguage = MutableLiveData<Language>()
    fun setPrimaryLanguage(language: Language) {
        primaryLanguage.postValue(language)
    }

    fun getPrimaryLanguage() = primaryLanguage

    private val secondaryLanguage = MutableLiveData<Language>()
    fun setSecondaryLanguage(language: Language) {
        secondaryLanguage.postValue(language)
    }

    fun getSecondaryLanguage() = secondaryLanguage

    private val detector = FirebaseVision.getInstance().cloudTextRecognizer
    private lateinit var image: FirebaseVisionImage

    private val languageState = MutableLiveData(LanguageState.PRIMARY)
    fun setLanguageState(state: LanguageState){
        languageState.postValue(state)
    }
    fun getLanguageState() = languageState

    fun detectText(bitmap: Bitmap, callback: (FirebaseVisionText) -> Unit) {
        image = FirebaseVisionImage.fromBitmap(bitmap)
        detector.processImage(image).addOnSuccessListener { firebaseVisionText ->
            callback(firebaseVisionText)
        }.addOnFailureListener { e ->
            e.printStackTrace()
        }
    }

    fun translateText(
        firebaseVisionText: FirebaseVisionText,
        source: String,
        target: String,
        callback: (List<TranslatedText>) -> Unit
    ) {
        val translatedTextList = mutableListOf<TranslatedText>()
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                firebaseVisionText.textBlocks.forEach { textBlock ->
                    textBlock.lines.forEachIndexed { index, line ->
                        translator.translate(line.text).addOnCompleteListener {
                            translatedTextList.add(
                                TranslatedText(
                                    text = it.result,
                                    boundingBox = line.boundingBox ?: Rect()
                                )
                            )
                            if(index == textBlock.lines.size - 1){
                                callback(translatedTextList)
                            }
                        }
                    }
                }
            }.addOnFailureListener {
                it.printStackTrace()
            }
    }
}