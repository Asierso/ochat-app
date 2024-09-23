package com.asierso.ochat

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

class Global {
    companion object {
        fun getPixels(context : Context, dp : Int) : Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }
    }
}