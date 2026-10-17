package com.cappuccino.kiosk

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.core.CvType
import org.opencv.core.Mat

@RunWith(AndroidJUnit4::class)
class OpenCvFaceEngineTest {
    @Test
    fun loadsLicensedModelsAndFindsNoFaceInBlankFrame() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = OpenCvFaceEngine(context)
        val blankFrame = Mat.zeros(320, 320, CvType.CV_8UC3)

        try {
            assertEquals(0, engine.countFaces(blankFrame))
        } finally {
            blankFrame.release()
        }
    }
}