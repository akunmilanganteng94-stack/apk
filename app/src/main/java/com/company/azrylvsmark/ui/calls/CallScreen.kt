package com.company.azrylvsmark.ui.calls

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.company.azrylvsmark.data.model.CallSession
import com.company.azrylvsmark.data.model.UserProfile
import com.company.azrylvsmark.data.repository.CallRepository
import com.company.azrylvsmark.ui.components.AzmassangeAvatar
import com.company.azrylvsmark.ui.theme.CardNavy
import com.company.azrylvsmark.ui.theme.ContainerNavy
import com.company.azrylvsmark.ui.theme.DeepNavy
import com.company.azrylvsmark.ui.theme.ElectricBlue
import com.company.azrylvsmark.ui.theme.ErrorRed
import com.company.azrylvsmark.ui.theme.NeonCyan
import com.company.azrylvsmark.ui.theme.OnlineGreen
import com.company.azrylvsmark.ui.theme.TextPrimaryDark
import com.company.azrylvsmark.ui.theme.TextSecondaryDark
import com.company.azrylvsmark.utils.DateTimeUtils
import com.company.azrylvsmark.webrtc.AudioSwitchManager
import com.company.azrylvsmark.webrtc.WebRtcClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallScreen(
    callId: String,
    currentUser: UserProfile?,
    otherUid: String,
    otherName: String,
    otherPhoto: String,
    isVideo: Boolean,
    isIncoming: Boolean,
    callRepository: CallRepository,
    onCallEnded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callSession by callRepository.observeCallSession(callId).collectAsState(initial = null)

    var isMicMuted by remember { mutableStateOf(false) }
    var isVideoDisabled by remember { mutableStateOf(!isVideo) }
    var isSpeakerphoneOn by remember { mutableStateOf(isVideo) }
    var callDurationSec by remember { mutableIntStateOf(0) }

    var localRenderer: SurfaceViewRenderer? by remember { mutableStateOf(null) }
    var remoteRenderer: SurfaceViewRenderer? by remember { mutableStateOf(null) }
    var remoteStreamState: MediaStream? by remember { mutableStateOf(null) }

    val audioSwitchManager = remember { AudioSwitchManager(context) }
    var webRtcClient: WebRtcClient? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        audioSwitchManager.startCallAudio(enableSpeaker = isVideo)
        val client = WebRtcClient(
            context = context,
            onIceCandidateGenerated = { candidate ->
                scope.launch {
                    val candidateMap = mapOf<String, Any>(
                        "sdp" to candidate.sdp,
                        "sdpMid" to (candidate.sdpMid ?: ""),
                        "sdpMLineIndex" to candidate.sdpMLineIndex
                    )
                    callRepository.sendIceCandidate(callId, !isIncoming, candidateMap)
                }
            },
            onRemoteTrackReceived = { stream ->
                remoteStreamState = stream
                remoteRenderer?.let { renderer ->
                    stream.videoTracks.firstOrNull()?.addSink(renderer)
                }
            }
        )
        webRtcClient = client
        client.startLocalAudio()
        if (isVideo) {
            client.startLocalVideo()
        }
        client.initPeerConnection(isVideo)

        if (!isIncoming) {
            client.createOffer { sdp ->
                scope.launch {
                    callRepository.sendOfferSdp(callId, sdp.description)
                }
            }
        }

        onDispose {
            audioSwitchManager.stopCallAudio()
            client.close()
            localRenderer?.release()
            remoteRenderer?.release()
        }
    }

    LaunchedEffect(callSession?.status) {
        val session = callSession ?: return@LaunchedEffect
        if (session.status == CallSession.STATUS_ENDED || session.status == CallSession.STATUS_REJECTED) {
            onCallEnded()
        }
    }

    val remoteOffer by callRepository.observeOffer(callId).collectAsState(initial = null)
    LaunchedEffect(remoteOffer) {
        val sdp = remoteOffer
        if (isIncoming && sdp != null && sdp.isNotBlank()) {
            webRtcClient?.setRemoteDescription(sdp, isOffer = true) {
                webRtcClient?.createAnswer { answer ->
                    scope.launch {
                        callRepository.sendAnswerSdp(callId, answer.description)
                    }
                }
            }
        }
    }

    val remoteAnswer by callRepository.observeAnswer(callId).collectAsState(initial = null)
    LaunchedEffect(remoteAnswer) {
        val sdp = remoteAnswer
        if (!isIncoming && sdp != null && sdp.isNotBlank()) {
            webRtcClient?.setRemoteDescription(sdp, isOffer = false)
        }
    }

    LaunchedEffect(Unit) {
        callRepository.observeRemoteCandidates(callId, !isIncoming).collect { map ->
            val sdp = map["sdp"] as? String ?: ""
            val sdpMid = map["sdpMid"] as? String ?: ""
            val lineIndex = (map["sdpMLineIndex"] as? Number)?.toInt() ?: 0
            if (sdp.isNotBlank()) {
                val candidate = IceCandidate(sdpMid, lineIndex, sdp)
                webRtcClient?.addRemoteIceCandidate(candidate)
            }
        }
    }

    LaunchedEffect(callSession?.status) {
        if (callSession?.status == CallSession.STATUS_CONNECTED) {
            while (true) {
                delay(1000)
                callDurationSec++
            }
        }
    }

    fun endCall() {
        scope.launch {
            callRepository.endCall(callId, callDurationSec)
            onCallEnded()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .testTag("call_screen")
    ) {
        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        init(webRtcClient?.eglBase?.eglBaseContext, null)
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        setEnableHardwareScaler(true)
                        remoteRenderer = this
                        remoteStreamState?.videoTracks?.firstOrNull()?.addSink(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .padding(top = 48.dp, end = 20.dp)
                    .size(width = 110.dp, height = 150.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, NeonCyan, RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            init(webRtcClient?.eglBase?.eglBaseContext, null)
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                            setMirror(true)
                            localRenderer = this
                            webRtcClient?.localVideoTrack?.addSink(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (!isVideo || remoteStreamState == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AzmassangeAvatar(photoUrl = otherPhoto, displayName = otherName, size = 110.dp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = otherName,
                    color = TextPrimaryDark,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val statusText = when (callSession?.status) {
                    CallSession.STATUS_RINGING -> "Memanggil..."
                    CallSession.STATUS_CONNECTED -> DateTimeUtils.formatDuration(callDurationSec)
                    else -> "Menghubungkan..."
                }
                Text(
                    text = statusText,
                    color = if (callSession?.status == CallSession.STATUS_CONNECTED) OnlineGreen else NeonCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp, start = 20.dp, end = 20.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(CardNavy.copy(alpha = 0.85f))
                .border(1.dp, ContainerNavy, RoundedCornerShape(32.dp))
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isMicMuted = !isMicMuted
                        webRtcClient?.setMicrophoneEnabled(!isMicMuted)
                        audioSwitchManager.setMicrophoneMute(isMicMuted)
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (isMicMuted) ErrorRed else ContainerNavy)
                        .testTag("toggle_mic_btn")
                ) {
                    Icon(
                        imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mikrofon",
                        tint = Color.White
                    )
                }

                if (isVideo) {
                    IconButton(
                        onClick = {
                            isVideoDisabled = !isVideoDisabled
                            webRtcClient?.setCameraEnabled(!isVideoDisabled)
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isVideoDisabled) ErrorRed else ContainerNavy)
                    ) {
                        Icon(
                            imageVector = if (isVideoDisabled) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = "Kamera",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { webRtcClient?.switchCamera() },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(ContainerNavy)
                    ) {
                        Icon(imageVector = Icons.Default.Cameraswitch, contentDescription = "Ganti Kamera", tint = Color.White)
                    }
                }

                IconButton(
                    onClick = {
                        isSpeakerphoneOn = audioSwitchManager.toggleSpeakerphone()
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (isSpeakerphoneOn) ElectricBlue else ContainerNavy)
                        .testTag("toggle_speaker_btn")
                ) {
                    Icon(
                        imageVector = if (isSpeakerphoneOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        contentDescription = "Speaker",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { endCall() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ErrorRed)
                        .testTag("end_call_btn")
                ) {
                    Icon(imageVector = Icons.Default.CallEnd, contentDescription = "Akhiri", tint = Color.White)
                }
            }
        }
    }
}
