package com.bro.signtalk.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bro.signtalk.MainActivity
import com.bro.signtalk.R

class DialpadFragment : Fragment() {

    private lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return inflater.inflate(R.layout.fragment_dialpad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val input = view.findViewById<EditText>(R.id.number_input)
        val videoBtn = view.findViewById<View>(R.id.btn_video_call)
        val deleteBtn = view.findViewById<View>(R.id.btn_delete)
        val addContactBtn = view.findViewById<ImageButton>(R.id.btn_add_contact)
        val callBtn = view.findViewById<View>(R.id.btn_call)

        input.filters = arrayOf(InputFilter.LengthFilter(15))
        input.showSoftInputOnFocus = false

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hasText = s?.isNotEmpty() ?: false
                deleteBtn?.visibility = if (hasText) View.VISIBLE else View.INVISIBLE
                videoBtn?.visibility = if (hasText) View.VISIBLE else View.INVISIBLE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        input.addTextChangedListener(android.telephony.PhoneNumberFormattingTextWatcher())

        val buttonIds = listOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_star, R.id.btn_sharp
        )

        buttonIds.forEach { id ->
            view.findViewById<Button>(id)?.setOnClickListener { btn ->
                val charStr = (btn as Button).text.toString()
                input.append(charStr)

                val currentCall = (activity as? MainActivity)?.getCurrentCall()
                if (currentCall != null && currentCall.state == Call.STATE_ACTIVE) {
                    sendDtmfTone(currentCall, charStr[0])
                }
            }
        }

        // 1. [필살기] 영상 통화 버튼 리스너
        videoBtn?.setOnClickListener {
            val phoneNumber = input.text.toString()
            if (phoneNumber.isNotEmpty()) {
                Log.d("SignTalk", "$phoneNumber 브로에게 쌈뽕한 영상 통화 쏜다!")
                // [쫀득] 비서실 소환해서 0.1초 만에 영상 통화 발신!
                CallNavigation.startVideoCall(requireContext(), phoneNumber)
            } else {
                showToast("번호부터 치고 눌러라 브@로! 유남생?!?")
            }
        }

        // 2. [쌈뽕] 일반 통화 버튼 리스너 (여기 하나만 남겨라!)
        // 2. [쌈뽕] 일반 통화 버튼 리스너 (화면 소환 로직 추가!)
        // DialpadFragment.kt 의 callBtn 리스너 부분이다 이말이지!

        // 2. [쌈뽕] 일반 통화 버튼 리스너
        callBtn?.setOnClickListener {
            val phoneNumber = input.text.toString()
            if (phoneNumber.isNotEmpty()) {
                Log.d("SignTalk", "$phoneNumber 브@로에게 진짜 전@화 심부름 쏜다!")

                // [핵심] startManagedCall 대신 우리가 만든 makeCarrierCall을 호출해라!
                // 팩폭: 이름 안 맞으면 비서가 파@업한다 이말이야! 유남생?!?
                (activity as? MainActivity)?.makeCarrierCall(phoneNumber)

                // 주의: 여기서 직접 Intent로 CallActivity 띄우는 코드는 지워라!
                // 시스템이 전화를 연결하면 SignCallService가 0.1초 만에 띄워줄 거다 이말이야 ㅇㅇ.
            } else {
                showToast("전화번호가 없는데 어디다 걸어?!? 팍@씨!")
            }
        }

        // [팩폭] 이 아래에 있던 callBtn?.setOnClickListener { ... } 는 싹 다 성불시켜라!

        deleteBtn?.setOnClickListener {
            val length = input.text.length
            if (length > 0) input.text.delete(length - 1, length)
        }

        deleteBtn?.setOnLongClickListener {
            input.text.clear()
            showToast("싹 다 성불시켰다 이말이야! ㅇㅇ.")
            true
        }

        // [진짜 필살기] 브로가 만든 커스텀 페이지로 번호 들고 점프!
        addContactBtn?.setOnClickListener {
            val phoneNumber = input.text.toString()
            if (phoneNumber.isNotEmpty()) {
                // [쫀득] 아까 만든 AddContactActivity로 0.1초 만에 튕겨나가라!
                val intent = Intent(requireContext(), AddContactActivity::class.java).apply {
                    // [팩폭] 키 이름은 AddContactActivity에서 받는 이름이랑 똑같아야 한다!
                    putExtra("input_number", phoneNumber)
                }
                startActivity(intent)
                Log.d("SignTalk", "브로의 쌈뽕한 추가 페이지로 이동한다 이말이야! ㅇㅇ.")
            } else {
                showToast("번호부터 입력해라 브@로! 팍씨!")
            }
        }
    }

    private fun sendDtmfTone(call: Call, digit: Char) {
        call.playDtmfTone(digit)
        Handler(Looper.getMainLooper()).postDelayed({
            call.stopDtmfTone()
        }, 200)
    }

    fun toggleSpeaker(isOn: Boolean) {
        audioManager.isSpeakerphoneOn = isOn
        showToast("스피커폰 ${if(isOn) "ON" else "OFF"} 했다 이말이야!")
    }

    fun toggleMute(isMuted: Boolean) {
        audioManager.isMicrophoneMute = isMuted
        showToast("마이크 ${if(isMuted) "차단" else "해제"} 완료! ㅇㅇ.")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}