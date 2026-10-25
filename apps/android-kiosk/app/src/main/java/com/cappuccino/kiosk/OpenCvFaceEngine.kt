package com.cappuccino.kiosk

import android.content.Context
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.objdetect.FaceDetectorYN
import org.opencv.objdetect.FaceRecognizerSF
import java.io.File
import kotlin.math.abs
import kotlin.math.min

class FaceObservation(
    val faceCount: Int,
    val yawRatio: Float,
    val sizeRatio: Float,
    val embedding: FloatArray?,
)

class OpenCvFaceEngine(context: Context) {
    private val detector: FaceDetectorYN
    private val recognizerModel = copyAsset(context, "face_recognition_sface_2021dec.onnx")
    private val recognizer: FaceRecognizerSF by lazy {
        FaceRecognizerSF.create(recognizerModel.absolutePath, "")
    }

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

    /**
     * Detects faces and, when exactly one is present, reports where the nose sits between the eyes
     * (0.5 = facing the camera; near 0 or 1 = head turned) and optionally its SFace embedding.
     */
    fun observe(bgrFrame: Mat, withEmbedding: Boolean): FaceObservation {
        require(!bgrFrame.empty()) { "Face input frame is empty" }
        require(bgrFrame.channels() == 3) { "Face input frame must have three color channels" }

        detector.setInputSize(Size(bgrFrame.cols().toDouble(), bgrFrame.rows().toDouble()))
        val detections = Mat()
        val alignedFace = Mat()
        val features = Mat()
        try {
            detector.detect(bgrFrame, detections)
            if (detections.rows() != 1) return FaceObservation(detections.rows(), 0.5f, 0f, null)

            // YuNet row: x, y, w, h, right eye (x, y), left eye (x, y), nose (x, y), mouth corners, score.
            val row = FloatArray(15)
            detections.get(0, 0, row)
            val eyeSpan = abs(row[6] - row[4])
            val yawRatio = if (eyeSpan > 1f) (row[8] - min(row[4], row[6])) / eyeSpan else 0.5f
            val sizeRatio = row[2] / bgrFrame.cols()

            val embedding = if (withEmbedding) {
                recognizer.alignCrop(bgrFrame, detections.row(0), alignedFace)
                recognizer.feature(alignedFace, features)
                if (features.type() == CvType.CV_32FC1 && features.total() > 0L) {
                    FloatArray(features.total().toInt()).also { features.get(0, 0, it) }
                } else {
                    null
                }
            } else {
                null
            }
            return FaceObservation(1, yawRatio, sizeRatio, embedding)
        } finally {
            detections.release()
            alignedFace.release()
            features.release()
        }
    }

    fun loadRecognitionModel() {
        recognizer.getNativeObjAddr()
    }

    fun extractEmbedding(bgrFrame: Mat): FloatArray? {
        require(!bgrFrame.empty()) { "Face input frame is empty" }
        require(bgrFrame.channels() == 3) { "Face input frame must have three color channels" }

        detector.setInputSize(Size(bgrFrame.cols().toDouble(), bgrFrame.rows().toDouble()))
        val detections = Mat()
        val alignedFace = Mat()
        val features = Mat()
        return try {
            detector.detect(bgrFrame, detections)
            if (detections.rows() != 1) return null

            recognizer.alignCrop(bgrFrame, detections.row(0), alignedFace)
            recognizer.feature(alignedFace, features)
            if (features.type() != CvType.CV_32FC1 || features.total() == 0L) return null

            FloatArray(features.total().toInt()).also { features.get(0, 0, it) }
        } finally {
            detections.release()
            alignedFace.release()
            features.release()
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