package com.bro.signtalk.ui.call

import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.bro.signtalk.R

class InCallActivity : AppCompatActivity() {
    private lateinit var audioManager: AudioManager
    private var isMuted = false
    private var isSpeakerOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_call)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // 1. [필살기] 내 소리 차단 (Mute)
        findViewById<ImageButton>(R.id.btn_mute).setOnClickListener { view ->
            isMuted = !isMuted
            audioManager.isMicrophoneMute = isMuted
            // [쫀득] 체크박스처럼 상태에 따라 배경색 똬악 바꿔라!
            view.isSelected = isMuted
            Log.d("InCall", "마이크 차단 상태: $isMuted")
        }

        // 2. [필살기] 스피커폰 전환
        findViewById<ImageButton>(R.id.btn_speaker).setOnClickListener { view ->
            isSpeakerOn = !isSpeakerOn
            audioManager.isSpeakerphoneOn = isSpeakerOn
            view.isSelected = isSpeakerOn
        }

        // 3. [쫀득] 키패드 소환 (레이아웃 visibility 조절)
        findViewById<ImageButton>(R.id.btn_keypad).setOnClickListener { view ->
            val keypadLayout = findViewById<View>(R.id.layout_dialpad)
            keypadLayout.visibility = if (keypadLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            view.isSelected = (keypadLayout.visibility == View.VISIBLE)
        }

        // 블루투스, 녹음 등은 시스템 API 권한이 더 빡세니 일단 버튼 연동부터 해라 이말이야!
    }
}