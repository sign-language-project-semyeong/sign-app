package com.bro.signtalk.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bro.signtalk.MainActivity
import com.bro.signtalk.ui.contacts.VideoCallActivity

/**
 * [필살기] 통화 관련 화면 이동을 담당하는 전담 비서실!
 */
// CallNavigation.kt 수정안

object CallNavigation {
    private const val TAG = "SignTalk_Nav"

    fun startVideoCall(context: Context, phoneNumber: String, name: String? = null) {
        if (phoneNumber.isEmpty()) return

        val mainActivity = when (context) {
            is MainActivity -> context
            is Activity -> context as? MainActivity
            else -> null
        }

        // [쫀득] 엔진한테 영상통화라고 (isVideo = true) 확실히 스위치 켜서 심부름 시켜라!
        mainActivity?.makeCarrierCall(phoneNumber, isVideo = true)
            ?: Log.e(TAG, "MainActivity를 못 찾아서 심@부름을 못 시켰다 브@로! 팍@씨!")

        // [팩폭] 억지로 화면 띄우던 이 밑에 있던 코드들 (val intent = Intent(...) 어쩌구)
        // 싹 다 지워버려라! 성불시켜라 이말이야!
        // 시스템이 placeCall() 요청을 받으면 SignCallService를 깨우고 거기서 VideoCallActivity를 쌈뽕하게 소환해준다 ㅇㅇ.
    }
}