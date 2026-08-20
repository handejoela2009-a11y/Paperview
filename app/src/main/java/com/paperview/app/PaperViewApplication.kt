package com.paperview.app

import android.app.Application
import com.paperview.app.data.PreferencesRepository

class PaperViewApplication : Application() {
    lateinit var repository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = PreferencesRepository(this)
    }
}
