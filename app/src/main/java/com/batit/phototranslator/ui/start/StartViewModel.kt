package com.batit.phototranslator.ui.start

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.StrictMode
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batit.phototranslator.R
import com.batit.phototranslator.core.data.Language
import com.batit.phototranslator.core.data.LanguageProvider
import com.batit.phototranslator.core.data.TranslatedText
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.RecognizedLanguage
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class StartViewModel : ViewModel() {
    private var modelDownloadTask: Task<Void> = Tasks.forCanceled()

    private lateinit var trans: Translate

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
    }

    fun getPrimaryLanguage() = primaryLanguage

    private val secondaryLanguage = MutableLiveData<Language>()
    fun setSecondaryLanguage(language: Language) {
        secondaryLanguage.value = language
    }

    fun getSecondaryLanguage() = secondaryLanguage

    private lateinit var image: FirebaseVisionImage

    private val languageState = MutableLiveData(LanguageState.PRIMARY)
    fun setLanguageState(state: LanguageState) {
        languageState.postValue(state)
    }

    fun getLanguageState() = languageState

    fun detectText(bitmap: Bitmap, callback: (FirebaseVisionText) -> Unit) {
        image = FirebaseVisionImage.fromBitmap(bitmap)
        val detector = FirebaseVision.getInstance().cloudTextRecognizer
        detector.processImage(image).addOnSuccessListener { firebaseVisionText ->
            callback(firebaseVisionText)
        }.addOnFailureListener { e ->
            e.printStackTrace()
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

    suspend fun translateText(
        source: String,
        target: String,
        text: String,
        callback: (String) -> Unit
    ) {

        val translation: Translation = trans.translate(
            text,
            Translate.TranslateOption.targetLanguage(target),
            if (source != Language.getDefaultLanguage().code) Translate.TranslateOption.sourceLanguage(
                source
            ) else Translate.TranslateOption.model("base")
        )
        withContext(Dispatchers.Main) {
            callback(translation.translatedText)
        }
    }


    private val _startMainEvent = LiveEvent<Boolean>()
    val startMainEvent: LiveData<Boolean> = _startMainEvent
    fun startMain() {
        _startMainEvent.postValue(true)
    }
}
