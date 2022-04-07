package com.batit.phototranslator.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.StrictMode
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batit.phototranslator.R
import com.batit.phototranslator.core.PhotoRepository
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.core.data.LanguageProvider
import com.batit.phototranslator.core.data.TranslatedText
import com.batit.phototranslator.core.db.PhotoItem
import com.batit.phototranslator.ui.start.LanguageState
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.RecognizedLanguage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


class MainViewModel : ViewModel() {
    private lateinit var trans: Translate
    private var modelDownloadTask: Task<Void> = Tasks.forCanceled()

    private val modelDownloading = MutableLiveData<Boolean>(true)
    fun setModelDownloading(downloading: Boolean) {
        modelDownloading.postValue(downloading)
    }

    private var translating: Boolean = false

    private val translators =
        object : LruCache<TranslatorOptions, Translator>(1) {
            override fun create(options: TranslatorOptions): Translator {
                return Translation.getClient(options)
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: TranslatorOptions,
                oldValue: Translator,
                newValue: Translator?
            ) {
                oldValue.close()
            }
        }

    fun getModelDownloading() = modelDownloading

    private val availableLanguages: List<Language> = LanguageProvider.getLanguages()
    fun getLanguages() = availableLanguages

    private val primaryLanguage = MutableLiveData<Language>()
    fun setPrimaryLanguage(language: Language) {
        primaryLanguage.value = language

    }

    fun getPrimaryLanguage() = primaryLanguage

    private val secondaryLanguage = MutableLiveData<Language>()
    fun setSecondaryLanguage(language: Language) {
        secondaryLanguage.value = language
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


    fun translateText(
        context: Context,
        firebaseVisionText: FirebaseVisionText,
        source: String,
        target: String,
        callback: (List<TranslatedText>) -> Unit
    ) {
        setModelDownloading(true)
        val translatedTextList = mutableListOf<TranslatedText>()
        getTranslateService(context)
        firebaseVisionText.textBlocks.forEach { textBlock ->
            textBlock.lines.forEachIndexed { index, line ->
                viewModelScope.launch(Dispatchers.IO) {
                    translateText( source, target, line.text) {
                        translatedTextList.add(
                            TranslatedText(
                                text = it,
                                boundingBox = line.boundingBox ?: Rect()
                            )
                        )
                        if (index == textBlock.lines.size - 1) {
                            setModelDownloading(false)
                            callback(translatedTextList)

                        }
                    }
                }


            }
        }
    }

    private fun getTranslateService(context: Context) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            context.resources.openRawResource(R.raw.credentials).use { `is` ->
                val myCredentials = GoogleCredentials.fromStream(`is`)
                val translateOptions =
                    TranslateOptions.newBuilder().setCredentials(myCredentials).build()
                trans = translateOptions.service
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }

    private suspend fun translateText(
        source: String,
        target: String,
        text: String,
        callback: (String) -> Unit
    ) {
        kotlin.runCatching {
            val translation: com.google.cloud.translate.Translation = trans.translate(
                text,
                Translate.TranslateOption.targetLanguage(target),
                if (source != Language.getDefaultLanguage().code) Translate.TranslateOption.sourceLanguage(
                    source
                ) else Translate.TranslateOption.model("base")
            )
            withContext(Dispatchers.Main) {
                callback(translation.translatedText)
            }
        }.exceptionOrNull()?.printStackTrace()

    }

    private val _startMainEvent = LiveEvent<Boolean>()

    private val _openDrawerEvent = LiveEvent<Boolean>()
    val openDrawerEvent: LiveData<Boolean> = _openDrawerEvent
    fun openDrawer() {
        _openDrawerEvent.postValue(true)
    }

    fun insertPhoto(photoItem: PhotoItem) {
        viewModelScope.launch {
            PhotoRepository.insertPhoto(photoItem)
        }
    }

    fun deletePhoto(photoItem: PhotoItem) {
        viewModelScope.launch {
            PhotoRepository.deletePhoto(photoItem)
        }
    }

    fun getPhotos() = PhotoRepository.getPhotos()

    private val inDeleteLiveData = MutableLiveData(false)
    fun setInDelete(value: Boolean) {
        inDeleteLiveData.value = value
    }

    fun getInDelete() = inDeleteLiveData

    fun dismissTranslate() {
        translating = false
    }
}