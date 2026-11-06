package com.example.ui.floating

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItem
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.FloatingCalculatorScreen
import com.example.ui.screens.FloatingWebAppsScreen
import com.example.ui.screens.PhotoDetailViewer
import com.example.ui.screens.PhotosScreen
import com.example.ui.screens.QuickNotesScreen
import com.example.ui.screens.VideoPlayerView
import com.example.ui.screens.VideosScreen

enum class FloatingTab {
    WEB_APPS,
    CALCULATOR,
    FILES,
    PHOTOS,
    VIDEOS,
    NOTES
}

@Composable
fun FloatingWindowRoot(
    isMinimized: Boolean,
    isFullScreen: Boolean,
    opacity: Float = 1.0f,
    onOpacityChange: (Float) -> Unit = {},
    onDrag: (dx: Float, dy: Float) -> Unit,
    onResize: (dw: Float, dh: Float) -> Unit,
    onToggleMinimize: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onClose: () -> Unit,
    onExternalDragStart: () -> Unit = {},
    onExternalDragEnd: (Boolean) -> Unit = {}
) {
    if (isMinimized) {
        // Floating Bubble mode
        FloatingBubbleView(
            onDrag = onDrag,
            onClick = onToggleMinimize
        )
    } else {
        // Floating Window mode
        FloatingWindowContent(
            isFullScreen = isFullScreen,
            opacity = opacity,
            onOpacityChange = onOpacityChange,
            onDrag = onDrag,
            onResize = onResize,
            onToggleMinimize = onToggleMinimize,
            onToggleFullScreen = onToggleFullScreen,
            onClose = onClose,
            onExternalDragStart = onExternalDragStart,
            onExternalDragEnd = onExternalDragEnd
        )
    }
}

