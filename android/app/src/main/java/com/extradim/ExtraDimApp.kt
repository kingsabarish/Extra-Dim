package com.extradim

import android.app.Application
import com.extradim.di.AppContainer

class ExtraDimApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
