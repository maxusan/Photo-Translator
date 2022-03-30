package com.batit.phototranslator.ui

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batit.phototranslator.core.PhotoRepository
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.core.data.LanguageProvider
import com.batit.phototranslator.core.data.TranslatedText
import com.batit.phototranslator.core.db.PhotoItem
import com.batit.phototranslator.ui.start.LanguageState
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.RecognizedLanguage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.launch


class MainViewModel : ViewModel() {

    private var modelDownloadTask: Task<Void> = Tasks.forCanceled()

    private val modelDownloading = MutableLiveData<Boolean>(true)
    private fun setModelDownloading(downloading: Boolean) {
        modelDownloading.postValue(downloading)
    }

    fun getModelDownloading() = modelDownloading

    private val availableLanguages: List<Language> = LanguageProvider.getLanguages()
    fun getLanguages() = availableLanguages

    private val primaryLanguage = MutableLiveData<Language>()
    fun setPrimaryLanguage(language: Language) {
        primaryLanguage.value = language
        kotlin.runCatching {
            downloadModelIfNeed(getSecondaryLanguage().value!!.code, language.code)
        }
    }

    fun getPrimaryLanguage() = primaryLanguage

    private val secondaryLanguage = MutableLiveData<Language>()
    fun setSecondaryLanguage(language: Language) {
        secondaryLanguage.value = language
        kotlin.runCatching {
            downloadModelIfNeed(getPrimaryLanguage().value!!.code, language.code)
        }
    }

    fun getSecondaryLanguage() = secondaryLanguage

    private val detector = FirebaseVision.getInstance().cloudTextRecognizer
    val languageIdentifier = LanguageIdentification.getClient()
    private lateinit var image: FirebaseVisionImage

    private val languageState = MutableLiveData(LanguageState.PRIMARY)
    fun setLanguageState(state: LanguageState) {
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

    private fun downloadModelIfNeed(
        source: String,
        target: String
    ) {
        setModelDownloading(true)
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)
        modelDownloadTask = translator.downloadModelIfNeeded()
    }

    private fun detectLanguage(text: String, callback: (String) -> Unit) {
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    Log.i("logs", "Can't identify language.")
                } else {
                    callback(languageCode)
                }
            }
            .addOnFailureListener {
                // Model couldn’t be loaded or other internal error.
                // ...
            }

    }

    private fun detectLanguage(firebaseVisionText: FirebaseVisionText): RecognizedLanguage? {
        val languagesList = mutableListOf<RecognizedLanguage>()
        firebaseVisionText.textBlocks.forEach {
            val pop = getPopularElement(it.recognizedLanguages.toTypedArray())
            if (pop != null) {
                languagesList.add(pop)
            }
        }
        return getPopularElement(languagesList.toTypedArray())
    }

    private fun getPopularElement(a: Array<RecognizedLanguage>): RecognizedLanguage? {
        kotlin.runCatching {
            var count = 1
            var tempCount: Int
            var popular = a[0]
            var temp = a[0]
            for (i in 0 until a.size - 1) {
                temp = a[i]
                tempCount = 0
                for (j in 1 until a.size) {
                    if (temp == a[j]) tempCount++
                }
                if (tempCount > count) {
                    popular = temp
                    count = tempCount
                }
            }
            return popular
        }
        return null
    }

    fun translateText(
        firebaseVisionText: FirebaseVisionText,
        source: String,
        target: String,
        callback: (List<TranslatedText>) -> Unit
    ) {
        val options = if (source != Language.getDefaultLanguage().code) {
            TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build()
        } else {
            TranslatorOptions.Builder()
                .setSourceLanguage(detectLanguage(firebaseVisionText)?.languageCode ?: "en")
                .setTargetLanguage(target)
                .build()
        }
        val translatedTextList = mutableListOf<TranslatedText>()

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
                            if (index == textBlock.lines.size - 1) {
                                callback(translatedTextList)
                                setModelDownloading(false)
                            }
                        }
                    }
                }
            }.addOnFailureListener {
                it.printStackTrace()
            }
    }

    fun translateText(
        text: String,
        source: String,
        target: String,
        callback: (String) -> Unit
    ) {
        detectLanguage(text){
            val options = if (source != Language.getDefaultLanguage().code) {
                TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build()
            } else {
                TranslatorOptions.Builder()
                    .setSourceLanguage(it)
                    .setTargetLanguage(target)
                    .build()
            }

            val translator = Translation.getClient(options)
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    setModelDownloading(false)
                    translator.translate(text).addOnCompleteListener {
                        callback(it.result)
                    }
                }.addOnFailureListener {
                    it.printStackTrace()
                }
        }

    }

    private val _startMainEvent = LiveEvent<Boolean>()
    val startMainEvent: LiveData<Boolean> = _startMainEvent
    fun startMain() {
        _startMainEvent.postValue(true)
    }

    private val _openDrawerEvent = LiveEvent<Boolean>()
    val openDrawerEvent: LiveData<Boolean> = _openDrawerEvent
    fun openDrawer() {
        _openDrawerEvent.postValue(true)
    }

    private val _pickDocumentEvent = LiveEvent<Uri>()
    val pickDocumentEvent: LiveData<Uri> = _pickDocumentEvent
    fun pickDocument(uri: Uri){
        _pickDocumentEvent.value = uri
    }

    fun insertPhoto(photoItem: PhotoItem){
        viewModelScope.launch {
            PhotoRepository.insertPhoto(photoItem)
        }
    }

    fun deletePhoto(photoItem: PhotoItem){
        viewModelScope.launch {
            PhotoRepository.deletePhoto(photoItem)
        }
    }

    fun getPhotos() = PhotoRepository.getPhotos()

    private val inDeleteLiveData = MutableLiveData(false)
    fun setInDelete(value: Boolean){
        inDeleteLiveData.value = value
    }
    fun getInDelete() = inDeleteLiveData
}