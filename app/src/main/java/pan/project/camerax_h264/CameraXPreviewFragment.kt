package pan.project.camerax_h264

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import pan.lib.camera_record.file.FileUtil
import pan.lib.camera_record.media.StreamManager
import pan.lib.camera_record.media.audio.AacInterface
import pan.lib.camera_record.media.video.CameraPreviewInterface
import pan.lib.camera_record.media.yuv.BitmapUtils
import java.nio.ByteBuffer
import android.media.MediaCodec
import android.media.MediaFormat
import pan.project.camerax_h264.databinding.FragmentCameraPreviewBinding

/**
 * @author pan qi
 * @since 2024/2/3
 */
class CameraXPreviewFragment : Fragment() {

    private lateinit var binding: FragmentCameraPreviewBinding
    private lateinit var streamManager: StreamManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        streamManager = StreamManager(
            requireContext(),
            viewLifecycleOwner,
            cameraPreviewInterface,
            aacInterface
        )

        binding.cameraSwitchButton.setOnClickListener {
            streamManager.switchCamera()
        }

        binding.stopButton.setOnClickListener {
            streamManager.stop()
        }

        requestPermissions()

    }

    private val cameraPreviewInterface = object : CameraPreviewInterface {
        val needSaveH264ToLocal = false // 是否保存h264到本地
        override fun getPreviewView(): PreviewView = binding.prewview

        override fun onNv21Frame(nv21: ByteArray, imageProxy: ImageProxy) {
            val bitmap = BitmapUtils.getBitmap(
                ByteBuffer.wrap(nv21),
                imageProxy.width,
                imageProxy.height,
                imageProxy.imageInfo.rotationDegrees
            )
            binding.myImageView.post {
                binding.myImageView.setImageBitmap(bitmap)
            }
        }

        override fun onSpsPpsVps(sps: ByteBuffer, pps: ByteBuffer?, vps: ByteBuffer?) {
            "CameraXPreviewFragment".logByteBufferContent("SPS", sps)
            "CameraXPreviewFragment".logByteBufferContent("PPS", pps)
            "CameraXPreviewFragment".logByteBufferContent("VPS", vps)//only for H265
        }

        override fun onVideoBuffer(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
            val data: ByteArray
            if (h264Buffer.hasArray()) {
                data = h264Buffer.array()
            } else {
                data = ByteArray(h264Buffer.remaining())
                h264Buffer.get(data)
            }
            if (needSaveH264ToLocal) {
                FileUtil.writeBytesToFile(requireContext(), data, "test.h264")
            }
        }
    }

    private val aacInterface = object : AacInterface {
        val needSaveAacToLocal = false // 是否保存aac到本地
        override fun getAacData(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
            val data: ByteArray
            if (aacBuffer.hasArray()) {
                data = aacBuffer.array()
            } else {
                data = ByteArray(aacBuffer.remaining())
                aacBuffer.get(data)
            }

            if (needSaveAacToLocal) {
                FileUtil.writeBytesToFile(requireContext(), data, "test.aac")
            }
        }

        override fun onAudioFormat(mediaFormat: MediaFormat) {
            Log.d("CameraXPreviewFragment", "onAudioFormat: $mediaFormat")
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionsResult(permissions)
        }

    private fun requestPermissions() {
        // 分别检查相机和录音权限的状态
        val cameraPermissionStatus = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        )
        val recordAudioPermissionStatus = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        )

        // 如果任一权限未被授予，则发起权限请求
        if (cameraPermissionStatus != PackageManager.PERMISSION_GRANTED ||
            recordAudioPermissionStatus != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        } else {
            // 权限确认后，启动streamManager
            binding.root.post {
                streamManager.start()
            }
        }
    }

    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            binding.root.post {
                streamManager.start()
            }
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys.joinToString("\n")
            showPermissionDeniedDialog(deniedPermissions)
        }
    }

    private fun showPermissionDeniedDialog(deniedPermissions: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("以下权限被拒绝")
            .setMessage(deniedPermissions)
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onDestroyView() {
        streamManager.stop()
        super.onDestroyView()
    }

    private fun String.logByteBufferContent(name: String, byteBuffer: ByteBuffer?) {
        if (byteBuffer == null) {
            Log.d(this, "$name is null")
            return
        }

        val byteData = ByteArray(byteBuffer.remaining())
        byteBuffer.rewind()
        byteBuffer.get(byteData)

        val hexString = byteData.joinToString(separator = " ") { "%02X".format(it) }
        Log.d(this, "$name: $hexString")
    }
}
