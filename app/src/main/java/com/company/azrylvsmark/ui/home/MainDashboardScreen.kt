package com.company.azrylvsmark.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.azrylvsmark.data.model.ChatConversation
import com.company.azrylvsmark.data.model.ContactItem
import com.company.azrylvsmark.data.model.StoryItem
import com.company.azrylvsmark.data.model.UserProfile
import com.company.azrylvsmark.data.repository.AuthRepository
import com.company.azrylvsmark.data.repository.CallRepository
import com.company.azrylvsmark.data.repository.ChatRepository
import com.company.azrylvsmark.data.repository.ContactRepository
import com.company.azrylvsmark.data.repository.StoryRepository
import com.company.azrylvsmark.ui.calls.CallsHistoryScreen
import com.company.azrylvsmark.ui.components.AzmassangeAvatar
import com.company.azrylvsmark.ui.components.EmptyStateView
import com.company.azrylvsmark.ui.contacts.ContactsScreen
import com.company.azrylvsmark.ui.profile.ProfileScreen
import com.company.azrylvsmark.ui.story.StoriesTabScreen
import com.company.azrylvsmark.ui.theme.BorderNavy
import com.company.azrylvsmark.ui.theme.CardNavy
import com.company.azrylvsmark.ui.theme.ContainerNavy
import com.company.azrylvsmark.ui.theme.DeepNavy
import com.company.azrylvsmark.ui.theme.ElectricBlue
import com.company.azrylvsmark.ui.theme.NeonCyan
import com.company.azrylvsmark.ui.theme.TextMutedDark
import com.company.azrylvsmark.ui.theme.TextPrimaryDark
import com.company.azrylvsmark.ui.theme.TextSecondaryDark
import com.company.azrylvsmark.utils.DateTimeUtils

@Composable
fun MainDashboardScreen(
    authRepository: AuthRepository,
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    storyRepository: StoryRepository,
    callRepository: CallRepository,
    onOpenChat: (chatId: String, otherUid: String, otherName: String, otherPhoto: String) -> Unit,
    onOpenStory: (storyId: String, ownerId: String) -> Unit,
    onCreateStory: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStartCall: (receiverId: String, receiverName: String, receiverPhoto: String, isVideo: Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentUser by authRepository.currentUserProfile.collectAsState()
    val conversations by chatRepository.observeConversations().collectAsState(initial = emptyList())
    val contacts by contactRepository.observeContacts().collectAsState(initial = emptyList())
    val stories by storyRepository.observeStories(contacts).collectAsState(initial = emptyList())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_dashboard_screen"),
        containerColor = DeepNavy,
        bottomBar = {
            NavigationBar(
                containerColor = CardNavy,
                tonalElevation = 8.dp,
                modifier = Modifier.border(1.dp, BorderNavy, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                val tabs = listOf(
                    Triple("Chats", Icons.Default.Chat, Icons.Outlined.Chat),
                    Triple("Stories", Icons.Outlined.PlayCircle, Icons.Outlined.PlayCircle),
                    Triple("Calls", Icons.Default.Call, Icons.Outlined.Call),
                    Triple("Contacts", Icons.Default.Contacts, Icons.Outlined.Contacts),
                    Triple("Profile", Icons.Default.Person, Icons.Outlined.Person)
                )
                tabs.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.second else item.third,
                                contentDescription = item.first,
                                tint = if (isSelected) ElectricBlue else TextMutedDark
                            )
                        },
                        label = {
                            Text(
                                text = item.first,
                                color = if (isSelected) ElectricBlue else TextMutedDark,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = ContainerNavy
                        ),
                        modifier = Modifier.testTag("tab_${item.first.lowercase()}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { selectedTab = 3 },
                    containerColor = ElectricBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("new_chat_fab")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "New Chat")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    ChatsDashboardContent(
                        currentUser = currentUser,
                        conversations = conversations,
                        stories = stories,
                        onOpenChat = onOpenChat,
                        onOpenStory = onOpenStory,
                        onCreateStory = onCreateStory,
                        onSearchClick = onSearchClick,
                        onProfileClick = { selectedTab = 4 }
                    )
                }
                1 -> {
                    StoriesTabScreen(
                        currentUser = currentUser,
                        stories = stories,
                        onOpenStory = onOpenStory,
                        onCreateStory = onCreateStory
                    )
                }
                2 -> {
                    CallsHistoryScreen(
                        callRepository = callRepository,
                        onStartCall = onStartCall
                    )
                }
                3 -> {
                    ContactsScreen(
                        contactRepository = contactRepository,
                        onStartChatWithContact = { contact ->
                            val chatId = chatRepository.getChatId(contact.contactUid)
                            onOpenChat(chatId, contact.contactUid, contact.displayName, contact.photoUrl)
                        },
                        onStartCallWithContact = { contact, isVideo ->
                            onStartCall(contact.contactUid, contact.displayName, contact.photoUrl, isVideo)
                        }
                    )
                }
                4 -> {
                    ProfileScreen(
                        currentUser = currentUser,
                        authRepository = authRepository,
                        onSettingsClick = onSettingsClick
                    )
                }
            }
        }
    }
}

