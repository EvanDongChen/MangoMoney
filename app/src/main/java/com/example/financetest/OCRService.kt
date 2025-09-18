package com.example.financetest

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OCRService {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(bitmap: Bitmap): String {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            
            val task = textRecognizer.process(image)
            task
                .addOnSuccessListener { visionText: Text ->
                    if (continuation.isActive) continuation.resume(visionText.text)
                }
                .addOnFailureListener { exception: Exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
        }
    }

    fun close() {
        textRecognizer.close()
    }
}