@Composable
fun FloatingBubbleView(
    onDrag: (dx: Float, dy: Float) -> Unit,
    onClick: () -> Unit
) {
    var totalDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(60.dp)
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag < 12f) {
                            onClick()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += kotlin.math.abs(dragAmount.x) + kotlin.math.abs(dragAmount.y)
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Layers,
            contentDescription = "Expand Floating Window",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun FloatingWindowContent(
    isFullScreen: Boolean,
    opacity: Float = 1.0f,
    onOpacityChange: (Float) -> Unit = {},
    onDrag: (dx: Float, dy: Float) -> Unit,
    onResize: (dw: Float, dh: Float) -> Unit,
    onToggleMinimize: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onClose: () -> Unit,
    onExternalDragStart: () -> Unit,
    onExternalDragEnd: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableStateOf(FloatingTab.WEB_APPS) }
    var directPhotoViewerItem by remember { mutableStateOf<MediaItem?>(null) }
    var directVideoPlayerItem by remember { mutableStateOf<MediaItem?>(null) }

    val cornerRadius = if (isFullScreen) 0.dp else 14.dp

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .shadow(if (isFullScreen) 0.dp else 12.dp, RoundedCornerShape(cornerRadius))
            .border(
                width = if (isFullScreen) 0.dp else 1.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title Bar (Draggable with Opacity, Minimize, Fullscreen, Close)
                TitleBar(
                    selectedTab = selectedTab,
                    isFullScreen = isFullScreen,
                    opacity = opacity,
                    onOpacityChange = onOpacityChange,
                    onDrag = onDrag,
                    onMinimize = onToggleMinimize,
                    onFullScreen = onToggleFullScreen,
                    onClose = onClose
                )

                // Main Content Body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        directPhotoViewerItem != null -> {
                            PhotoDetailViewer(
                                photos = listOf(directPhotoViewerItem!!),
                                initialIndex = 0,
                                onClose = { directPhotoViewerItem = null },
                                onExternalDragStart = onExternalDragStart,
                                onExternalDragEnd = onExternalDragEnd
                            )
                        }
                        directVideoPlayerItem != null -> {
                            VideoPlayerView(
                                video = directVideoPlayerItem!!,
                                onClose = { directVideoPlayerItem = null },
                                onExternalDragStart = onExternalDragStart,
                                onExternalDragEnd = onExternalDragEnd
                            )
                        }
                        else -> {
                            when (selectedTab) {
                                FloatingTab.WEB_APPS -> FloatingWebAppsScreen()
                                FloatingTab.CALCULATOR -> FloatingCalculatorScreen()
                                FloatingTab.FILES -> FileManagerScreen(
                                    onOpenPhoto = { directPhotoViewerItem = it },
                                    onOpenVideo = { directVideoPlayerItem = it },
                                    onExternalDragStart = onExternalDragStart,
                                    onExternalDragEnd = onExternalDragEnd
                                )
                                FloatingTab.PHOTOS -> PhotosScreen(
                                    onExternalDragStart = onExternalDragStart,
                                    onExternalDragEnd = onExternalDragEnd
                                )
                                FloatingTab.VIDEOS -> VideosScreen(
                                    onExternalDragStart = onExternalDragStart,
                                    onExternalDragEnd = onExternalDragEnd
                                )
                                FloatingTab.NOTES -> QuickNotesScreen()
                            }
                        }
                    }
                }

                // Bottom Tab Navigation (ScrollableTabRow for 6 tools)
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 4.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(54.dp)
                ) {
                    Tab(
                        selected = selectedTab == FloatingTab.WEB_APPS,
                        onClick = {
                            directPhotoViewerItem = null
                            directVideoPlayerItem = null
                            selectedTab = FloatingTab.WEB_APPS
                        },
                        icon = { Icon(Icons.Default.Language, contentDescription = "ওয়েব", modifier = Modifier.size(18.dp)) },
                        text = { Text("ওয়েব", fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedTab == FloatingTab.CALCULATOR,
                        onClick = {
                            directPhotoViewerItem = null
                            directVideoPlayerItem = null
                            selectedTab = FloatingTab.CALCULATOR
                        },
                        icon = { Icon(Icons.Default.Calculate, contentDescription = "হিসাব", modifier = Modifier.size(18.dp)) },
                        text = { Text("হিসাব", fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedTab == FloatingTab.FILES,
                        onClick = {
                            directPhotoViewerItem = null
                            directVideoPlayerItem = null
                            selectedTab = FloatingTab.FILES
                        },
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = "ফাইল", modifier = Modifier.size(18.dp)) },
                        text = { Text("ফাইল", fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedTab == FloatingTab.PHOTOS,
                        onClick = {
                            directPhotoViewerItem = null
                            directVideoPlayerItem = null
                            selectedTab = FloatingTab.PHOTOS
                        },
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "ছবি", modifier = Modifier.size(18.dp)) },
                        text = { Text("ছবি", fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedTab == FloatingTab.VIDEOS,
                        onClick = {
                            directPhotoViewerItem = null
                            directVideoPlayerItem = null
                            selectedTab = FloatingTab.VIDEOS
                        },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "ভিডিও", modifier = Modifier.size(18.dp)) },
                        text = { Text("ভিডিও", fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedTab == FloatingTab.NOTES,
                        onClick = {
                            directPhotoViewerItem = null
                            directVideoPlayerItem = null
                            selectedTab = FloatingTab.NOTES
                        },
                        icon = { Icon(Icons.Default.EditNote, contentDescription = "নোট", modifier = Modifier.size(18.dp)) },
                        text = { Text("নোট", fontSize = 10.sp) }
                    )
                }
            }

            // Bottom-Right Corner Resize Handle (Hidden in Fullscreen)
            if (!isFullScreen) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onResize(dragAmount.x, dragAmount.y)
                            }
                        },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Resize Window",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TitleBar(
    selectedTab: FloatingTab,
    isFullScreen: Boolean,
    opacity: Float = 1.0f,
    onOpacityChange: (Float) -> Unit = {},
    onDrag: (dx: Float, dy: Float) -> Unit,
    onMinimize: () -> Unit,
    onFullScreen: () -> Unit,
    onClose: () -> Unit
) {
    var showOpacityMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Drag Window",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            val tabTitle = when (selectedTab) {
                FloatingTab.WEB_APPS -> "সোশ্যাল ও ওয়েব (YT, FB, TG, WA)"
                FloatingTab.CALCULATOR -> "ভাসমান ক্যালকুলেটর"
                FloatingTab.FILES -> "ফাইল ম্যানেজার"
                FloatingTab.PHOTOS -> "ছবি ভিউয়ার"
                FloatingTab.VIDEOS -> "ভিডিও প্লেয়ার"
                FloatingTab.NOTES -> "দ্রুত নোট ও ক্লিপবোর্ড"
            }

            Text(
                text = tabTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Opacity Adjustment Button
            Box {
                IconButton(
                    onClick = { showOpacityMenu = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Opacity,
                        contentDescription = "Transparency",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOpacityMenu,
                    onDismissRequest = { showOpacityMenu = false },
                    modifier = Modifier.width(220.dp).padding(6.dp)
                ) {
                    Text(
                        text = "উইন্ডোর স্বচ্ছতা: ${(opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Slider(
                        value = opacity,
                        onValueChange = onOpacityChange,
                        valueRange = 0.25f..1.0f,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AssistChip(
                            onClick = { onOpacityChange(0.4f); showOpacityMenu = false },
                            label = { Text("৪০%", fontSize = 10.sp) }
                        )
                        AssistChip(
                            onClick = { onOpacityChange(0.7f); showOpacityMenu = false },
                            label = { Text("৭০%", fontSize = 10.sp) }
                        )
                        AssistChip(
                            onClick = { onOpacityChange(1.0f); showOpacityMenu = false },
                            label = { Text("১০০%", fontSize = 10.sp) }
                        )
                    }
                }
            }

            // Minimize Button
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Minimize",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Fullscreen Toggle Button
            IconButton(
                onClick = onFullScreen,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullScreen) "Restore" else "Maximize",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Close Button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
