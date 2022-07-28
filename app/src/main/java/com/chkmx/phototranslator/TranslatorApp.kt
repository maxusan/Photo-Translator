package com.chkmx.phototranslator

import android.app.Application
import com.chkmx.phototranslator.core.db.TranslatorDatabase

class TranslatorApp: Application() {

    override fun onCreate() {
        super.onCreate()
        TranslatorDatabase.initPhotoDB(this)
    }

}