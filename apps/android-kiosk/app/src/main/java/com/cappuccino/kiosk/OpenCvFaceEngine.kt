package com.cappuccino.kiosk

import android.content.Context
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.objdetect.FaceDetectorYN
import java.io.File

class OpenCvFaceEngine(context: Context) {
    private val detector: FaceDetectorYN

    init {
        System.loadLibrary("opencv_java4")
        val detectorModel = copyAsset(context, "face_detection_yunet_2023mar.onnx")
        detector = FaceDetectorYN.create(
            detectorModel.absolutePath,
            "",
            Size(320.0, 320.0),
            0.9f,
            0.3f,
            5000,
        )
    }

    fun countFaces(bgrFrame: Mat): Int {
        require(!bgrFrame.empty()) { "Face input frame is empty" }
        require(bgrFrame.channels() == 3) { "Face input frame must have three color channels" }

        detector.setInputSize(Size(bgrFrame.cols().toDouble(), bgrFrame.rows().toDouble()))
        val detections = Mat()
        return try {
            detector.detect(bgrFrame, detections)
            detections.rows()
        } finally {
            detections.release()
        }
    }

    private fun copyAsset(context: Context, fileName: String): File {
        val model = File(context.filesDir, "biometric-models/$fileName")
        if (!model.exists() || model.length() == 0L) {
            model.parentFile?.mkdirs()
            context.assets.open("models/$fileName").use { input ->
                model.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return model
    }
}