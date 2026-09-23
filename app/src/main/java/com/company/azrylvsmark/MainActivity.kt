package com.company.azrylvsmark

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.company.azrylvsmark.data.model.CallSession
import com.company.azrylvsmark.data.model.StoryItem
import com.company.azrylvsmark.data.repository.AuthRepository
import com.company.azrylvsmark.data.repository.CallRepository
import com.company.azrylvsmark.data.repository.ChatRepository
import com.company.azrylvsmark.data.repository.ContactRepository
import com.company.azrylvsmark.data.repository.StoryRepository
import com.company.azrylvsmark.ui.auth.LoginPhoneScreen
import com.company.azrylvsmark.ui.auth.OtpVerificationScreen
import com.company.azrylvsmark.ui.auth.ProfileSetupScreen
import com.company.azrylvsmark.ui.calls.CallScreen
import com.company.azrylvsmark.ui.calls.IncomingCallDialog
import com.company.azrylvsmark.ui.chat.ChatDetailScreen
import com.company.azrylvsmark.ui.home.MainDashboardScreen
import com.company.azrylvsmark.ui.home.SearchScreen
import com.company.azrylvsmark.ui.profile.SettingsScreen
import com.company.azrylvsmark.ui.splash.SplashScreen
import com.company.azrylvsmark.ui.story.CreateStoryScreen
import com.company.azrylvsmark.ui.story.StoryViewerScreen
import com.company.azrylvsmark.ui.theme.AzmassangeTheme
import com.company.azrylvsmark.ui.theme.DeepNavy
import kotlinx.coroutines.launch

