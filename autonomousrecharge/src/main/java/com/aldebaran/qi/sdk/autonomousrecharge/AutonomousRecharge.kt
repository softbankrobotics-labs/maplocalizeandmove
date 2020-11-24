package com.aldebaran.qi.sdk.autonomousrecharge

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.aldebaran.qi.sdk.QiContext

/**
 * Entry point for autonomous recharge-related APIs.
 */
object AutonomousRecharge {

    const val RECHARGE_PERMISSION = "com.softbankrobotics.permission.AUTO_RECHARGE"
    private const val DOCKING_SOON_INTENT = "com.softbankrobotics.intent.DOCKING_SOON"
    private const val DOCK_ACTION_NAME = "com.softbankrobotics.intent.action.AUTO_DOCK"
    private const val UNDOCK_ACTION_NAME = "com.softbankrobotics.intent.action.AUTO_UNDOCK"
    private const val RECALL_POD = "recall_pod"
    private val receiver = RechargeRequestReceiver()

    @JvmStatic
    fun registerReceiver(qiContext: QiContext) {
        receiver.qiContext = qiContext
        qiContext.registerReceiver(receiver, IntentFilter(DOCKING_SOON_INTENT))
    }

    @JvmStatic
    fun unregisterReceiver() {
        receiver.qiContext?.unregisterReceiver(receiver)
        receiver.qiContext = null
    }

    @JvmStatic
    fun addOnDockingSoonListener(listener: AutonomousRechargeListeners.OnDockingSoonListener) {
        receiver.onDockingSoonListeners.add(listener)
    }

    @JvmStatic
    fun removeOnDockingSoonListener(listener: AutonomousRechargeListeners.OnDockingSoonListener) {
        receiver.onDockingSoonListeners.remove(listener)
    }

    @JvmStatic
    fun removeAllOnDockingSoonListeners() {
        receiver.onDockingSoonListeners.clear()
    }

    /**
     * Sends an intent to start the Autonomous Recharge Docking activity.
     * @param context the [Context].
     * @param recallPod a boolean to indicate whether to recall the previous pod location.
     *
     * @return none
     */
    @JvmStatic
    fun startDockingActivity(context: Context, recallPod: Boolean? = null) {
        val intent = Intent(DOCK_ACTION_NAME)
        recallPod?.let { intent.putExtra(RECALL_POD, recallPod) }
        context.startActivity(intent)
    }

    /**
     * Sends an intent to start the Autonomous Recharge Undocking activity.
     * @param context the [Context].
     *
     * @return none
     */
    @JvmStatic
    fun startUndockingActivity(context: Context) {
        val intent = Intent(UNDOCK_ACTION_NAME)
        context.startActivity(intent)
    }
}
