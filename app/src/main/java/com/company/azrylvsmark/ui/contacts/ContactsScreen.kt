package com.company.azrylvsmark.ui.contacts

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.azrylvsmark.data.model.ContactItem
import com.company.azrylvsmark.data.repository.ContactRepository
import com.company.azrylvsmark.ui.components.AzmassangeAvatar
import com.company.azrylvsmark.ui.components.EmptyStateView
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
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(
    contactRepository: ContactRepository,
    onStartChatWithContact: (ContactItem) -> Unit,
    onStartCallWithContact: (ContactItem, isVideo: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contacts by contactRepository.observeContacts().collectAsState(initial = emptyList())
    var showAddContactDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contacts_screen"),
        containerColor = DeepNavy,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddContactDialog = true },
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_contact_fab")
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Tambah Kontak")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kontak (${contacts.size})",
                    color = TextPrimaryDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (contacts.isEmpty()) {
                EmptyStateView(
                    title = "Belum ada kontak tersimpan",
                    description = "Cari pengguna berdasarkan nomor HP untuk memulai obrolan atau panggilan.",
                    actionButtonText = "+ Tambah Kontak",
                    onActionClick = { showAddContactDialog = true },
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(contacts, key = { it.contactUid }) { contact ->
                        ContactItemRow(
                            contact = contact,
                            onChatClick = { onStartChatWithContact(contact) },
                            onVoiceCallClick = { onStartCallWithContact(contact, false) },
                            onVideoCallClick = { onStartCallWithContact(contact, true) },
                            onDelete = {
                                scope.launch {
                                    contactRepository.deleteContact(contact.contactUid)
                                    Toast.makeText(context, "Kontak dihapus", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBlockToggle = {
                                scope.launch {
                                    val newBlockState = !contact.isBlocked
                                    contactRepository.blockContact(contact.contactUid, newBlockState)
                                    Toast.makeText(
                                        context,
                                        if (newBlockState) "Pengguna diblokir" else "Pengguna dibuka blokirnya",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            contactRepository = contactRepository,
            onDismiss = { showAddContactDialog = false },
            onContactAdded = {
                showAddContactDialog = false
                Toast.makeText(context, "Kontak berhasil disimpan!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ContactItemRow(
    contact: ContactItem,
    onChatClick: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onDelete: () -> Unit,
    onBlockToggle: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("contact_item_${contact.contactUid}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AzmassangeAvatar(photoUrl = contact.photoUrl, displayName = contact.displayName, size = 48.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contact.displayName,
                    color = TextPrimaryDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.isMutual) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("Mutual", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (contact.isBlocked) "Diblokir" else contact.bio.ifBlank { "Kontak AZMASSANGE" },
                color = if (contact.isBlocked) ErrorRed else TextSecondaryDark,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onChatClick, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.Default.Chat, contentDescription = "Obrolan", tint = ElectricBlue, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onVoiceCallClick, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.Default.Call, contentDescription = "Panggilan", tint = ElectricBlue, modifier = Modifier.size(20.dp))
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = NeonCyan, modifier = Modifier.size(22.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(CardNavy)) {
                    DropdownMenuItem(
                        text = { Text(if (contact.isBlocked) "Buka Blokir" else "Blokir", color = TextPrimaryDark) },
                        onClick = {
                            showMenu = false
                            onBlockToggle()
                        },
                        leadingIcon = { Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = ErrorRed) }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus Kontak", color = ErrorRed) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ErrorRed) }
                    )
                }
            }
        }
    }
}

@Composable
fun AddContactDialog(
    contactRepository: ContactRepository,
    onDismiss: () -> Unit,
    onContactAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var inputPhone by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf<ContactItem?>(null) }
    var notFoundMessage by remember { mutableStateOf<String?>(null) }

    fun search() {
        if (inputPhone.trim().length < 6) return
        isSearching = true
        searchResult = null
        notFoundMessage = null
        scope.launch {
            val result = contactRepository.searchUserByPhone(inputPhone.trim())
            isSearching = false
            result.onSuccess { contact ->
                if (contact != null) {
                    searchResult = contact
                } else {
                    notFoundMessage = "Nomor tidak terdaftar di Azmassange."
                }
            }.onFailure { err ->
                notFoundMessage = err.localizedMessage ?: "Pengguna tidak ditemukan"
            }
        }
    }

    fun save(contact: ContactItem) {
        isSaving = true
        scope.launch {
            val result = contactRepository.saveContact(contact)
            isSaving = false
            result.onSuccess {
                onContactAdded()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardNavy,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Tambah Kontak", color = TextPrimaryDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Masukkan nomor HP pengguna untuk mencari profil.",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputPhone,
                        onValueChange = { inputPhone = it },
                        placeholder = { Text("+62812...", color = TextMutedDark, fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        keyboardActions = KeyboardActions(onDone = { search() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ContainerNavy,
                            unfocusedContainerColor = ContainerNavy,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { search() },
                        enabled = !isSearching && inputPhone.trim().length >= 6,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.height(52.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Cari", tint = Color.White)
                        }
                    }
                }
                if (notFoundMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(notFoundMessage!!, color = Color(0xFFEF4444), fontSize = 13.sp)
                }
                if (searchResult != null) {
                    val user = searchResult!!
                    Spacer(modifier = Modifier.height(18.dp))
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = ContainerNavy
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AzmassangeAvatar(photoUrl = user.photoUrl, displayName = user.displayName, size = 64.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(user.displayName, color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (user.username.isNotBlank()) {
                                Text("@${user.username}", color = NeonCyan, fontSize = 13.sp)
                            }
                            if (user.bio.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(user.bio, color = TextSecondaryDark, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { save(user) },
                                enabled = !isSaving,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                modifier = Modifier.fillMaxWidth().testTag("confirm_add_contact_btn")
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Text("Simpan Kontak", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = TextSecondaryDark)
            }
        }
    )
}
