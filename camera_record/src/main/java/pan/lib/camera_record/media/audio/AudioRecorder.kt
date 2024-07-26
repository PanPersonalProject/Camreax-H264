package pan.lib.camera_record.media.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("MissingPermission")
class AudioRecorder(aacInterface: AacInterface) {
    private val sampleRate = 44100 // 采样频率为 44100 Hz
    private val channelInMono = AudioFormat.CHANNEL_IN_MONO // 单声道
    private val encodingPcm16Bit = AudioFormat.ENCODING_PCM_16BIT // 量化精度为 16 位
    private val recordStarted = AtomicBoolean(false) // 是否开始录音
    private val minBufferSize: Int =
        AudioRecord.getMinBufferSize(sampleRate, channelInMono, encodingPcm16Bit) // 音频最小缓冲区大小

    private var audioRecord: AudioRecord
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val aacEncoder = AacEncoder(sampleRate, 1, 128000, true, aacInterface) // 比特率为 128 kbps

    init {
        try {
            // 尝试使用 VOICE_COMMUNICATION 音频源，VOICE_COMMUNICATION 会自动启用回声消除（AEC）、自动增益控制（AGC）和噪声抑制（NS）等音频处理功能
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelInMono,
                encodingPcm16Bit,
                minBufferSize
            )
            Log.d("AudioRecorder", "Using VOICE_COMMUNICATION audio source")
        } catch (e: Exception) {
            // 如果 VOICE_COMMUNICATION 不可用，回退到 MIC 并启用 AcousticEchoCanceler、NoiseSuppressor 和 AutomaticGainControl
            Log.d(
                "AudioRecorder",
                "VOICE_COMMUNICATION not supported, using MIC with AcousticEchoCanceler, NoiseSuppressor, and AutomaticGainControl"
            )
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelInMono,
                encodingPcm16Bit,
                minBufferSize
            )

            setupAudioEnhancements()
        }
    }

    //设置音频增强效果
    private fun setupAudioEnhancements() {
        if (AcousticEchoCanceler.isAvailable()) { // 判断回声消除是否可用
            // 启用回声消除
            echoCanceler = AcousticEchoCanceler.create(audioRecord.audioSessionId)
            echoCanceler?.enabled = true
            Log.d("AudioRecorder", "AcousticEchoCanceler enabled: ${echoCanceler?.enabled}")
        } else {
            Log.d("AudioRecorder", "AcousticEchoCanceler not available")
        }

        if (NoiseSuppressor.isAvailable()) { // 判断噪声抑制是否可用
            // 启用噪声抑制
            noiseSuppressor = NoiseSuppressor.create(audioRecord.audioSessionId)
            noiseSuppressor?.enabled = true
            Log.d("AudioRecorder", "NoiseSuppressor enabled: ${noiseSuppressor?.enabled}")
        } else {
            Log.d("AudioRecorder", "NoiseSuppressor not available")
        }

        if (AutomaticGainControl.isAvailable()) { // 判断自动增益控制是否可用
            // 启用自动增益控制
            automaticGainControl = AutomaticGainControl.create(audioRecord.audioSessionId)
            automaticGainControl?.enabled = true
            Log.d(
                "AudioRecorder",
                "AutomaticGainControl enabled: ${automaticGainControl?.enabled}"
            )
        } else {
            Log.d("AudioRecorder", "AutomaticGainControl not available")
        }
    }

    fun startRecording() {
        if (recordStarted.get()) return // 已经在录音中

        recordStarted.set(true)
        aacEncoder.initialize()

        scope.launch {
            audioRecord.startRecording()
            val buffer = ByteArray(minBufferSize)
            try {
                while (recordStarted.get()) {
//                    Log.d(
//                        "AudioRecorder",
//                        "Before read: state=${audioRecord.recordingState}, bufferSize=minBufferSize"
//                    )

                    val read = audioRecord.read(buffer, 0, minBufferSize)
                    if (read > 0) {
                        aacEncoder.encode(buffer, read)
                    } else {
                        Log.e("AudioRecorder", "Error reading from AudioRecord: $read")
                        delay(10)
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioRecorder", "AudioRecorder error: $e")
            } finally {
                aacEncoder.finalizeEncoding()
                audioRecord.stop()
                audioRecord.release()
                echoCanceler?.release()
                noiseSuppressor?.release()
                automaticGainControl?.release()
            }
        }
    }

    fun stopRecording() {
        recordStarted.set(false)
    }
}
