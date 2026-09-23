package com.company.azrylvsmark.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.company.azrylvsmark.data.model.CallSession
import com.company.azrylvsmark.ui.components.AzmassangeAvatar
import com.company.azrylvsmark.ui.theme.CardNavy
import com.company.azrylvsmark.ui.theme.ErrorRed
import com.company.azrylvsmark.ui.theme.NeonCyan
import com.company.azrylvsmark.ui.theme.OnlineGreen
import com.company.azrylvsmark.ui.theme.TextPrimaryDark
import com.company.azrylvsmark.ui.theme.TextSecondaryDark

@Composable
fun IncomingCallDialog(
    callSession: CallSession,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("incoming_call_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (callSession.isVideo) "Panggilan Video Masuk..." else "Panggilan Suara Masuk...",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                AzmassangeAvatar(photoUrl = callSession.callerPhoto, displayName = callSession.callerName, size = 80.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = callSession.callerName,
                    color = TextPrimaryDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AZMASSANGE Secure Audio/Video",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = onReject,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ErrorRed)
                            .testTag("reject_call_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CallEnd, contentDescription = "Tolak", tint = Color.White)
                    }
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(OnlineGreen)
                            .testTag("accept_call_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Terima", tint = Color.White)
                    }
                }
            }
        }
    }
}
