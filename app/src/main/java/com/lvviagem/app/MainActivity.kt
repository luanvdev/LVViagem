package com.lvviagem.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val hasAccess = mutableStateOf(false)
    private lateinit var music: MusicController

    private val legacyPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            hasAccess.value = checkAccess()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        music = MusicController(this)
        hasAccess.value = checkAccess()
        setContent {
            LvTheme {
                LvApp(hasAccess.value, { requestAccess() }, music)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasAccess.value = checkAccess()
    }

    override fun onDestroy() {
        music.release()
        super.onDestroy()
    }

    private fun checkAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            legacyPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
