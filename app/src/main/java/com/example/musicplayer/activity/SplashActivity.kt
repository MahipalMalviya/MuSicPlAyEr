package com.example.musicplayer.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import com.example.musicplayer.R

/**
 * Created by MAHIPAL-PC on 24-12-2017.
 */

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)

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

        private const val SPLASH_TIME_OUT = 2000L
    }
}
