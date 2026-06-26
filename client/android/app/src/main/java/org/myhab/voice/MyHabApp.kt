package org.myhab.voice

import android.app.Application
import org.myhab.voice.data.Session
import org.myhab.voice.voice.VoiceController

class MyHabApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Session.init(this)
        VoiceController.init(this)
    }
}
