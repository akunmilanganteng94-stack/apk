package com.company.azrylvsmark.ui.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.company.azrylvsmark.data.model.ChatMessage
import com.company.azrylvsmark.data.repository.AuthRepository
import com.company.azrylvsmark.data.repository.ChatRepository
import com.company.azrylvsmark.ui.components.AzmassangeAvatar
import com.company.azrylvsmark.ui.components.MessageStatusIcon
import com.company.azrylvsmark.ui.components.VoiceNotePlayerBubble
import com.company.azrylvsmark.ui.theme.BorderNavy
import com.company.azrylvsmark.ui.theme.CardNavy
import com.company.azrylvsmark.ui.theme.ContainerNavy
import com.company.azrylvsmark.ui.theme.DeepNavy
import com.company.azrylvsmark.ui.theme.ElectricBlue
import com.company.azrylvsmark.ui.theme.ErrorRed
import com.company.azrylvsmark.ui.theme.NeonCyan
import com.company.azrylvsmark.ui.theme.ReceiverBubbleDark
import com.company.azrylvsmark.ui.theme.SenderBubbleColor
import com.company.azrylvsmark.ui.theme.TextMutedDark
import com.company.azrylvsmark.ui.theme.TextPrimaryDark
import com.company.azrylvsmark.ui.theme.TextSecondaryDark
import com.company.azrylvsmark.utils.AudioPlayerHelper
import com.company.azrylvsmark.utils.AudioRecorderHelper
import com.company.azrylvsmark.utils.DateTimeUtils
import kotlinx.coroutines.launch

