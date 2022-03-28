package com.batit.phototranslator.core.data

import com.batit.phototranslator.R
import com.google.mlkit.nl.translate.TranslateLanguage

object LanguageProvider {

    fun getLanguages(): List<Language>{
        val languageList = mutableListOf<Language>()

        languageList.add(
            Language(
                code = TranslateLanguage.CHINESE,
                icon = R.drawable.ic_china
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.RUSSIAN,
                icon = R.drawable.ic_russia
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.ENGLISH,
                icon = R.drawable.ic_england
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.FRENCH,
                icon = R.drawable.ic_france
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.GERMAN,
                icon = R.drawable.ic_germany
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.HINDI,
                icon = R.drawable.ic_india
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.ITALIAN,
                icon = R.drawable.ic_italy
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.SPANISH,
                icon = R.drawable.ic_spain
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.SWEDISH,
                icon = R.drawable.ic_sweden
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.THAI,
                icon = R.drawable.ic_thailand
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.UKRAINIAN,
                icon = R.drawable.ic_ukraine
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.PORTUGUESE,
                icon = R.drawable.ic_portuguese
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.KOREAN,
                icon = R.drawable.ic_korea
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.GREEK,
                icon = R.drawable.ic_greek
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.ESTONIAN,
                icon = R.drawable.ic_estonian
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.ESPERANTO,
                icon = R.drawable.ic_esperanto
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.ALBANIAN,
                icon = R.drawable.ic_albania
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.BELARUSIAN,
                icon = R.drawable.ic_belarus
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.POLISH,
                icon = R.drawable.ic_polish
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.SLOVAK,
                icon = R.drawable.ic_slovakia
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.SLOVENIAN,
                icon = R.drawable.ic_slovenia
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.AFRIKAANS,
                icon = R.drawable.ic_africa
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.HUNGARIAN,
                icon = R.drawable.ic_hungary
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.CZECH,
                icon = R.drawable.ic_czech
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.GEORGIAN,
                icon = R.drawable.ic_georgia
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.HEBREW,
                icon = R.drawable.ic_hebrew
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.HAITIAN_CREOLE,
                icon = R.drawable.ic_haitian
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.DUTCH,
                icon = R.drawable.ic_haitian
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.CROATIAN,
                icon = R.drawable.ic_croatian
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.CATALAN,
                icon = R.drawable.ic_croatian
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.BULGARIAN,
                icon = R.drawable.ic_bulgaria
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.ARABIC,
                icon = R.drawable.ic_arabic
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.VIETNAMESE,
                icon = R.drawable.ic_vietnamese
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.JAPANESE,
                icon = R.drawable.ic_japan
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.KANNADA,
                icon = R.drawable.ic_kannada
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.IRISH,
                icon = R.drawable.ic_irish
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.TURKISH,
                icon = R.drawable.ic_turkish
            )
        )
        languageList.add(
            Language(
                code = TranslateLanguage.ICELANDIC,
                icon = R.drawable.ic_icelandic
            )
        )
        return languageList
    }

}