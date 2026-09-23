package com.company.azrylvsmark.ui.calls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.azrylvsmark.data.model.CallSession
import com.company.azrylvsmark.data.repository.CallRepository
import com.company.azrylvsmark.ui.components.AzmassangeAvatar
import com.company.azrylvsmark.ui.components.EmptyStateView
import com.company.azrylvsmark.ui.theme.ElectricBlue
import com.company.azrylvsmark.ui.theme.ErrorRed
import com.company.azrylvsmark.ui.theme.NeonCyan
import com.company.azrylvsmark.ui.theme.OnlineGreen
import com.company.azrylvsmark.ui.theme.TextMutedDark
import com.company.azrylvsmark.ui.theme.TextPrimaryDark
import com.company.azrylvsmark.ui.theme.TextSecondaryDark
import com.company.azrylvsmark.utils.DateTimeUtils

@Composable
fun CallsHistoryScreen(
    callRepository: CallRepository,
    onStartCall: (receiverId: String, receiverName: String, receiverPhoto: String, isVideo: Boolean) -> Unit
) {
    val callHistory by callRepository.observeCallHistory().collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calls_history_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Panggilan",
                color = TextPrimaryDark,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        if (callHistory.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Belum ada riwayat panggilan",
                    description = "Mulai panggilan suara atau video berkualitas tinggi yang terenkripsi dengan kontak Anda.",
                    modifier = Modifier.padding(top = 40.dp)
                )
            }
        } else {
            items(callHistory, key = { it.callId }) { call ->
                val isCaller = call.callerId == callRepository.currentUid
                val otherUid = if (isCaller) call.receiverId else call.callerId
                val otherName = if (isCaller) call.receiverName else call.callerName
                val otherPhoto = if (isCaller) call.receiverPhoto else call.callerPhoto

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStartCall(otherUid, otherName, otherPhoto, call.isVideo) }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("call_history_item_${call.callId}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AzmassangeAvatar(photoUrl = otherPhoto, displayName = otherName, size = 48.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = otherName,
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (icon, color) = when {
                                call.status == CallSession.STATUS_REJECTED || call.status == CallSession.STATUS_MISSED ->
                                    Icons.AutoMirrored.Filled.CallMissed to ErrorRed
                                isCaller -> Icons.AutoMirrored.Filled.CallMade to OnlineGreen
                                else -> Icons.AutoMirrored.Filled.CallReceived to ElectricBlue
                            }
                            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${DateTimeUtils.formatStoryTime(call.createdAt)}${if (call.durationSeconds > 0) " • ${DateTimeUtils.formatDuration(call.durationSeconds)}" else ""}",
                                color = TextMutedDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = { onStartCall(otherUid, otherName, otherPhoto, call.isVideo) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = "Panggil",
                            tint = if (call.isVideo) NeonCyan else ElectricBlue
                        )
                    }
                }
            }
        }
    }
}
