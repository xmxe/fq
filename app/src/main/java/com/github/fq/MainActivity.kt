package com.github.fq

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.fq.database.DatabaseInitializer

class MainActivity : AppCompatActivity() {

    // private var backgroundCheckRunnable: Runnable? = null
    // private var handler = Handler(Looper.getMainLooper())

    // app进入后台隐藏悬浮窗
    // private val lifecycleCallback = object : Application.ActivityLifecycleCallbacks {
    //     private var activityCount = 0
    //
    //     override fun onActivityStarted(activity: Activity) {
    //         activityCount++
    //         // 用户回到前台，取消后台任务
    //         backgroundCheckRunnable?.let { handler.removeCallbacks(it) }
    //         backgroundCheckRunnable = null
    //     }
    //
    //     override fun onActivityStopped(activity: Activity) {
    //         activityCount--
    //         // 所有 Activity 都 stopped 了，可能是退后台或退出
    //         if (activityCount == 0) {
    //             backgroundCheckRunnable = Runnable {
    //                 if (activityCount == 0) {
    //                     FloatWindowManager.hideWindow()
    //                 }
    //             }
    //             handler.postDelayed(backgroundCheckRunnable!!, 2000)
    //         }
    //     }
    //
    //     // 其他回调...
    //     override fun onActivityPaused(activity: Activity) {}
    //     override fun onActivityResumed(activity: Activity) {}
    //     override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    //     override fun onActivityDestroyed(activity: Activity) {}
    //     override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    // }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(applicationContext, "已获得悬浮窗权限", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "未获得悬浮窗权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val usageAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (hasUsageAccess()) {
            Toast.makeText(applicationContext, "已获得“使用情况访问”权限", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(applicationContext, "未获得“使用情况访问权限“，部分功能受限", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化数据库
        DatabaseInitializer.initializeDatabase(this)
        setContentView(R.layout.activity_main)

        // 自动检查权限并提示
        // if (!hasUsageAccess()) {
        //     Toast.makeText(this, "请先开启“使用情况访问权限”", Toast.LENGTH_LONG).show()
        // }
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
        //     Toast.makeText(this, "请开启悬浮窗权限", Toast.LENGTH_LONG).show()
        // }

        // 设置点击按钮
        findViewById<Button>(R.id.btnOpenAccessibility).setOnClickListener {
            openAccessibilitySettings()
        }
        findViewById<Button>(R.id.btnCheckOverlay).setOnClickListener {
            checkOverlayPermission()
        }
        // findViewById<Button>(R.id.btnGrantUsageAccess).setOnClickListener {
        //     openUsageAccessSettings()
        // }

        // application.registerActivityLifecycleCallbacks(lifecycleCallback)
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开无障碍设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
                // startActivityForResult(intent, REQUEST_CODE_OVERLAY)
            } else {
                Toast.makeText(this, "已获得悬浮窗权限", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "无需手动授权悬浮窗", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUsageAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            usageAccessLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开“使用情况访问”设置，请手动在权限中开启", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasUsageAccess(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return true

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 60 // 1分钟内
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            beginTime,
            endTime
        )
        return usageStats != null && usageStats.isNotEmpty()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 解注册，防止内存泄漏
        // application.unregisterActivityLifecycleCallbacks(lifecycleCallback)
        if (isFinishing) {
            FloatWindowManager.hideWindow()
        }
    }
}