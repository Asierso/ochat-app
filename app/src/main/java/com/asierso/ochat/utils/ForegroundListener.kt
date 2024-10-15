package com.asierso.ochat.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ForegroundListener : DefaultLifecycleObserver{
    companion object {
        var isForeground = false
        var canSendNotification = false;
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isForeground = true
        canSendNotification = true
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onStart(owner)
        isForeground = true
        canSendNotification = true
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        isForeground = false
        canSendNotification = false
    }
}