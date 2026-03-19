package com.bro.signtalk

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallsManager
import androidx.fragment.app.Fragment // [수정] 임포트 추가!
import androidx.lifecycle.lifecycleScope
import com.bro.signtalk.ui.CallActivity
import com.google.android.material.bottomnavigation.BottomNavigationView // [수정] 임포트 추가!
import kotlinx.coroutines.launch
import com.bro.signtalk.ui.DialpadFragment
import com.bro.signtalk.ui.RecentCallsFragment
import com.bro.signtalk.ui.ContactsFragment
class MainActivity : AppCompatActivity() {

    private val TAG = "SignTalk_Main"
    private lateinit var callsManager: CallsManager

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "이제 내가 대장이다 이말이야!")
            setupCoreTelecom()
        } else {
            Toast.makeText(this, "권한 줘라 브@로!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkDefaultDialerRole()

        // [핵심] 하단 내비게이션 연결이다 이말이야 유남생?!?
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            loadFragment(DialpadFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dialpad -> loadFragment(DialpadFragment())
                R.id.nav_recent -> loadFragment(RecentCallsFragment())
                R.id.nav_contacts -> loadFragment(ContactsFragment())
                else -> false
            }
        }

        // [주의] 만약 기존 테스트 버튼을 쓰고 싶으면 XML에 버튼이 살아있어야 한다 이말이야!
        // findViewById<Button>(R.id.btn_test_call)?.setOnClickListener { simulateIncomingCall() }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }

    private fun setupCoreTelecom() {
        callsManager = CallsManager(this)
        val capabilities = CallsManager.CAPABILITY_BASELINE or
                CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING

        callsManager.registerAppWithTelecom(capabilities)
        Log.d(TAG, "Core-Telecom 등록 완@료!")
    }

    fun simulateIncomingCall() {
        // 1. alpha03 버전은 callCapabilities가 그냥 Int(정수)다 이말이야!
        val capabilities = CallAttributesCompat.SUPPORTS_SET_INACTIVE

        // 2. [핵심] 인자를 딱 5개만, 이 순서대로 박아라 이말이야!
        val attributes = CallAttributesCompat(
            "수@어 브@로",                       // 1. displayName (CharSequence)
            Uri.parse("tel:01012345678"),        // 2. address (Uri)
            CallAttributesCompat.DIRECTION_INCOMING, // 3. direction (Int)
            CallAttributesCompat.CALL_TYPE_VIDEO_CALL, // 4. callType (Int)
            capabilities                         // 5. callCapabilities (Int)
        ) // 여기서 괄호 딱! 닫아라 이말이야 유남생?!?

        lifecycleScope.launch {
            try {
                callsManager.addCall(
                    attributes,
                    onAnswer = { callType -> Log.d(TAG, "수락! 타입: $callType") },
                    onDisconnect = { cause -> Log.d(TAG, "종료: $cause") },
                    onSetActive = { Log.d(TAG, "활성!") },
                    onSetInactive = { Log.d(TAG, "대기!") }
                ) {
                    // CallControlScope 진입! 여기서 UI 띄워라 이말이야!
                    val intent = Intent(this@MainActivity, CallActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "전화 추가 실패했다 이말이야: ${e.message}")
            }
        }
    }

    private fun launchCallUI() {
        // [오류 4 해결] 안전하게 Activity 실행하는 비기!
        val intent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        Log.d(TAG, "CallActivity 쌈@뽕하게 소환!")
    }

    private fun checkDefaultDialerRole() {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                roleRequestLauncher.launch(intent)
            } else {
                setupCoreTelecom() // 이미 기본 앱이면 바로 등록!
            }
        }
    }
}