sealed class Screen {
    data object Splash : Screen()
    data object LoginPhone : Screen()
    data class OtpVerification(val phoneNumber: String, val verificationId: String) : Screen()
    data class ProfileSetup(val phoneNumber: String) : Screen()
    data object MainDashboard : Screen()
    data object Search : Screen()
    data class ChatDetail(
        val chatId: String,
        val otherUid: String,
        val otherName: String,
        val otherPhoto: String
    ) : Screen()
    data object CreateStory : Screen()
    data class StoryViewer(val stories: List<StoryItem>, val initialIndex: Int) : Screen()
    data class Call(
        val callId: String,
        val otherUid: String,
        val otherName: String,
        val otherPhoto: String,
        val isVideo: Boolean,
        val isIncoming: Boolean
    ) : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    private val authRepository by lazy { AuthRepository(this) }
    private val contactRepository by lazy { ContactRepository() }
    private val chatRepository by lazy { ChatRepository(this) }
    private val storyRepository by lazy { StoryRepository(this, contactRepository) }
    private val callRepository by lazy { CallRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AzmassangeTheme {
                MainAppContent(
                    authRepository = authRepository,
                    contactRepository = contactRepository,
                    chatRepository = chatRepository,
                    storyRepository = storyRepository,
                    callRepository = callRepository
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    authRepository: AuthRepository,
    contactRepository: ContactRepository,
    chatRepository: ChatRepository,
    storyRepository: StoryRepository,
    callRepository: CallRepository
) {
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val currentUser by authRepository.currentUserProfile.collectAsState()
    val incomingCall by callRepository.observeIncomingCall().collectAsState(initial = null)

    // Notification Permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Presence update on lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> authRepository.updatePresence(true)
                Lifecycle.Event.ON_PAUSE -> authRepository.updatePresence(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            authRepository.updatePresence(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is Screen.Splash -> {
                    SplashScreen(
                        onSplashFinished = {
                            if (authRepository.isLoggedIn) {
                                currentScreen = Screen.MainDashboard
                            } else {
                                currentScreen = Screen.LoginPhone
                            }
                        }
                    )
                }

                is Screen.LoginPhone -> {
                    LoginPhoneScreen(
                        authRepository = authRepository,
                        onCodeSent = { verificationId, phoneNumber ->
                            currentScreen = Screen.OtpVerification(phoneNumber, verificationId)
                        },
                        onAutoVerified = {
                            currentScreen = Screen.MainDashboard
                        }
                    )
                }

                is Screen.OtpVerification -> {
                    OtpVerificationScreen(
                        phoneNumber = screen.phoneNumber,
                        verificationId = screen.verificationId,
                        authRepository = authRepository,
                        onVerified = { profileExists ->
                            if (profileExists) {
                                currentScreen = Screen.MainDashboard
                            } else {
                                currentScreen = Screen.ProfileSetup(screen.phoneNumber)
                            }
                        },
                        onBack = {
                            currentScreen = Screen.LoginPhone
                        }
                    )
                }

                is Screen.ProfileSetup -> {
                    ProfileSetupScreen(
                        phoneNumber = screen.phoneNumber,
                        authRepository = authRepository,
                        onProfileCompleted = {
                            currentScreen = Screen.MainDashboard
                        }
                    )
                }

                is Screen.MainDashboard -> {
                    MainDashboardScreen(
                        authRepository = authRepository,
                        chatRepository = chatRepository,
                        contactRepository = contactRepository,
                        storyRepository = storyRepository,
                        callRepository = callRepository,
                        onOpenChat = { chatId, otherUid, otherName, otherPhoto ->
                            currentScreen = Screen.ChatDetail(chatId, otherUid, otherName, otherPhoto)
                        },
                        onOpenStory = { storyId, _ ->
                            scope.launch {
                                val allStories = storyRepository.getStoryFeed(emptyList())
                                val index = allStories.indexOfFirst { it.storyId == storyId }.coerceAtLeast(0)
                                currentScreen = Screen.StoryViewer(allStories, index)
                            }
                        },
                        onCreateStory = {
                            currentScreen = Screen.CreateStory
                        },
                        onSearchClick = {
                            currentScreen = Screen.Search
                        },
                        onSettingsClick = {
                            currentScreen = Screen.Settings
                        },
                        onStartCall = { otherUid, otherName, otherPhoto, isVideo ->
                            scope.launch {
                                val result = callRepository.initiateCall(otherUid, otherName, otherPhoto, isVideo)
                                result.onSuccess { callId ->
                                    currentScreen = Screen.Call(callId, otherUid, otherName, otherPhoto, isVideo, false)
                                }
                            }
                        }
                    )
                }

                is Screen.Search -> {
                    SearchScreen(
                        chatRepository = chatRepository,
                        contactRepository = contactRepository,
                        onBack = { currentScreen = Screen.MainDashboard },
                        onOpenChat = { chatId, otherUid, otherName, otherPhoto ->
                            currentScreen = Screen.ChatDetail(chatId, otherUid, otherName, otherPhoto)
                        }
                    )
                }

                is Screen.ChatDetail -> {
                    ChatDetailScreen(
                        chatId = screen.chatId,
                        otherUid = screen.otherUid,
                        otherName = screen.otherName,
                        otherPhoto = screen.otherPhoto,
                        chatRepository = chatRepository,
                        authRepository = authRepository,
                        onBack = { currentScreen = Screen.MainDashboard },
                        onStartCall = { isVideo ->
                            scope.launch {
                                val result = callRepository.initiateCall(
                                    receiverId = screen.otherUid,
                                    receiverName = screen.otherName,
                                    receiverPhoto = screen.otherPhoto,
                                    isVideo = isVideo
                                )
                                result.onSuccess { callId ->
                                    currentScreen = Screen.Call(
                                        callId = callId,
                                        otherUid = screen.otherUid,
                                        otherName = screen.otherName,
                                        otherPhoto = screen.otherPhoto,
                                        isVideo = isVideo,
                                        isIncoming = false
                                    )
                                }
                            }
                        }
                    )
                }

                is Screen.CreateStory -> {
                    CreateStoryScreen(
                        currentUser = currentUser,
                        storyRepository = storyRepository,
                        onStoryCreated = { currentScreen = Screen.MainDashboard },
                        onClose = { currentScreen = Screen.MainDashboard }
                    )
                }

                is Screen.StoryViewer -> {
                    StoryViewerScreen(
                        stories = screen.stories,
                        initialIndex = screen.initialIndex,
                        currentUser = currentUser,
                        storyRepository = storyRepository,
                        onClose = { currentScreen = Screen.MainDashboard }
                    )
                }

                is Screen.Call -> {
                    CallScreen(
                        callId = screen.callId,
                        currentUser = currentUser,
                        otherUid = screen.otherUid,
                        otherName = screen.otherName,
                        otherPhoto = screen.otherPhoto,
                        isVideo = screen.isVideo,
                        isIncoming = screen.isIncoming,
                        callRepository = callRepository,
                        onCallEnded = { currentScreen = Screen.MainDashboard }
                    )
                }

                is Screen.Settings -> {
                    SettingsScreen(
                        onBack = { currentScreen = Screen.MainDashboard }
                    )
                }
            }
        }

        // Overlay incoming call banner/dialog when ringing
        val activeIncomingCall = incomingCall
        if (activeIncomingCall != null && currentScreen !is Screen.Call) {
            IncomingCallDialog(
                callSession = activeIncomingCall,
                onAccept = {
                    scope.launch {
                        callRepository.acceptCall(activeIncomingCall.callId)
                        currentScreen = Screen.Call(
                            callId = activeIncomingCall.callId,
                            otherUid = activeIncomingCall.callerId,
                            otherName = activeIncomingCall.callerName,
                            otherPhoto = activeIncomingCall.callerPhoto,
                            isVideo = activeIncomingCall.isVideo,
                            isIncoming = true
                        )
                    }
                },
                onReject = {
                    scope.launch {
                        callRepository.rejectCall(activeIncomingCall.callId)
                    }
                }
            )
        }
    }
}
