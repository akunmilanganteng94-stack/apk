package com.company.azrylvsmark.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.company.azrylvsmark.data.model.ChatMessage
import com.company.azrylvsmark.ui.theme.BorderNavy
import com.company.azrylvsmark.ui.theme.CardNavy
import com.company.azrylvsmark.ui.theme.ContainerNavy
import com.company.azrylvsmark.ui.theme.DeepNavy
import com.company.azrylvsmark.ui.theme.ElectricBlue
import com.company.azrylvsmark.ui.theme.NeonCyan
import com.company.azrylvsmark.ui.theme.OnlineGreen
import com.company.azrylvsmark.ui.theme.ReadCheckBlue
import com.company.azrylvsmark.ui.theme.TextMutedDark
import com.company.azrylvsmark.ui.theme.TextPrimaryDark
import com.company.azrylvsmark.ui.theme.TextSecondaryDark
import com.company.azrylvsmark.utils.DateTimeUtils

@Composable
fun AzmassangeAvatar(
    photoUrl: String?,
    displayName: String,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    hasStory: Boolean = false,
    isStoryViewed: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val storyBrush = if (hasStory) {
        if (isStoryViewed) {
            Brush.sweepGradient(listOf(TextMutedDark, ContainerNavy))
        } else {
            Brush.sweepGradient(listOf(ElectricBlue, NeonCyan, ElectricBlue))
        }
    } else null

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Story ring if present
        if (storyBrush != null) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(storyBrush)
                    .padding(2.5.dp)
                    .clip(CircleShape)
                    .background(DeepNavy)
            )
        }

        val avatarInnerSize = if (hasStory) size - 6.dp else size
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(avatarInnerSize)
                    .clip(CircleShape)
            )
        } else {
            val initial = displayName.trim().firstOrNull()?.uppercase() ?: "A"
            Box(
                modifier = Modifier
                    .size(avatarInnerSize)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(ElectricBlue, ContainerNavy))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (avatarInnerSize.value * 0.42f).sp
                )
            }
        }

        // Online indicator dot
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .border(2.dp, DeepNavy, CircleShape)
                    .clip(CircleShape)
                    .background(OnlineGreen)
            )
        }
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardNavy.copy(alpha = 0.85f),
    borderColor: Color = BorderNavy.copy(alpha = 0.6f),
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .border(1.dp, borderColor, shape),
        shape = shape,
        color = backgroundColor
    ) {
        content()
    }
}

@Composable
fun MessageStatusIcon(status: String) {
    when (status) {
        ChatMessage.STATUS_SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = TextMutedDark,
                modifier = Modifier.size(14.dp)
            )
        }
        ChatMessage.STATUS_DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = TextSecondaryDark,
                modifier = Modifier.size(15.dp)
            )
        }
        ChatMessage.STATUS_READ -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                tint = ReadCheckBlue,
                modifier = Modifier.size(15.dp)
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = TextMutedDark,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun VoiceNotePlayerBubble(
    mediaUrl: String,
    durationSec: Int,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    isSender: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isSender) Color.White else ElectricBlue
    val textColor = if (isSender) Color.White.copy(alpha = 0.9f) else TextSecondaryDark

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(36.dp)
                .background(primaryColor.copy(alpha = 0.2f), CircleShape)
                .testTag("play_pause_voice_note")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Slider(
                value = progress,
                onValueChange = { onSeek(it) },
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = primaryColor,
                    inactiveTrackColor = primaryColor.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
            Text(
                text = DateTimeUtils.formatDuration(durationSec),
                color = textColor,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ContainerNavy),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(ElectricBlue, NeonCyan))
                    )
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            color = TextSecondaryDark,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("empty_state_action_btn")
            ) {
                Text(actionButtonText, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}
