package com.example.musicplayer

import android.app.Application
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

class MusicApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
    }
}