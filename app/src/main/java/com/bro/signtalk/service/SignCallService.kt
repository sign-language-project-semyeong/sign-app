package com.bro.signtalk.service

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import android.content.Intent // 이게 없으면 Intent가 뭔지 모른다 이말이지!
import com.bro.signtalk.ui.CallActivity // 4번 친구 화면도 임포트 해야지?!?

// 브@로, 이게 바로 안드로이드 통화 시스템의 멱@살을 잡는 클래스다 이말이야!
class SignCallService : InCallService() {

    private val TAG = "SignTalk_Service"

    // 1. 전화가 딱! 들어오면 시스템이 브@로를 호출한다 이말이야?!?
    // SignCallService.kt 안의 onCallAdded 수정!
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "전화 왔다! 멱살 잡는다 이말이야!")

        // [필수] 이거 안 하면 전화 끊기는 거 감지 못 한다 이말이야!
        call.registerCallback(callCallback)

        val intent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        setAudioRoute(CallAudioState.ROUTE_SPEAKER)
    }

    // 4. 전화가 끊기면 깰@끔하게 정리해라 이말이야!
    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "상황 종료! 전화 끊겼다 이말이야.")
        call.unregisterCallback(callCallback)
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            // 전화가 [응답됨/대기중/끊김] 상태 변할 때마다 로그 찍어라 이말이야!
            Log.d(TAG, "전화 상태 변했다 이말이야: $state")
        }
    }
}