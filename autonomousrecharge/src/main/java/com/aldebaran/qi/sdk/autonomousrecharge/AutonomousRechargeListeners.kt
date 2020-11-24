package com.aldebaran.qi.sdk.autonomousrecharge

import com.aldebaran.qi.sdk.QiContext

interface AutonomousRechargeListeners {

    /**
     * Triggered when the battery is less than 3% above the low battery threshold, or when the time
     * is 5 minutes before the docking alarm.
     */
    interface OnDockingSoonListener {
        fun onDockingSoon(qiContext: QiContext)
    }
}