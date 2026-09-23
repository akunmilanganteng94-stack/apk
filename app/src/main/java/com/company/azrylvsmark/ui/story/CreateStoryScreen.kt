package com.company.azrylvsmark.ui.story

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.company.azrylvsmark.data.model.StoryItem
import com.company.azrylvsmark.data.model.UserProfile
import com.company.azrylvsmark.data.repository.StoryRepository
import com.company.azrylvsmark.ui.theme.CardNavy
import com.company.azrylvsmark.ui.theme.DeepNavy
import com.company.azrylvsmark.ui.theme.ElectricBlue
import com.company.azrylvsmark.ui.theme.NeonCyan
import com.company.azrylvsmark.ui.theme.TextMutedDark
import com.company.azrylvsmark.ui.theme.TextPrimaryDark
import kotlinx.coroutines.launch

@Composable
fun CreateStoryScreen(
    currentUser: UserProfile?,
    storyRepository: StoryRepository,
    onStoryCreated: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPhotoStory by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var selectedBgColor by remember { mutableStateOf("#182033") }
    var isPublishing by remember { mutableStateOf(false) }

    val bgColors = listOf("#080B12", "#182033", "#1E40AF", "#065F46", "#831843", "#701A75")

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isPhotoStory = true
        }
    }

    fun publish() {
        if (currentUser == null) return
        if (!isPhotoStory && caption.trim().isEmpty()) {
            Toast.makeText(context, "Ketik teks untuk cerita Anda", Toast.LENGTH_SHORT).show()
            return
        }

        isPublishing = true
        scope.launch {
            val result = storyRepository.createStory(
                type = if (isPhotoStory) StoryItem.TYPE_IMAGE else StoryItem.TYPE_TEXT,
                mediaUri = if (isPhotoStory) selectedImageUri else null,
                caption = caption,
                backgroundColor = selectedBgColor,
                currentUser = currentUser
            )
            isPublishing = false
            result.onSuccess {
                Toast.makeText(context, "Cerita diterbitkan! (Aktif selama 24 jam)", Toast.LENGTH_SHORT).show()
                onStoryCreated()
            }.onFailure {
                Toast.makeText(context, "Gagal menerbitkan cerita: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isPhotoStory) Color.Black
                else Color(android.graphics.Color.parseColor(selectedBgColor))
            )
            .testTag("create_story_screen")
    ) {
        if (isPhotoStory && selectedImageUri != null) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = "Selected Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = {
                        Text(
                            "Ketik cerita Anda...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_text_input")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isPhotoStory) ElectricBlue else Color.Black.copy(alpha = 0.4f))
                        .testTag("pick_story_photo_btn")
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = "Foto", tint = Color.White)
                }

                IconButton(
                    onClick = {
                        isPhotoStory = false
                        selectedImageUri = null
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (!isPhotoStory) ElectricBlue else Color.Black.copy(alpha = 0.4f))
                        .testTag("pick_story_text_btn")
                ) {
                    Icon(imageVector = Icons.Default.TextFields, contentDescription = "Teks", tint = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
        ) {
            if (!isPhotoStory) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    bgColors.forEach { hex ->
                        val isSelected = selectedBgColor == hex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) NeonCyan else Color.White.copy(alpha = 0.3f),
                                    CircleShape
                                )
                                .clickable { selectedBgColor = hex }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Tambahkan keterangan...", color = TextMutedDark, fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardNavy.copy(alpha = 0.9f),
                        unfocusedContainerColor = CardNavy.copy(alpha = 0.9f),
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { publish() },
                    enabled = !isPublishing,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue)
                        .testTag("publish_story_btn")
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Terbitkan", tint = Color.White)
                    }
                }
            }
        }
    }
}
