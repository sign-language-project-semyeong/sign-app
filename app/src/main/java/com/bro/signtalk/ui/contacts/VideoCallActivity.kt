package com.bro.signtalk.ui.contacts

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bro.signtalk.R
import com.bro.signtalk.service.SignCallService

class VideoCallActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager
    private lateinit var cameraManager: CameraManager
    private var isCameraOpening = false
    // [필살기] 도화지(SurfaceView) 변수를 클래스 전역으로 0.1초 만에 격상!
    private lateinit var localSurface: SurfaceView
    private lateinit var remoteSurface: SurfaceView

    private var isCameraOn = true
    private var isMuted = false
    private var isSpeakerOn = true
    private var currentCameraFacing = CameraCharacteristics.LENS_FACING_FRONT

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    // [팩폭] 브로가 만든 통합 리시버! 이름 똑바로 기억해라!
    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.bro.signtalk.CALL_ENDED" -> {
                    Log.d("VideoCall", "통화 종료 검거! 0.1초 만에 자폭한다!")
                    // [핵심] 이거 무조건 들어가야 끊길 때 닫히고 키패드로 돌아간다 팍씨!
                    finish()
                }
                "com.bro.signtalk.CALL_STARTED" -> {
                    Log.d("VideoCall", "드디어 연결 신호 포착! UI 변신 드간다!")
                    // [필살기] 메인 스레드에서 0.1초 만에 UI 갈아치우기!
                    runOnUiThread {
                        refreshCallUI()
                        // 카메라도 확실하게 한 번 더 기강 잡아라!
                        openCamera(localSurface.holder)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        // [쫀득] 여기서 변수에 똬@악 할당해줘야 밑에서 에러 안 난다!
        localSurface = findViewById(R.id.sv_local_video)
        remoteSurface = findViewById(R.id.sv_remote_video)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        setSpeakerphone(true)

        val filter = IntentFilter().apply {
            addAction("com.bro.signtalk.CALL_ENDED")
            addAction("com.bro.signtalk.CALL_STARTED")
        }
        // [쌈뽕] callReceiver로 찰지게 등록!
        ContextCompat.registerReceiver(this, callReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        setupUI()
        initCamera()
    }

    // [필살기] UI 구성 엔진
    private fun setupUI() {
        val number = intent.getStringExtra("receiver_phone") ?: ""
        findViewById<TextView>(R.id.tv_call_name).text = getContactName(number)
        findViewById<TextView>(R.id.tv_call_number).text = number

        // 1. 카메라 토글
        // 1. [쫀득] 내 화면 끄기/켜기 (카메라 토글)
        findViewById<View>(R.id.btn_camera_toggle).setOnClickListener {
            isCameraOn = !isCameraOn
            it.isSelected = !isCameraOn // 버튼 UI 상태 변경

            if (isCameraOn) {
                openCamera(localSurface.holder)
                // 상대방한테 내 영상 다시 쏴주기
                SignCallService.currentCall?.videoCall?.setPreviewSurface(localSurface.holder.surface)
            } else {
                closeCamera()
                // 상대방한테는 내 영상 안 보낸다고 신호 쏘기
                SignCallService.currentCall?.videoCall?.setPreviewSurface(null)
            }
        }


        // 2. [쌈뽕] 카메라 전/후면 전환
        findViewById<View>(R.id.btn_switch_camera).setOnClickListener {
            if (!isCameraOn || isCameraOpening) {
                Log.d("VideoCall", "카메라가 꺼져있거나 바쁜 중이라 무시한다 팍씨!")
                return@setOnClickListener
            }

            // [쫀득] 방향 촥 바꾸기
            currentCameraFacing = if (currentCameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                CameraCharacteristics.LENS_FACING_BACK
            } else {
                CameraCharacteristics.LENS_FACING_FRONT
            }

            Log.d("VideoCall", "카메라 0.1초 만에 앞뒤 바꾼다 ㅇㅇ.")
            closeCamera() // 일단 닫고!
            openCamera(localSurface.holder) // 다시 열어라!
        }

        // 3. 음소거 (투명도 조절 엔진)
        val btnMute = findViewById<ImageButton>(R.id.btn_mute)
        btnMute.alpha = if (isMuted) 1.0f else 0.4f // 초기 상태 세팅!
        btnMute.isSelected = isMuted
        btnMute.setOnClickListener {
            val btn = it as android.widget.ImageButton
            isMuted = !isMuted
            audioManager.isMicrophoneMute = isMuted

            // 켜지면 찐하게(1.0), 꺼지면 흐릿하게(0.4) 똬악 바꿔라!
            btn.alpha = if (isMuted) 1.0f else 0.4f
            btn.isSelected = isMuted
        }

        // 4. 스피커폰 (투명도 조절 엔진)
        val btnSpeaker = findViewById<ImageButton>(R.id.btn_speaker)

        // [핵심] 이거 빼먹어서 여태 헷갈렸던 거다 팍씨! 처음 켜졌을 때도 UI를 찐하게 만들어라!
        btnSpeaker.alpha = if (isSpeakerOn) 1.0f else 0.4f
        btnSpeaker.isSelected = isSpeakerOn

        btnSpeaker.setOnClickListener {
            val btn = it as android.widget.ImageButton
            isSpeakerOn = !isSpeakerOn
            setSpeakerphone(isSpeakerOn)

            // [쫀득] 스피커 켜지면 1.0으로 확! 꺼지면 0.4로 스르륵!
            btn.alpha = if (isSpeakerOn) 1.0f else 0.4f
            btn.isSelected = isSpeakerOn
        }

        // 5. 종료
        findViewById<View>(R.id.btn_hangup).setOnClickListener {
            SignCallService.currentCall?.disconnect()
            finish()
        }
    }

    // [쌈뽕] 내 화면 축소 애니메이션 엔진
    private fun animateLocalVideoToCorner() {
        localSurface.animate()
            .scaleX(0.33f)
            .scaleY(0.25f)
            .translationX(-320f)
            .translationY(-850f)
            .setDuration(600)
            .withStartAction {
                Log.d("VideoCall", "애니메이션 엔진 쫀득하게 풀가동! ㅇㅇ.")
            }
            .start()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("VideoCall", "알림 타고 복귀! UI 서열 정리 드간다 이말이야! ㅇㅇ.")

        val loadingLayout = findViewById<View>(R.id.layout_camera_loading)
        val infoLayout = findViewById<View>(R.id.layout_info)
        val controllerLayout = findViewById<View>(R.id.layout_controller)

        if (SignCallService.currentCall?.state != android.telecom.Call.STATE_ACTIVE) {
            loadingLayout.bringToFront()
            infoLayout.bringToFront()
            controllerLayout.bringToFront()
            loadingLayout.requestLayout()
            loadingLayout.invalidate()
        } else {
            Log.d("VideoCall", "통화 중에 돌아왔다! 스트림 심폐소생술 드간다!")
            loadingLayout.visibility = View.GONE
            controllerLayout.visibility = View.VISIBLE

            // [쫀득] 여기서도 도화지 검문 필수!
            if (localSurface.holder.surface.isValid && remoteSurface.holder.surface.isValid) {
                if (isCameraOn && cameraDevice == null) {
                    openCamera(localSurface.holder)
                }
                startVideoStreams()
            }
        }
    }

    // [필살기] 상태 변경 시 UI와 카메라 기강 잡기
    private fun refreshCallUI() {
        val currentCall = SignCallService.currentCall
        val isConnected = currentCall?.state == android.telecom.Call.STATE_ACTIVE

        val loadingLayout = findViewById<View>(R.id.layout_camera_loading)
        val controllerLayout = findViewById<View>(R.id.layout_controller)

        if (isConnected) {
            loadingLayout.visibility = View.GONE
            controllerLayout.visibility = View.VISIBLE
            animateLocalVideoToCorner()

            startVideoStreams()
            Log.d("VideoCall", "리얼 대화 모드 및 스트림 엔진 가동! ㅇㅇ.")
        } else {
            loadingLayout.visibility = View.VISIBLE
            controllerLayout.visibility = View.VISIBLE

            findViewById<TextView>(R.id.tv_loading_msg)?.text = "상대방과 영상 채널 연결 중이다 이말이야... ㅇㅇ."
            Log.d("VideoCall", "영상 엔진 예열 중이다 이말이야!")
        }
    }

    private fun initCamera() {
        localSurface.setZOrderMediaOverlay(true)

        localSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d("VideoCall", "내 얼굴 도화지 펴졌다! 프리뷰 & 스트림 쏜다!")
                // [필살기] 도화지 생겼으니까 카메라 켜고 스트림 꽂아라!
                if (isCameraOn && cameraDevice == null) {
                    openCamera(holder)
                }
                startVideoStreams()
            }
            override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {
                startVideoStreams()
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // [팩폭] 도화지 찢어졌으면 카메라도 조용히 꺼놔야 꼬이지 않는다!
                closeCamera()
            }
        })

        remoteSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startVideoStreams()
            }
            override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {
                startVideoStreams()
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }

    private fun startVideoStreams() {
        val currentCall = SignCallService.currentCall ?: return
        val videoCallProvider = currentCall.videoCall ?: return

        val remoteS = remoteSurface.holder.surface
        val localS = localSurface.holder.surface

        if (remoteS.isValid && localS.isValid) {
            try {
                videoCallProvider.setDisplaySurface(remoteS)
                videoCallProvider.setPreviewSurface(localS)
                Log.d("VideoCall", "스트림 똬@악 꽂았다 이말이야! ㅇㅇ.")
            } catch (e: Exception) {
                Log.e("VideoCall", "스트림 연결 에러: ${e.message}")
            }
        }
    }

    private fun openCamera(holder: SurfaceHolder) {
        // [쫀득] 이미 문 열고 있으면 씹어버려라!
        if (isCameraOpening) {
            Log.d("VideoCall", "이미 카메라 여는 중이다! 중복 실행 컷! ㅇㅇ.")
            return
        }

        try {
            isCameraOpening = true // [필살기] 자물쇠 철컥!
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == currentCameraFacing
            } ?: cameraManager.cameraIdList[0]

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        isCameraOpening = false // [쌈뽕] 무사히 열렸으면 자물쇠 풀고 프리뷰 쏴라!
                        cameraDevice = camera
                        startPreview(holder)
                    }
                    override fun onDisconnected(camera: CameraDevice) {
                        isCameraOpening = false
                        closeCamera()
                    }
                    override fun onError(camera: CameraDevice, error: Int) {
                        isCameraOpening = false
                        closeCamera()
                    }
                }, null)
            }
        } catch (e: Exception) {
            isCameraOpening = false
            Log.e("VideoCall", "카메라 오픈 실패! 유남생?!?")
        }
    }

    private fun startPreview(holder: SurfaceHolder) {
        val device = cameraDevice ?: return
        val surface = holder.surface

        if (!surface.isValid) {
            Log.e("VideoCall", "도화지가 아직 축축해서 못 그린다 브로!")
            return
        }

        try {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(surface)

            captureSession?.close()
            captureSession = null

            device.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) {
                        session.close()
                        return
                    }

                    captureSession = session
                    try {
                        session.setRepeatingRequest(builder.build(), null, null)
                        Log.d("VideoCall", "로컬 프리뷰 엔진 드디어 소생 완료! ㅇㅇ.")
                    } catch (e: IllegalStateException) {
                        Log.w("VideoCall", "세션이 0.1초 만에 닫혔구만?!? 무시하고 드가자! ㅇㅇ.")
                    } catch (e: Exception) {
                        Log.e("VideoCall", "반복 요청 실패: ${e.message}")
                        val retryView = findViewById<View>(android.R.id.content)
                        retryView.removeCallbacks(null)
                        retryView.postDelayed({ startPreview(holder) }, 500)
                    }
                }

                override fun onConfigureFailed(s: CameraCaptureSession) {
                    Log.e("VideoCall", "세션 구성 싹바가지 없게 실패! 유남생?!?")
                }

                override fun onClosed(session: CameraCaptureSession) {
                    super.onClosed(session)
                    if (captureSession == session) captureSession = null
                }
            }, null)
        } catch (e: Exception) {
            Log.e("VideoCall", "프리뷰 시작 로직 대참사: ${e.message}")
        }
    }

    private fun setSpeakerphone(on: Boolean) {
        audioManager.isSpeakerphoneOn = on
        SignCallService.instance?.setAudioRoute(if (on) android.telecom.CallAudioState.ROUTE_SPEAKER
        else android.telecom.CallAudioState.ROUTE_WIRED_OR_EARPIECE)
        Log.d("VideoCall", "스피커폰 상태: $on 이말이야!")
    }

    private fun closeCamera() {
        captureSession?.close(); captureSession = null
        cameraDevice?.close(); cameraDevice = null
    }

    private fun getContactName(phoneNumber: String): String {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        return contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else phoneNumber
        } ?: phoneNumber
    }

    override fun onResume() {
        super.onResume()
        com.bro.signtalk.service.SignCallService.CallScreenTracker.isVisible = true

        // [팩폭] 이거 안 부르면 돌아왔을 때 까만 로딩창이 내 얼굴 덮고 있어서 먹통처럼 보인다 이말이야!
        refreshCallUI()

        // [쫀득] 통화 중이면서 도화지가 멀쩡할 때만 카메라 살려라!
        if (SignCallService.currentCall?.state == android.telecom.Call.STATE_ACTIVE) {
            Log.d("VideoCall", "화면 복귀! 도화지 상태 검문 드간다 ㅇㅇ.")
            if (localSurface.holder.surface.isValid && remoteSurface.holder.surface.isValid) {
                if (isCameraOn && cameraDevice == null) {
                    openCamera(localSurface.holder)
                }
                startVideoStreams()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        com.bro.signtalk.service.SignCallService.CallScreenTracker.isVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        // [팩폭] 해지할 때도 등록할 때 썼던 callReceiver를 똬악 넣어라!
        try { unregisterReceiver(callReceiver) } catch (e: Exception) { }
    }
}