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
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                
                val task = textRecognizer.process(image)
                task
                    .addOnSuccessListener { visionText: Text ->
                        if (continuation.isActive) {
                            val extractedText = visionText.text ?: ""
                            android.util.Log.d("OCRService", "Successfully extracted text: ${extractedText.length} characters")
                            continuation.resume(extractedText)
                        }
                    }
                    .addOnFailureListener { exception: Exception ->
                        if (continuation.isActive) {
                            android.util.Log.e("OCRService", "OCR processing failed", exception)
                            continuation.resumeWithException(exception)
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    android.util.Log.e("OCRService", "Error creating InputImage", e)
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    fun close() {
        textRecognizer.close()
    }
}
