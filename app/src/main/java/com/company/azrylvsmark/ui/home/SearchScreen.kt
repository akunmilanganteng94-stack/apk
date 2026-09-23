package com.company.azrylvsmark.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.azrylvsmark.data.repository.ChatRepository
import com.company.azrylvsmark.data.repository.ContactRepository
import com.company.azrylvsmark.ui.components.AzmassangeAvatar
import com.company.azrylvsmark.ui.theme.BorderNavy
import com.company.azrylvsmark.ui.theme.CardNavy
import com.company.azrylvsmark.ui.theme.DeepNavy
import com.company.azrylvsmark.ui.theme.ElectricBlue
import com.company.azrylvsmark.ui.theme.TextMutedDark
import com.company.azrylvsmark.ui.theme.TextPrimaryDark
import com.company.azrylvsmark.ui.theme.TextSecondaryDark

@Composable
fun SearchScreen(
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    onBack: () -> Unit,
    onOpenChat: (chatId: String, otherUid: String, otherName: String, otherPhoto: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val conversations by chatRepository.observeConversations().collectAsState(initial = emptyList())
    val contacts by contactRepository.observeContacts().collectAsState(initial = emptyList())

    val filteredContacts = remember(searchQuery, contacts) {
        if (searchQuery.isBlank()) emptyList()
        else contacts.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredChats = remember(searchQuery, conversations) {
        if (searchQuery.isBlank()) emptyList()
        else conversations.filter {
            it.otherUserName.contains(searchQuery, ignoreCase = true) ||
                    it.lastMessage.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(top = 16.dp)
            .testTag("search_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardNavy)
                    .testTag("search_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search chats & contacts", color = TextMutedDark, fontSize = 14.sp)
                },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMutedDark)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMutedDark)
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardNavy,
                    unfocusedContainerColor = CardNavy,
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = BorderNavy,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("search_query_input")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            if (searchQuery.isBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Search your saved contacts or chat messages",
                            color = TextMutedDark,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                if (filteredContacts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Contacts",
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(filteredContacts, key = { it.contactUid }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val chatId = chatRepository.getChatId(contact.contactUid)
                                    onOpenChat(chatId, contact.contactUid, contact.displayName, contact.photoUrl)
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AzmassangeAvatar(photoUrl = contact.photoUrl, displayName = contact.displayName, size = 44.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = contact.displayName, color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                if (contact.bio.isNotBlank()) {
                                    Text(text = contact.bio, color = TextSecondaryDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                if (filteredChats.isNotEmpty()) {
                    item {
                        Text(
                            text = "Messages",
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(filteredChats, key = { it.chatId }) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenChat(chat.chatId, chat.otherUserUid, chat.otherUserName, chat.otherUserPhoto)
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AzmassangeAvatar(photoUrl = chat.otherUserPhoto, displayName = chat.otherUserName, size = 44.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = chat.otherUserName, color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = chat.lastMessage, color = TextSecondaryDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (filteredContacts.isEmpty() && filteredChats.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching contacts or messages found",
                                color = TextMutedDark,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
