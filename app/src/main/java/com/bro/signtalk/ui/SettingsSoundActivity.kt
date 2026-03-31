package com.bro.signtalk.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.bro.signtalk.R
import android.provider.Settings
class SettingsSoundActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    private var currentRingtoneUri: Uri? = null

    // 1. [필살기] 벨소리 선택 후 돌아오는 배달 기사!
    private val ringtoneLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                currentRingtoneUri = it

                // [필살기] 시스템 설정 변경 권한이 있는지 0.1초 만에 확인!
                if (Settings.System.canWrite(this)) {
                    // [쫀득] 드디어 진짜 시스템 벨소리를 똬악 갈아치운다!
                    RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, it)
                    Toast.makeText(this, "시스템 벨소리 쌈뽕하게 변경 완료!", Toast.LENGTH_SHORT).show()
                } else {
                    // [팩폭] 권한 없으면 설정 화면으로 0.1초 만에 보낸다!
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    Toast.makeText(this, "설정 변경 권한부터 줘라 브@로!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_sound)

        // [쫀득] 매니저들 초기화 똬악!
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        setupSoundSettings()
        setupVibrationSettings()
    }

    private fun setupSoundSettings() {
        val soundSwitch = findViewById<SwitchCompat>(R.id.switch_sound_enable)
        val ringtoneBtn = findViewById<Button>(R.id.btn_select_ringtone)
        val volumeSeekBar = findViewById<SeekBar>(R.id.seekbar_volume)

        // 소리 모드 상태 반영
        soundSwitch.isChecked = audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL

        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            // [팩폭] 스위치 끄면 무음 모드 진입이다 유남생?!?
            audioManager.ringerMode = if (isChecked) AudioManager.RINGER_MODE_NORMAL else AudioManager.RINGER_MODE_SILENT
        }

        // 벨소리 선택창 소환
        ringtoneBtn.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "벨소리 골라라 브@로!")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentRingtoneUri)
            }
            ringtoneLauncher.launch(intent)
        }

        // 볼륨 바 조절
        volumeSeekBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_RING, progress, 0)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun setupVibrationSettings() {
        val vibrateSwitch = findViewById<SwitchCompat>(R.id.switch_vibrate_enable)
        val vibrationSeekBar = findViewById<SeekBar>(R.id.seekbar_vibration)
        val vibrationText = findViewById<TextView>(R.id.tv_vibration_level)

        vibrationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                // progress: 0, 1, 2, 3, 4 (5단계)
                val level = progress + 1

                // [필살기] 바를 옮길 때마다 텍스트도 쌈뽕하게 바꿔주기!
                vibrationText.text = "진동 세기: ${level}단계"

                if (fromUser && vibrateSwitch.isChecked) {
                    // [쫀득] 단계별 세기 계산 (51, 102, ..., 255)
                    val strength = (level * 51).coerceAtMost(255)

                    // [찰진] 0.1초 동안 똬악 울려주기!
                    val effect = VibrationEffect.createOneShot(100, strength)
                    vibrator.vibrate(effect)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // [팩폭] 진동 스위치 꺼져있으면 바 못 만지게 막는 것도 싹바가지 있는 UI다!
        vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            vibrationSeekBar.isEnabled = isChecked
            vibrationText.alpha = if (isChecked) 1.0f else 0.5f
        }
    }
}