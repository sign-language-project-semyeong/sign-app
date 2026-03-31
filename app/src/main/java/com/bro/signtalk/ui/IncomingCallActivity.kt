package com.bro.signtalk.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bro.signtalk.R
import com.bro.signtalk.service.SignCallService

class IncomingCallActivity : AppCompatActivity() {

    private var initialX = 0f
    // [핵심] 번호 변수를 클래스 전역으로 격상!
    private var phoneNumber: String = ""

    private val callEndedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.bro.signtalk.CALL_ENDED") {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... (잠금화면 설정 코드 기존 유지) ...
        setContentView(R.layout.activity_incoming_call)

        // [쫀득] 데이터 멱살 잡기
        phoneNumber = intent.getStringExtra("receiver_phone") ?: ""
        val displayName = if (phoneNumber.isNotEmpty()) getContactName(phoneNumber) else "알 수 없는 브@로"

        findViewById<TextView>(R.id.tv_incoming_name).text = displayName
        findViewById<TextView>(R.id.tv_incoming_number)?.text = phoneNumber

        // [팩폭] 아래 코드는 XML에 btn_answer가 없으니깐 싹 다 지워버려라!
        // findViewById<View>(R.id.btn_answer)?.setOnClickListener { ... } <- 삭제!

        // [핵심] 오직 슬라이드 핸들(iv_swipe_handle)로만 승부한다!
        val handle = findViewById<ImageView>(R.id.iv_swipe_handle)
        setupSwipeListener(handle)
    }

    // [쌈뽕] 공통 응답 로직 엔진
    private fun answerCall() {
        SignCallService.currentCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
        val intent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("receiver_phone", phoneNumber) // 클래스 변수 사용!
        }
        startActivity(intent)
        finish()
    }

    private fun setupSwipeListener(handle: View) {
        handle.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { initialX = event.rawX; true }
                MotionEvent.ACTION_MOVE -> {
                    val diff = event.rawX - initialX
                    if (Math.abs(diff) < 400) view.translationX = diff
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val finalDiff = event.rawX - initialX
                    when {
                        finalDiff > 200 -> answerCall() // [찰진] 통합 엔진 호출!
                        finalDiff < -200 -> {
                            // [핵심] API 레벨 30 안 따지는 쌈뽕한 disconnect!
                            SignCallService.currentCall?.disconnect()
                            finish()
                        }
                        else -> view.animate().translationX(0f).setDuration(200).start()
                    }
                    true
                }
                else -> false
            }
        }
    }
    // ... (getContactName, onDestroy 기존 유지) ...
    private fun getContactName(phoneNumber: String): String {
        val uri = android.net.Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(phoneNumber)
        )
        val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
        return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else phoneNumber
        } ?: phoneNumber
    }

    override fun onDestroy() {
        super.onDestroy()
        // 6. [팩폭] 나갈 때 무전기 안 끄면 메모리 지랄난다! 똬악 해제해라!
        try {
            unregisterReceiver(callEndedReceiver)
        } catch (e: Exception) { }
    }
}