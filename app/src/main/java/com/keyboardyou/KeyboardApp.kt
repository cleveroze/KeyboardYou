package com.keyboardyou

import android.app.Application
import com.keyboardyou.data.ClipboardRepository
import com.keyboardyou.data.SettingsRepository
import com.keyboardyou.data.WordRepository

class KeyboardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(this)
        ClipboardRepository.init(this)
        WordRepository.init(this)
    }
}
