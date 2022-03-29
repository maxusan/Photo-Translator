package com.batit.phototranslator

import android.app.Application
import com.batit.phototranslator.core.db.TranslatorDatabase

class TranslatorApp: Application() {

    override fun onCreate() {
        super.onCreate()
        TranslatorDatabase.initPhotoDB(this)
    }

}