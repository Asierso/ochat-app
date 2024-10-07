package com.asierso.ochat.utils

import android.content.Context
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
    }
}