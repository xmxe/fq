package com.github.fq

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.github.fq.database.AppDatabase
import com.github.fq.database.DataItemDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MyAccessibilityService : AccessibilityService() {

    private lateinit var db: AppDatabase
    private lateinit var dao: DataItemDao
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var lastEventTime = 0L
    private val DEBOUNCE_DELAY = 3000L // 防抖
    companion object {
        private var lastShownText: String? = null
        private var lastShowTime = 0L
        private const val MIN_DISPLAY_INTERVAL = 2000L
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        db = AppDatabase.getDatabase(this)
        dao = db.dataItemDao()
        Log.d("MyAccessibilityService", "无障碍服务已连接")
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SELECTED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED

            // 可选：限制只监听某个 App（提高性能）
            // packageName = arrayOf("com.example.myapp")

            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

            notificationTimeout = 100
        }
    }

    private fun extractCurrentPageText() {
        Handler(Looper.getMainLooper()).postDelayed({
            val rootNode = rootInActiveWindow ?: return@postDelayed
            try {
                val textSet = hashSetOf<String>()
                collectTextFromNode(rootNode, textSet)
                if (textSet.isNotEmpty()) {
                    Log.d("CurrentPage", "✅ 当前页面文本: $textSet")
                    FloatWindowManager.showWindow(this, textSet.toString())
                }
            } catch (e: Exception) {
                Log.e("Accessibility", "提取文本失败", e)
            } finally {
                rootNode.recycle()
            }
        }, 100)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventType = event?.eventType ?: return
        when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SELECTED,      // Tab 选中
            AccessibilityEvent.TYPE_VIEW_CLICKED,       // 点击
            AccessibilityEvent.TYPE_VIEW_SCROLLED       // 滑动
                -> {
                // 继续处理
            }
            else -> return
        }
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEventTime < DEBOUNCE_DELAY) {
            return
        }
        lastEventTime = currentTime

        val className = event.className?.toString()
        Log.d("当前界面", "切换到: $className")
        Handler(Looper.getMainLooper()).postDelayed({
            val rootNode = rootInActiveWindow ?: return@postDelayed

            try {
                val textSet = hashSetOf<String>()
                collectTextFromNode(rootNode, textSet)

                Log.d("MyAccessibilityService", "提取到文本: $textSet")
                FloatWindowManager.showWindow(this@MyAccessibilityService, textSet.toString())
                // 遍历所有文本，尝试搜索
                // for (text in textSet) {
                //     if (text.length < 2) continue
                //
                //     scope.launch {
                //         // 可选：查询数据库
                //         // val result = withContext(Dispatchers.IO) { dao.searchResult(text) }
                //         // val displayText = result ?: text
                //         if (text == lastShownText && currentTime - lastShowTime < MIN_DISPLAY_INTERVAL) {
                //             return@launch
                //         }
                //         Log.d("MyAccessibilityService", "显示: $text")
                //         FloatWindowManager.showWindow(this@MyAccessibilityService, text)
                //
                //         delay(2000)
                //         if (FloatWindowManager.isShowing()) {
                //             FloatWindowManager.hideWindow()
                //         }
                //
                //         lastShownText = text
                //         lastShowTime = currentTime
                //
                //         // if (!result.isNullOrEmpty()) {
                //         //     Log.d("MyAccessibilityService", "匹配成功: '$text' -> '$result'")
                //         //     FloatWindowManager.showWindow(this@MyAccessibilityService, text)
                //         //     // 3秒后自动关闭
                //         //     delay(3000)
                //         //     FloatWindowManager.hideWindow()
                //         //     // 匹配成功后退出（避免重复弹窗）
                //         //     return@launch
                //         // }
                //     }
                // }
            } catch (e: Exception) {
                Log.e("MyAccessibilityService", "处理事件时出错", e)
            } finally {
                // ✅ 必须回收 rootNode，避免内存泄漏
                rootNode.recycle()
            }
        }, 100)
    }

    /**
     * 递归收集节点中的文本和描述
     */
    private fun collectTextFromNode(node: AccessibilityNodeInfo, texts: HashSet<String>) {
        // 添加文本
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { texts.add(it) }
        // 添加描述（如图片描述）
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { texts.add(it) }

        // 遍历子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                try {
                    collectTextFromNode(child, texts)
                } catch (e: Exception) {
                    Log.e("MyAccessibilityService", "遍历子节点时出错: $i", e)
                } finally {
                    child.recycle() // 必须回收
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("MyAccessibilityService", "服务被中断")
        scope.cancel() // 取消所有协程
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        FloatWindowManager.hideWindow()
        Log.d("MyAccessibilityService", "服务已销毁")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        FloatWindowManager.hideWindow()
        return super.onUnbind(intent)
    }
}