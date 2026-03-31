package com.bro.signtalk.ui.call

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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bro.signtalk.R
import com.bro.signtalk.service.SignCallService

class VideoCallActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager
    private lateinit var cameraManager: CameraManager

    // [필살기] 도화지(SurfaceView) 변수를 클래스 전역으로 0.1초 만에 격상!
    private lateinit var localSurface: SurfaceView
    private lateinit var remoteSurface: SurfaceView

    private var isCameraOn = true
    private var isMuted = false
    private var isSpeakerOn = false
    private var currentCameraFacing = CameraCharacteristics.LENS_FACING_FRONT

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

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

        setSpeakerphone(false)

        val filter = IntentFilter().apply {
            addAction("com.bro.signtalk.CALL_ENDED")
            addAction("com.bro.signtalk.CALL_STARTED")
        }
        ContextCompat.registerReceiver(this, callReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        setupUI()
        initCamera()
    }

    // [필살기] UI 구성 엔진 (가출했던 거 다시 잡아왔다 이말이야!)
    private fun setupUI() {
        val number = intent.getStringExtra("receiver_phone") ?: ""
        findViewById<TextView>(R.id.tv_call_name).text = getContactName(number)
        findViewById<TextView>(R.id.tv_call_number).text = number

        // 1. 카메라 토글
        findViewById<View>(R.id.btn_camera_toggle).setOnClickListener {
            isCameraOn = !isCameraOn
            it.isSelected = !isCameraOn
            if (isCameraOn) openCamera(localSurface.holder)
            else closeCamera()
        }

        // 2. 전/후면 전환
        findViewById<View>(R.id.btn_switch_camera).setOnClickListener {
            currentCameraFacing = if (currentCameraFacing == CameraCharacteristics.LENS_FACING_FRONT)
                CameraCharacteristics.LENS_FACING_BACK else CameraCharacteristics.LENS_FACING_FRONT
            closeCamera()
            openCamera(localSurface.holder)
        }

        // 3. 음소거
        findViewById<View>(R.id.btn_mute).setOnClickListener {
            isMuted = !isMuted
            audioManager.isMicrophoneMute = isMuted
            it.isSelected = isMuted
        }

        // 4. 스피커폰
        findViewById<View>(R.id.btn_speaker).setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            it.isSelected = isSpeakerOn
            setSpeakerphone(isSpeakerOn)
        }

        // 5. 종료
        findViewById<View>(R.id.btn_hangup).setOnClickListener {
            SignCallService.currentCall?.disconnect()
            finish()
        }
    }

    // [쌈뽕] 내 화면 축소 애니메이션 엔진 (이것도 가출했었지?!?)
    private fun animateLocalVideoToCorner() {
        // 0.1초 만에 슥- 하고 왼쪽 위로 이동하며 작아지는 마법!
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

        // [필살기] 알림 타고 돌아왔을 때 UI가 영상 뒤로 숨지 않게 강제 기강 잡기!
        val loadingLayout = findViewById<View>(R.id.layout_camera_loading)
        val infoLayout = findViewById<View>(R.id.layout_info) // LinearLayout id 확인해라!
        val controllerLayout = findViewById<View>(R.id.layout_controller) // LinearLayout id 확인!

        // [쫀득] 아직 연결 전이라면 안내판을 다시 맨 앞으로 똬@악!
        if (SignCallService.currentCall?.state != android.telecom.Call.STATE_ACTIVE) {
            loadingLayout.bringToFront()
            infoLayout.bringToFront()
            controllerLayout.bringToFront()

            // 팩폭: bringToFront만으로 부족하면 억지로라도 다시 그려라!
            loadingLayout.requestLayout()
            loadingLayout.invalidate()
        }
    }

    private fun refreshCallUI() {
        val isConnected = SignCallService.currentCall?.state == android.telecom.Call.STATE_ACTIVE
        val loadingLayout = findViewById<View>(R.id.layout_camera_loading)

        if (isConnected) {
            // 연결된 상태면 안내문 치우고 애니메이션 가동!
            loadingLayout.visibility = View.GONE
            animateLocalVideoToCorner()
        } else {
            // [쫀득] 연결 전인데 내 얼굴이 보인다면?
            // 안내문 투명도를 조절해서 얼굴 위에 글자만 띄워라!
            loadingLayout.visibility = View.VISIBLE
            loadingLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            Log.d("VideoCall", "연결 전 프리뷰 모드 가동 중이다 이말이야!")
        }
    }

    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.bro.signtalk.CALL_ENDED" -> {
                    Log.d("VideoCall", "통화 종료! 자폭한다!")
                    finish()
                }
                "com.bro.signtalk.CALL_STARTED" -> {
                    Log.d("VideoCall", "통화 연결 완료! 안내판 치우고 엔진 시동 건다 이말이야! ㅇㅇ.")
                    findViewById<View>(R.id.layout_camera_loading).visibility = View.GONE
                    animateLocalVideoToCorner()

                    // [핵심] 이제 전역 변수라 에러 없이 찰지게 작동한다!
                    openCamera(localSurface.holder)
                    startVideoStreams()
                }
            }
        }
    }

    private fun initCamera() {
        // [팩폭] 이미 위에서 가@져왔으니 여기서 val로 또 만들지 마라!
        localSurface.setZOrderMediaOverlay(true)
        //localSurface.setZOrderOnTop(true)

        localSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d("VideoCall", "내 얼굴 도화지 펴졌다! 0.1초 만에 프리뷰 쏜다!")
                // [쫀득] 연결 전에는 그림만 그려놓고 카메라는 아직 안 깨워도 된다!
                startVideoStreams()
            }
            override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {
                startVideoStreams()
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) { closeCamera() }
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

        // [쌈뽕] 전역 변수에서 0.1초 만에 surface 낚아채기!
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

    // ... 나머지 openCamera, startPreview 등 기존 코드 유지 ...


    private fun openCamera(holder: SurfaceHolder) {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == currentCameraFacing
            } ?: cameraManager.cameraIdList[0]

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        startPreview(holder)
                    }
                    override fun onDisconnected(camera: CameraDevice) { closeCamera() }
                    override fun onError(camera: CameraDevice, error: Int) { closeCamera() }
                }, null)
            }
        } catch (e: Exception) { Log.e("VideoCall", "카메라 오픈 실패! 유남생?!?") }
    }

    private fun startPreview(holder: SurfaceHolder) {
        val device = cameraDevice ?: return
        val surface = holder.surface

        // [필살기] 도화지 알맹이가 진짜 있는지 0.1초 만에 검문해라!
        if (!surface.isValid) {
            Log.e("VideoCall", "도화지가 아직 축축해서 못 그린다 브로!")
            return
        }

        try {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(surface)

            // [쫀득] 기존 세션이 싹바가지 없게 살아있으면 확실히 박멸해라!
            captureSession?.close()
            captureSession = null

            device.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    // [핵심] 장치가 도중에 죽었는지 한 번 더 확인하는 디테일!
                    if (cameraDevice == null) return
                    captureSession = session
                    try {
                        // [쌈뽕] 세션 가동! 무한 반복 요청 쏴라!
                        session.setRepeatingRequest(builder.build(), null, null)
                        Log.d("VideoCall", "로컬 프리뷰 엔진 드디어 소생 완료! ㅇㅇ.")
                    } catch (e: Exception) {
                        Log.e("VideoCall", "반복 요청 실패(세션 충돌): ${e.message}")
                        // [필살기] 실패하면 0.5초 뒤에 찰지게 재시도해라!
                        findViewById<View>(android.R.id.content).postDelayed({
                            startPreview(holder)
                        }, 500)
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
        // [쫀득] 텔레콤 오디오 경로도 같이 바꿔줘야 찰지다!
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

    override fun onDestroy() {
        Log.d("VideoCall", "통화 종료 엔진 가동! 자원 0.1초 만에 싹 비운다!")
        // [필살기] 스트림 멱살부터 풀기!
        SignCallService.currentCall?.videoCall?.let {
            it.setDisplaySurface(null)
            it.setPreviewSurface(null)
        }
        closeCamera()
        super.onDestroy()
        try { unregisterReceiver(callReceiver) } catch (e: Exception) {}
    }
}