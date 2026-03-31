package com.bro.signtalk.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SignCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        Log.d("SignTalk_Receiver", "신호 검거: $action 이말이야! ㅇㅇ.")

        when (action) {
            ACTION_HANGUP -> {
                // [필살기] 서비스의 currentCall 멱살 잡고 종료!
                SignCallService.currentCall?.disconnect()
            }
        }
    }
}