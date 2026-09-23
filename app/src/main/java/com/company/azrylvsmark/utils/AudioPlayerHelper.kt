package com.company.azrylvsmark.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

class AudioPlayerHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingUrl: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    var onProgressUpdate: ((progress: Float, currentMs: Int, totalMs: Int) -> Unit)? = null
    var onPlaybackCompleted: (() -> Unit)? = null
    var onPlaybackStarted: (() -> Unit)? = null

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    fun play(urlOrPath: String) {
        if (currentPlayingUrl == urlOrPath && mediaPlayer != null) {
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer?.start()
                startProgressTracker()
                onPlaybackStarted?.invoke()
            }
            return
        }

        stop()
        currentPlayingUrl = urlOrPath

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
                    setDataSource(context, Uri.parse(urlOrPath))
                } else {
                    setDataSource(urlOrPath)
                }
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    startProgressTracker()
                    onPlaybackStarted?.invoke()
                }
                setOnCompletionListener {
                    stopProgressTracker()
                    onProgressUpdate?.invoke(0f, 0, it.duration)
                    onPlaybackCompleted?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Error initializing player", e)
            stop()
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                stopProgressTracker()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Error pausing player", e)
        }
    }

    fun stop() {
        stopProgressTracker()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Error releasing player", e)
        } finally {
            mediaPlayer = null
            currentPlayingUrl = null
        }
    }

    fun seekTo(progressRatio: Float) {
        mediaPlayer?.let { mp ->
            val targetMs = (mp.duration * progressRatio.coerceIn(0f, 1f)).toInt()
            mp.seekTo(targetMs)
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying && mp.duration > 0) {
                        val current = mp.currentPosition
                        val total = mp.duration
                        val ratio = current.toFloat() / total.toFloat()
                        onProgressUpdate?.invoke(ratio, current, total)
                        handler.postDelayed(this, 100)
                    }
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressTracker() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }
}
