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

        return languageList
    }

}