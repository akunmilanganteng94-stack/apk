package com.company.azrylvsmark.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log

class AudioSwitchManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var previousAudioMode = AudioManager.MODE_NORMAL
    private var isSpeakerOn = false

    fun startCallAudio(enableSpeaker: Boolean = false) {
        try {
            previousAudioMode = audioManager.mode
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isMicrophoneMute = false
            setSpeakerphoneOn(enableSpeaker)
        } catch (e: Exception) {
            Log.e("AudioSwitchManager", "Error starting call audio", e)
        }
    }

    fun setSpeakerphoneOn(on: Boolean) {
        try {
            isSpeakerOn = on
            audioManager.isSpeakerphoneOn = on
        } catch (e: Exception) {
            Log.e("AudioSwitchManager", "Error setting speakerphone", e)
        }
    }

    fun toggleSpeakerphone(): Boolean {
        setSpeakerphoneOn(!isSpeakerOn)
        return isSpeakerOn
    }

    fun setMicrophoneMute(mute: Boolean) {
        try {
            audioManager.isMicrophoneMute = mute
        } catch (e: Exception) {
            Log.e("AudioSwitchManager", "Error toggling microphone mute", e)
        }
    }

    fun stopCallAudio() {
        try {
            audioManager.isSpeakerphoneOn = false
            audioManager.isMicrophoneMute = false
            audioManager.mode = previousAudioMode
        } catch (e: Exception) {
            Log.e("AudioSwitchManager", "Error stopping call audio", e)
        }
    }
}
