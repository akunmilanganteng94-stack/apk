package com.company.azrylvsmark.ui.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.azrylvsmark.data.model.UserProfile
import com.company.azrylvsmark.data.repository.AuthRepository
import com.company.azrylvsmark.ui.components.AzmassangeAvatar
import com.company.azrylvsmark.ui.components.GlassmorphicCard
import com.company.azrylvsmark.ui.theme.BorderNavy
import com.company.azrylvsmark.ui.theme.CardNavy
import com.company.azrylvsmark.ui.theme.ContainerNavy
import com.company.azrylvsmark.ui.theme.DeepNavy
import com.company.azrylvsmark.ui.theme.ElectricBlue
import com.company.azrylvsmark.ui.theme.ErrorRed
import com.company.azrylvsmark.ui.theme.NeonCyan
import com.company.azrylvsmark.ui.theme.TextMutedDark
import com.company.azrylvsmark.ui.theme.TextPrimaryDark
import com.company.azrylvsmark.ui.theme.TextSecondaryDark
import com.company.azrylvsmark.utils.CryptoUtils
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    currentUser: UserProfile?,
    authRepository: AuthRepository,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var username by remember { mutableStateOf(currentUser?.username ?: "") }
    var bio by remember { mutableStateOf(currentUser?.bio ?: "") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            isEditing = true
        }
    }

    fun saveChanges() {
        isSaving = true
        scope.launch {
            val result = authRepository.saveProfile(
                displayName = displayName,
                username = username,
                bio = bio,
                photoUri = selectedPhotoUri,
                rawPhoneNumber = currentUser?.phoneNumber ?: ""
            )
            isSaving = false
            result.onSuccess {
                isEditing = false
                Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Gagal memperbarui: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .testTag("profile_screen")
    ) {
        Text(
            text = "Profil Anda",
            color = TextPrimaryDark,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar and photo picker
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AzmassangeAvatar(
                    photoUrl = selectedPhotoUri?.toString() ?: currentUser?.photoUrl,
                    displayName = currentUser?.displayName ?: "User",
                    size = 90.dp
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue)
                        .border(2.dp, DeepNavy, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("change_profile_photo_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Ubah Foto", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isEditing) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Nama Tampilan", color = TextMutedDark) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardNavy,
                    unfocusedContainerColor = CardNavy,
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = BorderNavy,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username", color = TextMutedDark) },
                prefix = { Text("@", color = NeonCyan) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardNavy,
                    unfocusedContainerColor = CardNavy,
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = BorderNavy,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio", color = TextMutedDark) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardNavy,
                    unfocusedContainerColor = CardNavy,
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = BorderNavy,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { isEditing = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ContainerNavy),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Batal", color = TextSecondaryDark)
                }
                Button(
                    onClick = { saveChanges() },
                    enabled = !isSaving && displayName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.weight(1f).testTag("save_profile_edits_btn")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Simpan", color = Color.White)
                    }
                }
            }
        } else {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileInfoRow(
                        icon = Icons.Default.Person,
                        label = "Nama",
                        value = currentUser?.displayName ?: "-"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderNavy)
                    ProfileInfoRow(
                        icon = Icons.Default.Phone,
                        label = "Nomor Telepon",
                        value = CryptoUtils.formatDisplayPhoneNumber(currentUser?.phoneNumber ?: "")
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderNavy)
                    ProfileInfoRow(
                        icon = Icons.Default.Info,
                        label = "Bio",
                        value = currentUser?.bio ?: "Hey there! I am using AZMASSANGE."
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    displayName = currentUser?.displayName ?: ""
                    username = currentUser?.username ?: ""
                    bio = currentUser?.bio ?: ""
                    isEditing = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = ContainerNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("edit_profile_btn")
            ) {
                Text("Edit Profil", color = NeonCyan, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Actions
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                SettingsActionRow(
                    icon = Icons.Default.Settings,
                    title = "Pengaturan Aplikasi",
                    onClick = onSettingsClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderNavy)
                SettingsActionRow(
                    icon = Icons.Default.Security,
                    title = "Privasi & Enkripsi",
                    onClick = onSettingsClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BorderNavy)
                SettingsActionRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Keluar (Sign Out)",
                    titleColor = ErrorRed,
                    onClick = {
                        authRepository.signOut()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = label, color = TextMutedDark, fontSize = 11.5.sp)
            Text(text = value, color = TextPrimaryDark, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    titleColor: Color = TextPrimaryDark,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(14.dp))
    }
}