@Composable
fun ChatDetailScreen(
    chatId: String,
    otherUid: String,
    otherName: String,
    otherPhoto: String,
    chatRepository: ChatRepository,
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onStartCall: (isVideo: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myUid = authRepository.currentUid ?: ""
    val messages by chatRepository.observeMessages(chatId).collectAsState(initial = emptyList())
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var viewingPhotoUrl by remember { mutableStateOf<String?>(null) }

    val audioRecorder = remember { AudioRecorderHelper(context) }
    val audioPlayer = remember { AudioPlayerHelper(context) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var currentlyPlayingUrl by remember { mutableStateOf<String?>(null) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        audioPlayer.onProgressUpdate = { progress, _, _ ->
            playbackProgress = progress
        }
        audioPlayer.onPlaybackCompleted = {
            currentlyPlayingUrl = null
            playbackProgress = 0f
        }
        onDispose {
            audioPlayer.stop()
            audioRecorder.cancelRecording()
            chatRepository.setTyping(chatId, false)
        }
    }

    LaunchedEffect(messages.size) {
        chatRepository.markMessagesAsRead(chatId)
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                chatRepository.sendImageMessage(
                    chatId = chatId,
                    receiverId = otherUid,
                    imageUri = uri,
                    replyToMessage = replyingToMessage
                )
                replyingToMessage = null
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = audioRecorder.startRecording()
            if (file != null) {
                isRecording = true
                recordingDuration = 0
            }
        } else {
            Toast.makeText(context, "Izin mikrofon diperlukan untuk pesan suara", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMessage() {
        if (inputText.trim().isEmpty()) return
        val textToSend = inputText.trim()
        inputText = ""
        val reply = replyingToMessage
        replyingToMessage = null
        scope.launch {
            chatRepository.sendTextMessage(
                chatId = chatId,
                receiverId = otherUid,
                text = textToSend,
                replyToMessage = reply
            )
            chatRepository.setTyping(chatId, false)
        }
    }

    fun stopAndSendVoiceNote() {
        if (!isRecording) return
        val (file, durationSec) = audioRecorder.stopRecording()
        isRecording = false
        if (file != null && durationSec > 0) {
            scope.launch {
                chatRepository.sendVoiceMessage(
                    chatId = chatId,
                    receiverId = otherUid,
                    audioFile = file,
                    durationSeconds = durationSec
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("chat_detail_screen"),
        containerColor = DeepNavy,
        topBar = {
            ChatHeader(
                otherName = otherName,
                otherPhoto = otherPhoto,
                onBack = onBack,
                onVoiceCall = { onStartCall(false) },
                onVideoCall = { onStartCall(true) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.messageId }) { message ->
                    val isSender = message.senderId == myUid
                    MessageBubbleRow(
                        message = message,
                        isSender = isSender,
                        onImageClick = { url -> viewingPhotoUrl = url },
                        isPlaying = currentlyPlayingUrl == message.mediaUrl && audioPlayer.isPlaying,
                        progress = if (currentlyPlayingUrl == message.mediaUrl) playbackProgress else 0f,
                        onPlayPauseVoice = {
                            if (currentlyPlayingUrl == message.mediaUrl && audioPlayer.isPlaying) {
                                audioPlayer.pause()
                            } else {
                                currentlyPlayingUrl = message.mediaUrl
                                audioPlayer.play(message.mediaUrl)
                            }
                        },
                        onSeekVoice = { ratio ->
                            if (currentlyPlayingUrl == message.mediaUrl) {
                                audioPlayer.seekTo(ratio)
                            }
                        },
                        onReply = { replyingToMessage = message },
                        onDelete = {
                            scope.launch { chatRepository.deleteMessage(chatId, message.messageId) }
                        }
                    )
                }
            }

            AnimatedVisibility(visible = replyingToMessage != null) {
                replyingToMessage?.let { replyMsg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardNavy)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(32.dp)
                                .background(NeonCyan, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (replyMsg.senderId == myUid) "Membalas pesan sendiri" else "Membalas ke $otherName",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = replyMsg.text.ifBlank { "Media" },
                                color = TextSecondaryDark,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { replyingToMessage = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Batal", tint = TextMutedDark)
                        }
                    }
                }
            }

            ChatInputBar(
                inputText = inputText,
                onTextChanged = {
                    inputText = it
                    chatRepository.setTyping(chatId, it.isNotBlank())
                },
                onSend = { sendMessage() },
                onAttachPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                isRecording = isRecording,
                onStartRecording = {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onStopRecording = { stopAndSendVoiceNote() },
                onCancelRecording = {
                    audioRecorder.cancelRecording()
                    isRecording = false
                }
            )
        }
    }

    viewingPhotoUrl?.let { url ->
        ImageViewerDialog(imageUrl = url, onDismiss = { viewingPhotoUrl = null })
    }
}

@Composable
fun ChatHeader(
    otherName: String,
    otherPhoto: String,
    onBack: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    Surface(
        color = CardNavy,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderNavy, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp).testTag("chat_back_btn")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            AzmassangeAvatar(photoUrl = otherPhoto, displayName = otherName, size = 42.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = otherName,
                    color = TextPrimaryDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "AZMASSANGE Secure",
                    color = NeonCyan,
                    fontSize = 11.5.sp
                )
            }
            IconButton(onClick = onVoiceCall, modifier = Modifier.testTag("voice_call_btn")) {
                Icon(imageVector = Icons.Default.Call, contentDescription = "Voice Call", tint = ElectricBlue)
            }
            IconButton(onClick = onVideoCall, modifier = Modifier.testTag("video_call_btn")) {
                Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = NeonCyan)
            }
        }
    }
}

@Composable
fun MessageBubbleRow(
    message: ChatMessage,
    isSender: Boolean,
    onImageClick: (String) -> Unit,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseVoice: () -> Unit,
    onSeekVoice: (Float) -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSender) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSender) 16.dp else 4.dp,
                            bottomEnd = if (isSender) 4.dp else 16.dp
                        )
                    )
                    .background(if (isSender) SenderBubbleColor else ReceiverBubbleDark)
                    .border(
                        1.dp,
                        if (isSender) ElectricBlue.copy(alpha = 0.4f) else BorderNavy,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSender) 16.dp else 4.dp,
                            bottomEnd = if (isSender) 4.dp else 16.dp
                        )
                    )
                    .clickable { showMenu = true }
                    .padding(10.dp)
            ) {
                if (message.replyToText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = message.replyToText,
                            color = if (isSender) Color.White.copy(alpha = 0.8f) else NeonCyan,
                            fontSize = 11.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (message.type == ChatMessage.TYPE_IMAGE && message.mediaUrl.isNotBlank()) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 220.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onImageClick(message.mediaUrl) }
                    )
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = message.text, color = Color.White, fontSize = 14.sp)
                    }
                } else if (message.type == ChatMessage.TYPE_VOICE && message.mediaUrl.isNotBlank()) {
                    VoiceNotePlayerBubble(
                        mediaUrl = message.mediaUrl,
                        durationSec = message.voiceDurationSeconds,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPause = onPlayPauseVoice,
                        onSeek = onSeekVoice,
                        isSender = isSender
                    )
                } else {
                    Text(
                        text = if (message.isDeleted) "Pesan ini telah dihapus" else message.text,
                        color = if (message.isDeleted) TextMutedDark else Color.White,
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateTimeUtils.formatMessageTime(message.timestamp),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp
                    )
                    if (isSender) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(status = message.status)
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(CardNavy)
            ) {
                DropdownMenuItem(
                    text = { Text("Balas", color = Color.White) },
                    onClick = {
                        showMenu = false
                        onReply()
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Reply, contentDescription = null, tint = NeonCyan)
                    }
                )
                if (message.type == ChatMessage.TYPE_TEXT && !message.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("Salin", color = Color.White) },
                        onClick = {
                            showMenu = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("message", message.text))
                            Toast.makeText(context, "Disalin ke papan klip", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                if (isSender && !message.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("Hapus Pesan", color = ErrorRed) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ErrorRed)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachPhoto: () -> Unit,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit
) {
    Surface(
        color = CardNavy,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderNavy, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        if (isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(ErrorRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Merekam pesan suara...", color = Color.White, fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCancelRecording) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Batal", tint = TextMutedDark)
                    }
                    IconButton(
                        onClick = onStopRecording,
                        modifier = Modifier
                            .size(40.dp)
                            .background(ElectricBlue, CircleShape)
                            .testTag("stop_send_voice_btn")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim Suara", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onAttachPhoto, modifier = Modifier.testTag("attach_photo_btn")) {
                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Lampirkan Foto", tint = NeonCyan)
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onTextChanged,
                    placeholder = { Text("Ketik pesan...", color = TextMutedDark, fontSize = 14.sp) },
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ContainerNavy,
                        unfocusedContainerColor = ContainerNavy,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderNavy,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text_field")
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (inputText.isNotBlank()) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(42.dp)
                            .background(ElectricBlue, CircleShape)
                            .testTag("send_message_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onStartRecording,
                        modifier = Modifier
                            .size(42.dp)
                            .background(ContainerNavy, CircleShape)
                            .testTag("record_voice_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Rekam Suara",
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
