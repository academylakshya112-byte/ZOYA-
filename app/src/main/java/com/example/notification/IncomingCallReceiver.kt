package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class IncomingCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "IncomingCallReceiver"
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                Log.d(TAG, "Phone State changed: $state, Incoming number: $incomingNumber")

                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        CallAnnouncer.announceIncomingPhoneCall(context, incomingNumber)
                    }
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        // Call answered
                        CallAnnouncer.stopAnnouncement()
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        // Call ended or missed
                        CallAnnouncer.stopAnnouncement()
                    }
                }
                lastState = state ?: TelephonyManager.EXTRA_STATE_IDLE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling phone state broadcast", e)
        }
    }
}
