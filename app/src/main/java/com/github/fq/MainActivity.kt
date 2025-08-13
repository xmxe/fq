package com.github.fq

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.fq.database.DatabaseInitializer

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY = 1002

    private val lifecycleCallback = object : Application.ActivityLifecycleCallbacks {
        private var activityCount = 0

        override fun onActivityStarted(activity: Activity) {
            activityCount++
        }

        override fun onActivityStopped(activity: Activity) {
            activityCount--
            // 所有 Activity 都 stopped 了，可能是退后台或退出
            if (activityCount == 0) {
                // 可选：延迟几秒判断是否真的退出
                Handler(Looper.getMainLooper()).postDelayed({
                    if (activityCount == 0) {
                        FloatWindowManager.hideWindow()
                    }
                }, 2000)
            }
        }

        // 其他回调...
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {

//        Toast.makeText(this, "页面已加载", Toast.LENGTH_LONG).show()
        super.onCreate(savedInstanceState)

        // 初始化数据库
        DatabaseInitializer.initializeDatabase(this)

        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnOpenAccessibility).setOnClickListener {
            openAccessibilitySettings()
        }

        findViewById<Button>(R.id.btnCheckOverlay).setOnClickListener {
            checkOverlayPermission()
        }
        registerActivityLifecycleCallbacks(lifecycleCallback)
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
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, REQUEST_CODE_OVERLAY)
            } else {
                Toast.makeText(this, "已获得悬浮窗权限", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "无需手动授权悬浮窗", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "已获得悬浮窗权限", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "未获得权限", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            FloatWindowManager.hideWindow()
        }
    }
}