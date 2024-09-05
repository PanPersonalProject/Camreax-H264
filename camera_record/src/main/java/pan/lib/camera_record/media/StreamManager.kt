package pan.lib.camera_record.media

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import pan.lib.camera_record.media.audio.AacInterface
import pan.lib.camera_record.media.audio.AudioRecorder
import pan.lib.camera_record.media.video.CameraPreviewInterface
import pan.lib.camera_record.media.video.CameraRecorder

/**
 * StreamManager 管理音频和视频录制，使用 AudioRecorder 和 CameraRecorder。
 * 它提供了用于启动、停止和切换摄像头的功能。
 *
 * @param context 应用上下文。
 * @param lifecycleOwner 用于管理摄像头生命周期的 LifecycleOwner。
 * @param cameraPreviewInterface 摄像头预览接口
 * @param aacInterface AAC音频接口
 */
open class StreamManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val cameraPreviewInterface: CameraPreviewInterface,
    private val aacInterface: AacInterface
) {
    private val cameraRecorder: CameraRecorder by lazy {
        CameraRecorder(
            context,
            lifecycleOwner,
            cameraPreviewInterface
        )
    }

    private val audioRecorder: AudioRecorder by lazy {
        AudioRecorder(context, aacInterface)
    }

    /**
     * 启动摄像头和音频录制。
     */
    fun start() {
        cameraRecorder.startRecording()
        audioRecorder.startRecording()
    }

    /**
     * 停止摄像头和音频录制。
     */
    fun stop() {
        cameraRecorder.stopRecording()
        audioRecorder.stopRecording()
    }

    /**
     * 切换前后摄像头。
     */
    fun switchCamera() {
        cameraRecorder.switchCamera()
    }
}
