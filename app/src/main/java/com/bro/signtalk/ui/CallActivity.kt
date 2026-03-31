package com.bro.signtalk.ui

import android.widget.Chronometer
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bro.signtalk.R
import com.bro.signtalk.service.SignCallService
import com.bro.signtalk.ui.contacts.VideoCallActivity // 패키지 경로 확인해라 브로!

class CallActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager
    private var isSpeakerOn = false
    private var isMuted = false

    // [쫀득] 모든 신호를 낚아채는 통합 수신기다 이말이야!
    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.bro.signtalk.CALL_ENDED" -> {
                    Log.d("Call", "통화 종료 검거! 0.1초 만에 자폭한다!")
                    findViewById<Chronometer>(R.id.chronometer_call_time).stop()
                    finish()
                }
                "com.bro.signtalk.CALL_STARTED" -> {
                    Log.d("Call", "통화 연결 확인! 타이머 똬악 시작한다 이말이야! ㅇㅇ.")
                    val timer = findViewById<Chronometer>(R.id.chronometer_call_time)
                    timer.base = android.os.SystemClock.elapsedRealtime()
                    timer.visibility = View.VISIBLE
                    timer.start()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // [쌈뽕] 잠금화면 뚫고 나오는 설정은 필수다 이말이야!
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        // [팩폭] 리시버 등록할 때 오타('signtalk') 주의해라!
        val filter = IntentFilter().apply {
            addAction("com.bro.signtalk.CALL_ENDED")
            addAction("com.bro.signtalk.CALL_STARTED")
        }
        ContextCompat.registerReceiver(this, callReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        setupUI()
    }

    private fun setupUI() {
        val number = intent.getStringExtra("receiver_phone") ?: ""
        val nameFromIntent = intent.getStringExtra("receiver_name")
        val displayName = if (!nameFromIntent.isNullOrEmpty() && nameFromIntent != "알 수 없음") {
            nameFromIntent
        } else {
            getContactName(number)
        }

        findViewById<TextView>(R.id.tv_call_name).text = displayName
        findViewById<TextView>(R.id.tv_call_number).text = number

        // 1. 스피커폰 (InCallActivity의 로직을 쫀득하게 이식!)
        findViewById<View>(R.id.btn_speaker).setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            it.isSelected = isSpeakerOn
            setSpeakerphone(isSpeakerOn)
        }

        // 2. 뮤트 (마이크 차단)
        findViewById<View>(R.id.btn_mute).setOnClickListener {
            isMuted = !isMuted
            audioManager.isMicrophoneMute = isMuted
            it.isSelected = isMuted
        }

        // 3. 키패드 토글 (애니메이션 기강 잡아라!)
        findViewById<View>(R.id.btn_keypad).setOnClickListener {
            val dialpad = findViewById<View>(R.id.layout_dialpad)
            if (dialpad.visibility == View.GONE) {
                dialpad.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up))
                dialpad.visibility = View.VISIBLE
                it.isSelected = true
            } else {
                val slideDown = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_down)
                slideDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        dialpad.visibility = View.GONE
                    }
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })
                dialpad.startAnimation(slideDown)
                it.isSelected = false
            }
        }

        // 4. 영상 통화 전환 (필살기!)
        findViewById<View>(R.id.btn_video)?.setOnClickListener {
            SignCallService.currentCall?.let { call ->
                Log.d("Call", "음성에서 영상으로 변신! ㅇㅇ.")
                // 시스템에 영상 모드 응답 쏴라!
                call.answer(android.telecom.VideoProfile.STATE_BIDIRECTIONAL)

                val intent = Intent(this, VideoCallActivity::class.java).apply {
                    putExtra("receiver_phone", number)
                    putExtra("receiver_name", displayName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
                finish()
            }
        }

        setupKeypad()

        // 5. 종료 버튼 (통화 세션 확실히 끊어라!)
        findViewById<View>(R.id.btn_hangup).setOnClickListener {
            SignCallService.currentCall?.disconnect()
            finish()
        }
    }

    private fun setupKeypad() {
        val buttonIds = arrayOf(R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_star, R.id.btn_pound)
        val digits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '#')
        buttonIds.forEachIndexed { index, id ->
            findViewById<View>(id)?.setOnClickListener { sendDtmf(digits[index]) }
        }
    }

    private fun sendDtmf(digit: Char) {
        SignCallService.currentCall?.let {
            it.playDtmfTone(digit)
            it.stopDtmfTone()
        }
    }

    private fun setSpeakerphone(on: Boolean) {
        SignCallService.instance?.setAudioRoute(
            if (on) android.telecom.CallAudioState.ROUTE_SPEAKER
            else android.telecom.CallAudioState.ROUTE_WIRED_OR_EARPIECE
        )
    }

    private fun getContactName(phoneNumber: String): String {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        return contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else phoneNumber
        } ?: phoneNumber
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.mode = AudioManager.MODE_NORMAL
        try { unregisterReceiver(callReceiver) } catch (e: Exception) { }
    }
}