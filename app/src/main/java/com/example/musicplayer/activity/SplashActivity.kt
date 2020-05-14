package com.example.musicplayer.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.musicplayer.R
import java.util.ArrayList
import java.util.HashMap

/**
 * Created by MAHIPAL-PC on 24-12-2017.
 */

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.lay_splash_screen)

        if (isAppPermissionGrantedOrNot()) {
            requestAppPermissions()
            return
        }

        runOnUiThread {
            Handler().postDelayed({
                launchMp3Activity()
            }, SPLASH_TIME_OUT)
        }
    }

    private fun launchMp3Activity() {
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
        finish()
    }

    private fun requestAppPermissions() {
        //check which permission granted
        val listPermissionNeeded = ArrayList<String>()
        for (permission in permissionList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(permission)
            }
        }

        //ask for non-permission granted
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionNeeded.toTypedArray(), REQUEST_CODE_PERMISSION)
            return
        }
    }

    private fun isAppPermissionGrantedOrNot(): Boolean {
        val listPermissionNeeded = ArrayList<String>()
        for (permission in permissionList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(permission)
            }
        }
        return listPermissionNeeded.size > 0
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //Gather permission grant results
        if (requestCode == REQUEST_CODE_PERMISSION) {
            val perms = HashMap<String, Int>()
            // Initial
//            perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
            perms[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED

            var deniedCount = 0
            for (i in permissions.indices) {
                perms[permissions[i]] = grantResults[i]
                //add only permissions which are denied

                if (perms[Manifest.permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED &&
                        perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED) {

                    launchMp3Activity()
                    return
                } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    deniedCount++
                }
            }

            if (deniedCount > 0) {
                requestAppPermissions()
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        val permissionList = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        private const val SPLASH_TIME_OUT = 3000L
        private const val REQUEST_CODE_PERMISSION = 1001
    }
}
