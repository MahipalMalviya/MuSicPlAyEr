package com.example.musicplayer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity

/**
 * Created by MAHIPAL-PC on 24-12-2017.
 */

class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lay_splash_screen)

        runOnUiThread {
            Handler().postDelayed({
                val mainIntent = Intent(this, MainActivity::class.java)
                startActivity(mainIntent)
                finish()
            }, SPLASH_TIME_OUT)
        }
    }

    companion object {

        private const val SPLASH_TIME_OUT = 3000L
    }
}
