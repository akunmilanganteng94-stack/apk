package com.company.azrylvsmark.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMillis: Long = 0L

    fun startRecording(): File? {
        val file = File(context.cacheDir, "az_voice_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)

            try {
                prepare()
                start()
                startTimeMillis = System.currentTimeMillis()
            } catch (e: IOException) {
                Log.e("AudioRecorderHelper", "prepare() failed", e)
                return null
            } catch (e: IllegalStateException) {
                Log.e("AudioRecorderHelper", "start() failed", e)
                return null
            }
        }
        return file
    }

    fun stopRecording(): Pair<File?, Int> {
        val durationSec = ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Error stopping recorder", e)
        } finally {
            mediaRecorder = null
        }
        val file = currentOutputFile
        currentOutputFile = null
        return Pair(file, maxOf(1, durationSec))
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.w("AudioRecorderHelper", "Exception canceling recorder", e)
        } finally {
            mediaRecorder = null
            currentOutputFile?.delete()
            currentOutputFile = null
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
