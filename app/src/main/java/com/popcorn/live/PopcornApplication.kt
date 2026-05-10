package com.popcorn.live

import android.app.Application
import com.popcorn.live.di.AppContainer

class PopcornApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
