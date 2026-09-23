package com.company.azrylvsmark.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import com.company.azrylvsmark.data.model.StoryItem
import com.company.azrylvsmark.data.model.UserProfile
import com.company.azrylvsmark.ui.components.AzmassangeAvatar
import com.company.azrylvsmark.ui.components.EmptyStateView
import com.company.azrylvsmark.ui.theme.DeepNavy
import com.company.azrylvsmark.ui.theme.ElectricBlue
import com.company.azrylvsmark.ui.theme.TextMutedDark
import com.company.azrylvsmark.ui.theme.TextPrimaryDark
import com.company.azrylvsmark.ui.theme.TextSecondaryDark
import com.company.azrylvsmark.utils.DateTimeUtils

@Composable
fun StoriesTabScreen(
    currentUser: UserProfile?,
    stories: List<StoryItem>,
    onOpenStory: (storyId: String, ownerId: String) -> Unit,
    onCreateStory: () -> Unit
) {
    val myStories = stories.filter { it.ownerId == currentUser?.uid }
    val contactStories = stories.filter { it.ownerId != currentUser?.uid }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stories_tab_screen"),
        containerColor = DeepNavy,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateStory,
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("new_story_fab")
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Tambah Cerita")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            item {
                Text(
                    text = "Cerita",
                    color = TextPrimaryDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (myStories.isNotEmpty()) {
                                onOpenStory(myStories.first().storyId, currentUser?.uid ?: "")
                            } else {
                                onCreateStory()
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .testTag("my_story_tab_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AzmassangeAvatar(
                            photoUrl = currentUser?.photoUrl,
                            displayName = currentUser?.displayName ?: "Me",
                            size = 54.dp,
                            hasStory = myStories.isNotEmpty(),
                            isStoryViewed = false
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Cerita Saya",
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (myStories.isNotEmpty()) "${myStories.size} pembaruan aktif • Ketuk untuk melihat"
                            else "Ketuk untuk menambahkan cerita",
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Text(
                    text = "Pembaruan Terkini",
                    color = TextSecondaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (contactStories.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Belum ada cerita",
                        description = "Cerita dari kontak bersama (mutual) akan tampil di sini.\nKedua pihak harus saling menyimpan kontak.",
                        modifier = Modifier.padding(top = 40.dp)
                    )
                }
            } else {
                items(contactStories, key = { it.storyId }) { story ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenStory(story.storyId, story.ownerId) }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .testTag("contact_story_row_${story.storyId}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AzmassangeAvatar(
                            photoUrl = story.ownerPhoto,
                            displayName = story.ownerName,
                            size = 52.dp,
                            hasStory = true,
                            isStoryViewed = story.isViewedByMe
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = story.ownerName,
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = DateTimeUtils.formatStoryTime(story.createdAt),
                                color = TextMutedDark,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
