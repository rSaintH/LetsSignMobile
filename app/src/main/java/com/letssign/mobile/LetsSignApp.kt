package com.letssign.mobile

import android.app.Application
import com.letssign.mobile.di.AppContainer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class LetsSignApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        container = AppContainer(this)
    }
}
