package com.bro.signtalk.service


import android.app.Notification
import android.app.NotificationChannel
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bro.signtalk.R
import com.bro.signtalk.ui.CallActivity
import com.bro.signtalk.ui.IncomingCallActivity
import com.bro.signtalk.ui.contacts.VideoCallActivity
import kotlin.jvm.java

// [필살기] 상수 정의
const val ACTION_HANGUP = "com.bro.signtalk.HANGUP"
const val ACTION_SPEAKER = "com.bro.signtalk.SPEAKER"
const val ACTION_MUTE = "com.bro.signtalk.MUTE"

class SignCallService : InCallService() {

    private val TAG = "SignTalk_Service"
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    // SignCallService.kt 상단 수정
    companion object {
        var currentCall: Call? = null
        var instance: SignCallService? = null
        const val NOTIFICATION_ID = 1004
        // [핵심] v2를 붙여서 시스템한테 "새로운 무음 채널 파라!"고 멱살 잡아라!
        const val CHANNEL_ID = "call_service_channel_v2"
    }



    // [핵심/팩폭] 시스템이 울부짖던 그 채널ID로 0.1초 만에 채널 생성!
    // [쌈뽕] 채널 생성 엔진
    // SignCallService.kt 내부
    private fun createCallNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "수어 통화 서비스", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "브로의 쌈뽕한 통화 알림"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // [팩폭] 알림 자체의 진동과 소리는 싹바가지 없게 꺼버려라!
                enableVibration(false)
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlerts()
        instance = null
    }

    // SignCallService.kt 파일의 onCallAdded 부분
    // SignCallService.kt - 이@게 진짜 화면 띄우는 신호탄이다!

    // SignCallService.kt 의 onCallAdded를 이@렇게 고쳐라!
    // SignCallService.kt

    // SignCallService.kt 내부 (이게 없으면 화면 전환 절대 안 된다!)

    // SignCallService.kt 내부
    // SignCallService.kt 수정안
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call

        // [핵심] 콜백 등록! 이걸 안 하면 시스템 신호를 못 받는다 이말이야!
        call.registerCallback(callCallback)

        // [필살기] 전화 상태에 따라 쫀득하게 분기 처리!
        if (call.state == Call.STATE_RINGING) {
            // 1. 벨소리랑 진동 엔진 풀가동!
            startAlerts()

            // 2. 수신 전화 전용 화면(IncomingCallActivity) 소환!
            val intent = Intent(this, IncomingCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("receiver_phone", call.details.handle?.schemeSpecificPart)
            }
            startActivity(intent)

            // 3. 상단 알림창에도 "전화 왔다"고 기강 잡기!
            showCallNotification(call.details.handle?.schemeSpecificPart ?: "", isIncoming = true, isConnected = false)

        } else {
            // [팩폭] 묻지도 따지지도 않고 CallActivity 띄우던 과거는 성불시켜라!
            val videoState = call.details?.videoState ?: android.telecom.VideoProfile.STATE_AUDIO_ONLY
            val isVideoCall = videoState != android.telecom.VideoProfile.STATE_AUDIO_ONLY

            val targetActivity = if (isVideoCall) {
                Log.d(TAG, "영상 통화 발신 포착! 쌈뽕하게 VideoCallActivity 소환!")
                com.bro.signtalk.ui.contacts.VideoCallActivity::class.java
            } else {
                Log.d(TAG, "음성 통화 발신 포착! CallActivity 소환!")
                CallActivity::class.java
            }

            val intent = Intent(this, targetActivity).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("receiver_phone", call.details.handle?.schemeSpecificPart)
            }
            startActivity(intent)
        }

        Log.d(TAG, "시스템 전@화 포착! 상태별 화면 소환 완료했다 이말이야! ㅇㅇ.")
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            val phoneNumber = call.details.handle?.schemeSpecificPart ?: "알 수 없음"
            Log.d(TAG, "상태 변화 검거: $state 이말이야! ㅇㅇ.")

            // 1. [쫀득] 벨소리/진동 즉시 검거!
            if (state == Call.STATE_ACTIVE || state == Call.STATE_DISCONNECTED) {
                stopAlerts()
            }

            // 2. [필살기] 통화 연결(ACTIVE) 시 알림 교체 + 화면 타이머 시작 신호 발사!
            if (state == Call.STATE_ACTIVE) {
                Log.d(TAG, "연결 확인! 0.1초 만에 엔진 풀가동한다 이말이야! ㅇㅇ.")

                // [핵심] 알림창 문구 "통화 중"으로 교체!
                showCallNotification(phoneNumber, isIncoming = false, isConnected = true)

                // [필살기] CallActivity에 "타이머 시작해라!"라고 찰지게 신호 발사! 똬악!
                sendBroadcast(Intent("com.bro.signtalk.CALL_STARTED").apply {
                    setPackage(packageName)
                })

                // [쌈뽕] 오디오 모드 통화 중으로 강제 변경!
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            }

            // 3. [찰진] 통화 종료 시 세션 박멸 (이건 이미 잘하고 있다 브로!)
            if (state == Call.STATE_DISCONNECTED) {
                Log.d(TAG, "통화 종료! 0.1초 만에 정리 들어간다 이말이야!")
                cleanUpCallSession(call)
            }
        }
    }

    // SignCallService.kt의 onCreate나 onStartCommand에서 호출해라 브@로!
    private fun startForegroundService() {
        val channelId = "sign_talk_call_channel"
        val channelName = "수어 통화 알림"

        // 1. [쫀득] 알림 채널 기강 잡기 (안드로이드 8.0 이상 필수!)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // 2. [쌈뽕] 시스템에 보여줄 알림 객체 생성!
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SignTalk 전@화 중")
            .setContentText("통화가 활성화되어 있다 이말이야 ㅇㅇ.")
            .setSmallIcon(R.drawable.ic_call) // 브로 프로젝트의 아이콘으로 바꿔라!
            .setOngoing(true)
            .build()

        // 3. [핵심] 포그라운드 서비스 선언! (타입 꼭 지정해라!)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. 채널 먼저 찰지게 만들고!
        createCallNotificationChannel()

        // 2. [필살기] 초기 알림 띄우기
        // 팩폭: 여기서도 CHANNEL_ID를 써야 아까 만든 채널이랑 톱니바퀴가 맞는다 유남생?!?
        val initialNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle("SignTalk 서비스")
            .setContentText("전화 대기 중이다 이말이야 ㅇㅇ.")
            .setPriority(NotificationCompat.PRIORITY_MAX) // 통화는 계급이 높아야지!
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()

        // 3. 포그라운드 서비스 기강 잡기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        // 진동자 세팅은 그대로 유지해라 브로!
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun showCallNotification(phoneNumber: String, isIncoming: Boolean, isConnected: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val displayName = getContactName(phoneNumber)

        // 1. [필살기] 영상 통화 여부 판단
        val videoState = currentCall?.details?.videoState ?: android.telecom.VideoProfile.STATE_AUDIO_ONLY
        val supportsVideo = currentCall?.details?.can(0x00000400) ?: false
        val isVideoCall = videoState != android.telecom.VideoProfile.STATE_AUDIO_ONLY || supportsVideo

        // 2. [쌈뽕] 상태에 따른 타겟 Activity 결정
        val targetActivity = when {
            isIncoming && !isConnected -> IncomingCallActivity::class.java
            isVideoCall -> VideoCallActivity::class.java
            else -> CallActivity::class.java
        }

        val activityIntent = Intent(this, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("receiver_phone", phoneNumber)
            putExtra("receiver_name", displayName)
        }

        val pendingActivity = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val hangupIntent = Intent(this, SignCallReceiver::class.java).apply { action = ACTION_HANGUP }
        val pendingHangup = PendingIntent.getBroadcast(this, 0, hangupIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val person = Person.Builder().setName(displayName).build()

        // 3. [쫀득] 알림 빌더 기강 잡기
        // SignCallService.kt 내부 showCallNotification 함수의 핵심 부분
        // [필살기] 알림 빌더 기강 확실히 잡아라!
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call)
            .setPriority(NotificationCompat.PRIORITY_MAX) // [핵심] 최상위 계급!
            .setCategory(NotificationCompat.CATEGORY_CALL) // [쌈뽕] 통화 카테고리!
            .setFullScreenIntent(pendingActivity, true) // [필살기] 이게 있어야 화면이 똬악 뜬다!
            .setOngoing(true) // [쫀득] 사용자가 못 지우게 막아라!
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        // 4. [핵심] 연결 상태에 따른 텍스트 및 스타일 엔진 분기
        if (!isConnected) {
            builder.setContentTitle(if (isIncoming) "전화 왔다 이말이야!" else "전화 거는 중이다 이말이야!")
                .setContentText("$displayName 브로와 연결 중...")
                .setUsesChronometer(false)
                .setStyle(if (isIncoming) {
                    NotificationCompat.CallStyle.forIncomingCall(person, pendingHangup, pendingActivity)
                } else {
                    NotificationCompat.CallStyle.forOngoingCall(person, pendingHangup)
                })
        } else {
            builder.setContentTitle("$displayName 브로와 통화 중!")
                .setContentText(if (isVideoCall) "쌈뽕한 영상 통화 중..." else "쫀득한 음성 대화 중...")
                .setUsesChronometer(true)
                .setWhen(System.currentTimeMillis()) // 연결된 그 순간부터 타이머 시작!
                .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, pendingHangup))
        }

        // 5. [팩폭] 안드로이드 14 대응: startForeground 선빵 필승!
        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        // notificationManager.notify는 startForeground가 알아서 해주니깐 생략해도 된다 이말이야!
    }

    // [필살기] 연락처 DB에서 이름 가져오는 마법의 함수!
    private fun getContactName(phoneNumber: String): String {
        if (phoneNumber.isEmpty()) return "알 수 없음"
        val uri = android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(phoneNumber))
        val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)

        return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else phoneNumber
        } ?: phoneNumber
    }



    private fun stopCallNotification() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        cleanUpCallSession(call)
    }


    // [필살기] 공통 세션 청소 함수! 이거 하나면 다음 전화 쫀득하게 된다!
    private fun cleanUpCallSession(call: Call) {
        Log.d(TAG, "세션 청소 드가자! ㅇㅇ.")
        stopCallNotification()
        stopAlerts()

        if (currentCall == call) currentCall = null
        call.unregisterCallback(callCallback)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL

        sendBroadcast(Intent("com.bro.signtalk.CALL_ENDED").apply { setPackage(packageName) })
    }

    private fun startAlerts() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            try {
                val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
                ringtone = RingtoneManager.getRingtone(this, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ringtone?.isLooping = true
                ringtone?.play()
            } catch (e: Exception) { }
            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            else vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlerts() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
    }
}