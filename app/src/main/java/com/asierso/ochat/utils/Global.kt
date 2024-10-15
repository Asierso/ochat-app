package com.asierso.ochat.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.asierso.ochat.MainActivity
import com.asierso.ochat.SettingsActivity
import com.asierso.ochat.models.ClientSettings

class Global {
    companion object {
        fun getPixels(context : Context, dp : Int) : Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }

        fun bakeUrl(settings: ClientSettings?): String? {
            if (settings == null)
                return null

            return "${if (settings.isSsl) "https" else "http"}://${settings.ip}:${settings.port}"
        }

        fun checkPermissions(context: Context) : Boolean{
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        context as SettingsActivity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        0
                    )
                    return (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED)
                }
            }
            return false
        }
    }
}