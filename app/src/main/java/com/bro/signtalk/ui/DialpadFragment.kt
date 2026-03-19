package com.bro.signtalk.ui

import android.content.Intent
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.bro.signtalk.MainActivity
import com.bro.signtalk.R
import android.util.Log
import android.widget.Toast
class DialpadFragment : Fragment() {

    // 1. 뷰를 생성하는 곳이다 이말이야!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dialpad, container, false)
    }

    // 2. 뷰가 다 만들어진 후 버튼을 연결하는 곳이다 이말이지 ㅇㅇ
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val input = view.findViewById<EditText>(R.id.number_input)
        val videoBtn = view.findViewById<View>(R.id.btn_video_call) // 영상통화 버튼 소환!
        val deleteBtn = view.findViewById<View>(R.id.btn_delete)   // 지우기 버튼 소환!

        // [팩폭] 키보드 차단하고 복사/붙여넣기만 살리는 비기!
        input.showSoftInputOnFocus = false

        // 1. 하이픈 자동 추가 및 버튼 가시성 제어 드간다!
        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                // 숫@자가 있으면 영상통화랑 지우기 버튼을 똬@악 노출!
                val visibility = if (s.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
                videoBtn?.visibility = visibility
                deleteBtn?.visibility = visibility
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 하이픈 포맷팅 왓쳐도 잊지 마라 이말이야!
        input.addTextChangedListener(android.telephony.PhoneNumberFormattingTextWatcher())

        // 2. 숫자 버튼 0~9, *, # 처리 (기존 로직 찰@지게 유지!)
        val buttonIds = listOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_star, R.id.btn_sharp
        )

        buttonIds.forEach { id ->
            view.findViewById<Button>(id)?.setOnClickListener { btn ->
                input.append((btn as Button).text)
            }
        }

        // 3. 지우기 버튼 (BackSpace) 쫀득한 로직
        // 3. 지우기 버튼 (BackSpace) 쫀득한 수정 로직 드간다!
        deleteBtn?.setOnClickListener {
            // [핵심] setText 쓰지 말고, 시스템 백스페이스 이벤트를 직접 쏴버려라 이말이야!
            val action = android.view.KeyEvent.ACTION_DOWN
            val code = android.view.KeyEvent.KEYCODE_DEL
            val event = android.view.KeyEvent(action, code)

            // 이@렇게 하면 PhoneNumberFormattingTextWatcher가 삐@지지 않고 계속 일한다 이말이지 유남생?!?
            input.dispatchKeyEvent(event)
        }

        // 4. 일반 전화 버튼 (MainActivity 소환!)
        view.findViewById<View>(R.id.btn_call).setOnClickListener {
            if (input.text.isNotEmpty()) {
                (activity as? MainActivity)?.simulateIncomingCall()
            } else {
                Toast.makeText(requireContext(), "번@호부터 찍어라 브@로!", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. [핵심] 영상통화 버튼 -> 이게 우리 '수@어 통화'의 시작이다 이말이지!
        videoBtn?.setOnClickListener {
            if (input.text.isNotEmpty()) {
                // 여기서는 일반 전화가 아니라 우리만의 CallActivity를 바로 쏴버려!
                val intent = Intent(requireContext(), com.bro.signtalk.ui.CallActivity::class.java)
                startActivity(intent)
                Log.d("Dialpad", "수@어 영@상통화 시작한다 이말이야 유남생?!?")
            }
        }
    }
}