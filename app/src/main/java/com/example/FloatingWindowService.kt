package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ui.floating.FloatingWindowRoot
import com.example.ui.theme.MyApplicationTheme

class FloatingWindowService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_START = "ACTION_START_FLOATING_WINDOW"
        const val ACTION_STOP = "ACTION_STOP_FLOATING_WINDOW"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_window_channel"

        val isServiceRunningFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
        val isServiceRunning: Boolean get() = isServiceRunningFlow.value
    }

    private var windowManager: WindowManager? = null
    private var floatingComposeView: ComposeView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    // Window dimensions and states
    private var windowWidth = 850
    private var windowHeight = 1100
    private var windowX = 80
    private var windowY = 150

    private var isMinimized by mutableStateOf(false)
    private var isFullScreen by mutableStateOf(false)
    private var windowOpacity by mutableStateOf(1.0f)

    // Saved dimensions before fullscreen
    private var savedWidth = 850
    private var savedHeight = 1100
    private var savedX = 80
    private var savedY = 150

    // Lifecycle, ViewModelStore, and SavedStateRegistry for ComposeView inside Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        isServiceRunningFlow.value = true

        initWindowManager()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun getScreenDimensions(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager?.currentWindowMetrics?.bounds
            Pair(bounds?.width() ?: 1080, bounds?.height() ?: 1920)
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    }

    private fun initWindowManager() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val (screenWidth, screenHeight) = getScreenDimensions()

        // Set default window size dynamically based on screen
        windowWidth = (screenWidth * 0.85f).toInt().coerceIn(600, 1200)
        windowHeight = (screenHeight * 0.70f).toInt().coerceIn(700, 1600)
        savedWidth = windowWidth
        savedHeight = windowHeight

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowLayoutParams = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = windowX
            y = windowY
            @Suppress("DEPRECATION")
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val backDispatcher = androidx.activity.OnBackPressedDispatcher()
        val backPressedDispatcherOwner = object : androidx.activity.OnBackPressedDispatcherOwner {
            override val lifecycle: Lifecycle get() = this@FloatingWindowService.lifecycle
            override val onBackPressedDispatcher: androidx.activity.OnBackPressedDispatcher get() = backDispatcher
        }

        val composeView = ComposeView(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setViewTreeLifecycleOwner(this@FloatingWindowService)
            setViewTreeViewModelStoreOwner(this@FloatingWindowService)
            setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)

            setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.activity.compose.LocalOnBackPressedDispatcherOwner provides backPressedDispatcherOwner
                ) {
                    MyApplicationTheme {
                        FloatingWindowRoot(
                            isMinimized = isMinimized,
                            isFullScreen = isFullScreen,
                            opacity = windowOpacity,
                            onOpacityChange = { handleOpacityChange(it) },
                            onDrag = { dx, dy -> handleDrag(dx, dy) },
                            onResize = { dw, dh -> handleResize(dw, dh) },
                            onToggleMinimize = { handleToggleMinimize() },
                            onToggleFullScreen = { handleToggleFullScreen() },
                            onClose = { stopSelf() },
                            onExternalDragStart = { handleExternalDragStart() },
                            onExternalDragEnd = { success -> handleExternalDragEnd(success) }
                        )
                    }
                }
            }
        }

        floatingComposeView = composeView
        try {
            windowManager?.addView(floatingComposeView, windowLayoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var restoreOpacityRunnable: Runnable? = null

    private fun handleExternalDragStart() {
        mainHandler.post {
            val params = windowLayoutParams ?: return@post
            params.alpha = 0.35f
            try {
                windowManager?.updateViewLayout(floatingComposeView, params)
            } catch (_: Exception) {}

            // Automatic safety timeout: restore opacity after 12s in case drop target swallowed event
            restoreOpacityRunnable?.let { mainHandler.removeCallbacks(it) }
            restoreOpacityRunnable = Runnable {
                handleExternalDragEnd(false)
            }
            mainHandler.postDelayed(restoreOpacityRunnable!!, 12000)
        }
    }

    private fun handleExternalDragEnd(success: Boolean) {
        mainHandler.post {
            restoreOpacityRunnable?.let { mainHandler.removeCallbacks(it) }
            val params = windowLayoutParams ?: return@post
            params.alpha = windowOpacity
            try {
                windowManager?.updateViewLayout(floatingComposeView, params)
            } catch (_: Exception) {}
        }
    }

    private fun handleOpacityChange(alpha: Float) {
        windowOpacity = alpha.coerceIn(0.25f, 1.0f)
        val params = windowLayoutParams ?: return
        params.alpha = windowOpacity
        try {
            windowManager?.updateViewLayout(floatingComposeView, params)
        } catch (_: Exception) {}
    }

    private fun handleDrag(dx: Float, dy: Float) {
        if (isFullScreen) return
        val params = windowLayoutParams ?: return
        val (screenWidth, screenHeight) = getScreenDimensions()
        val maxX = (screenWidth - 120).coerceAtLeast(0)
        val maxY = (screenHeight - 120).coerceAtLeast(0)
        params.x = (params.x + dx.toInt()).coerceIn(0, maxX)
        params.y = (params.y + dy.toInt()).coerceIn(0, maxY)
        windowX = params.x
        windowY = params.y
        try {
            windowManager?.updateViewLayout(floatingComposeView, params)
        } catch (_: Exception) {}
    }

    private fun handleResize(dw: Float, dh: Float) {
        if (isFullScreen || isMinimized) return
        val params = windowLayoutParams ?: return

        val (maxW, maxH) = getScreenDimensions()

        params.width = (params.width + dw.toInt()).coerceIn(400, maxW)
        params.height = (params.height + dh.toInt()).coerceIn(500, maxH)
        windowWidth = params.width
        windowHeight = params.height
        savedWidth = windowWidth
        savedHeight = windowHeight

        try {
            windowManager?.updateViewLayout(floatingComposeView, params)
        } catch (_: Exception) {}
    }

    private fun handleToggleMinimize() {
        val params = windowLayoutParams ?: return
        isMinimized = !isMinimized

        if (isMinimized) {
            // Bubble dimensions: 60dp (~180px)
            val bubbleSize = (60 * resources.displayMetrics.density).toInt()
            params.width = bubbleSize
            params.height = bubbleSize
        } else {
            // Restore window dimensions
            params.width = if (isFullScreen) WindowManager.LayoutParams.MATCH_PARENT else windowWidth
            params.height = if (isFullScreen) WindowManager.LayoutParams.MATCH_PARENT else windowHeight
            params.x = if (isFullScreen) 0 else windowX
            params.y = if (isFullScreen) 0 else windowY
        }

        try {
            windowManager?.updateViewLayout(floatingComposeView, params)
        } catch (_: Exception) {}
    }

    private fun handleToggleFullScreen() {
        if (isMinimized) return
        val params = windowLayoutParams ?: return
        isFullScreen = !isFullScreen

        if (isFullScreen) {
            savedWidth = params.width
            savedHeight = params.height
            savedX = params.x
            savedY = params.y

            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.x = 0
            params.y = 0
        } else {
            params.width = savedWidth
            params.height = savedHeight
            params.x = savedX
            params.y = savedY
            windowWidth = savedWidth
            windowHeight = savedHeight
            windowX = savedX
            windowY = savedY
        }

        try {
            windowManager?.updateViewLayout(floatingComposeView, params)
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Window Viewer")
            .setContentText("উইন্ডো চলছে • বন্ধ করতে ট্যাপ করুন")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close Window", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunningFlow.value = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()

        try {
            floatingComposeView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        floatingComposeView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