@Composable
fun ChatsDashboardContent(
    currentUser: UserProfile?,
    conversations: List<ChatConversation>,
    stories: List<StoryItem>,
    onOpenChat: (chatId: String, otherUid: String, otherName: String, otherPhoto: String) -> Unit,
    onOpenStory: (storyId: String, ownerId: String) -> Unit,
    onCreateStory: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            DashboardHeader(
                currentUser = currentUser,
                onSearchClick = onSearchClick,
                onProfileClick = onProfileClick
            )
        }
        item {
            DashboardStoriesSection(
                currentUser = currentUser,
                stories = stories,
                onOpenStory = onOpenStory,
                onCreateStory = onCreateStory
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Text(
                text = "Chats",
                color = TextPrimaryDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        if (conversations.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No conversations yet",
                    description = "Start a private conversation\nwith someone in your contacts.",
                    actionButtonText = "+ New Chat",
                    onActionClick = onSearchClick,
                    modifier = Modifier.padding(top = 28.dp)
                )
            }
        } else {
            items(conversations, key = { it.chatId }) { conversation ->
                ConversationItemRow(
                    conversation = conversation,
                    onClick = {
                        onOpenChat(
                            conversation.chatId,
                            conversation.otherUserUid,
                            conversation.otherUserName,
                            conversation.otherUserPhoto
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardHeader(
    currentUser: UserProfile?,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        Brush.linearGradient(listOf(ElectricBlue, ContainerNavy))
                    )
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "AZMASSANGE",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardNavy)
                    .testTag("dashboard_search_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextPrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            AzmassangeAvatar(
                photoUrl = currentUser?.photoUrl,
                displayName = currentUser?.displayName ?: "A",
                size = 38.dp,
                onClick = onProfileClick,
                modifier = Modifier.testTag("dashboard_profile_btn")
            )
        }
    }
}

@Composable
fun DashboardStoriesSection(
    currentUser: UserProfile?,
    stories: List<StoryItem>,
    onOpenStory: (storyId: String, ownerId: String) -> Unit,
    onCreateStory: () -> Unit
) {
    val myStories = stories.filter { it.ownerId == currentUser?.uid }
    val contactStories = stories.filter { it.ownerId != currentUser?.uid }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        if (myStories.isNotEmpty()) {
                            onOpenStory(myStories.first().storyId, currentUser?.uid ?: "")
                        } else {
                            onCreateStory()
                        }
                    }
                    .testTag("my_story_item")
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AzmassangeAvatar(
                        photoUrl = currentUser?.photoUrl,
                        displayName = currentUser?.displayName ?: "Me",
                        size = 58.dp,
                        hasStory = myStories.isNotEmpty(),
                        isStoryViewed = false
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                            .border(2.dp, DeepNavy, CircleShape)
                            .clickable { onCreateStory() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Story",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "My Story",
                    color = TextPrimaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        items(contactStories, key = { it.storyId }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onOpenStory(story.storyId, story.ownerId) }
                    .testTag("contact_story_${story.ownerId}")
            ) {
                AzmassangeAvatar(
                    photoUrl = story.ownerPhoto,
                    displayName = story.ownerName,
                    size = 58.dp,
                    hasStory = true,
                    isStoryViewed = story.isViewedByMe
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = story.ownerName.split(" ").firstOrNull() ?: story.ownerName,
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ConversationItemRow(
    conversation: ChatConversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("conversation_row_${conversation.chatId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AzmassangeAvatar(
            photoUrl = conversation.otherUserPhoto,
            displayName = conversation.otherUserName,
            size = 52.dp,
            isOnline = conversation.otherUserOnline
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.otherUserName,
                    color = TextPrimaryDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = DateTimeUtils.formatMessageTime(conversation.lastMessageAt),
                    color = TextMutedDark,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isTyping = conversation.typingUserIds.contains(conversation.otherUserUid)
                Text(
                    text = if (isTyping) "typing..." else conversation.lastMessage,
                    color = if (isTyping) NeonCyan else TextSecondaryDark,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isTyping || conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
