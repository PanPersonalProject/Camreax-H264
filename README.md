
# CameraX YUV420_888 to H264 Buffer

这是一个用于将 CameraX 输出的 YUV420_888 格式的图像数据转换为 H264 编码的组件。通过这个组件，您可以轻松地将 CameraX 的原始图像数据编码为 H264 格式，以便进行存储、或buffer流传输。

## 功能

1. **视频和音频采集与编码管理**
2. **将摄像头 YUV 图像转换为 H264 编码**

## 使用示例

### 1. 视频和音频采集与编码管理
通过 [StreamManager](camera_record/src/main/java/pan/lib/camera_record/media/StreamManager.kt) 管理视频和音频的采集与编码，里面包含 `CameraRecorder` 和 `AudioRecorder`。 (完整流程请参考 [CameraXPreviewFragment](app/src/main/java/pan/project/camerax_h264/CameraXPreviewFragment.kt))
```kotlin
        streamManager = StreamManager(
            requireContext(),
            viewLifecycleOwner,
            cameraPreviewInterface,
            aacInterface
        )
```
#### 1.1 `CameraPreviewInterface` 视频采集

```kotlin

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
```

#### 1.2 `AacInterface` 音频采集

```kotlin
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
```

### 2. 将摄像头 YUV 图像进行转换并显示的逻辑

#### 2.1 将 YUV_420_888 转换为 NV21

```kotlin
val nv21 = YuvUtil.YUV_420_888toNV21(imageProxy.image)
```

此行将图像数据从 `YUV_420_888` 格式转换为 `NV21` 格式。`NV21` 是摄像头预览和视频编码常用的格式。

#### 2.2 旋转 NV21 图像

```kotlin
val rotateNV21Right90 = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
    YuvUtil.rotateYUV420Degree270(nv21, imageProxy.width, imageProxy.height)
} else {
    YuvUtil.rotateYUV420Degree90(nv21, imageProxy.width, imageProxy.height)
}
```

此代码检查镜头朝向（前置或后置），并将 `NV21` 图像旋转 270 度（如果是前置摄像头）或 90 度（如果是后置摄像头）。这可确保图像在屏幕上正确显示。

#### 2.3 将 NV21 转换为 NV12

```kotlin
val nv12 = ByteArray(imageProxy.width * imageProxy.height * 3 / 2)
YuvUtil.NV21ToNV12(rotateNV21Right90, nv12, imageProxy.width, imageProxy.height)
```

此代码将旋转后的 `NV21` 图像转换为 `NV12` 格式。`NV12` 是 `MediaCodec` 需要的格式。

#### 2.4 从 NV12 图像创建位图

```kotlin
val bitmap = BitmapUtils.getBitmap(
    ByteBuffer.wrap(nv12),
    imageProxy.width,
    imageProxy.height,
    imageProxy.imageInfo.rotationDegrees
)
```

此行从 `NV12` 图像数据创建一个 `Bitmap` 对象。`rotationDegrees` 参数指定要应用于位图的旋转角度。

该代码在 UI 线程中使用创建的位图更新 `ImageView`。


## 引用:

```java
dependencies {
    implementation 'com.github.PanPersonalProject:CameraX-H264:1.2.0'
}
```
## IDE

Android Studio Ladybug | 2024.1.3 Canary 1

