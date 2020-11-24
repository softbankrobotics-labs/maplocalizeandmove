package com.aldebaran.qi.sdk.autonomousrecharge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aldebaran.qi.sdk.QiContext
import java.util.concurrent.CopyOnWriteArrayList

class RechargeRequestReceiver(var qiContext: QiContext? = null): BroadcastReceiver() {
    val onDockingSoonListeners = CopyOnWriteArrayList<AutonomousRechargeListeners.OnDockingSoonListener>()

    override fun onReceive(context: Context?, intent: Intent?) {
        qiContext?.let { qiContext ->
            onDockingSoonListeners.forEach {
                it.onDockingSoon(qiContext)
            }
        }
    }
}