package com.company.azrylvsmark.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageCompressor {
    private const val MAX_DIMENSION = 1280
    private const val COMPRESS_QUALITY = 82

    suspend fun compressImageUri(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open uri")

        // First decode bounds
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        // Calculate sample size
        val width = options.outWidth
        val height = options.outHeight
        val maxSide = max(width, height)
        var inSampleSize = 1
        if (maxSide > MAX_DIMENSION) {
            inSampleSize = (maxSide.toFloat() / MAX_DIMENSION.toFloat()).roundToInt()
        }

        // Decode actual bitmap
        val decodeStream = context.contentResolver.openInputStream(uri)
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
        decodeStream?.close()

        val outputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outputStream)
        bitmap?.recycle()
        outputStream.toByteArray()
    }

    suspend fun saveCompressedToFile(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val bytes = compressImageUri(context, uri)
        val tempFile = File(context.cacheDir, "az_img_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { it.write(bytes) }
        tempFile
    }
}
