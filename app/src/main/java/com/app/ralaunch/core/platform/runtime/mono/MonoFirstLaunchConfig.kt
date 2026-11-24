package com.app.ralaunch.core.platform.runtime.mono

import android.content.Context
import com.app.ralaunch.core.logging.AppLog
import org.json.JSONObject
import java.io.File

/**
 * 首次启动时的分辨率修复。
 *
 * Mono 版的 Terraria / tModLoader 在配置文件不存在时会以 800x600 窗口模式
 * 初始化 GraphicsDevice，而 Android 上的 SDL Surface 始终是全屏的，结果首帧
 * 被拉伸到整块屏幕；必须进游戏设置切一次「全屏」才会恢复正常分辨率。
 *
 * 这里只在配置文件**不存在**（也就是第一次启动）时写入全屏 + 设备分辨率，
 * 之后完全交给游戏自己维护，不会覆盖玩家在游戏内改过的窗口/分辨率设置。
 */
object MonoFirstLaunchConfig {

    private const val TAG = "MonoFirstLaunchConfig"

    fun ensureFullscreenOnFirstLaunch(
        dataDir: String,
        gameType: String?,
        assemblyPath: String,
        context: Context
    ) {
        try {
            val candidates = resolveConfigCandidates(dataDir, gameType, assemblyPath)
            if (candidates.isEmpty()) return

            val metrics = context.resources.displayMetrics
            val width = maxOf(metrics.widthPixels, metrics.heightPixels)
            val height = minOf(metrics.widthPixels, metrics.heightPixels)
            if (width <= 0 || height <= 0) return

            for (configFile in candidates) {
                if (configFile.exists()) {
                    // 不是首次启动：尊重游戏内已有设置。
                    continue
                }
                configFile.parentFile?.mkdirs()
                val json = JSONObject().apply {
                    put("Fullscreen", true)
                    put("WindowMaximized", false)
                    put("WindowBorderless", true)
                    put("DisplayWidth", width)
                    put("DisplayHeight", height)
                }
                configFile.writeText(json.toString(2))
                AppLog.i(
                    TAG,
                    "首次启动修复分辨率: ${configFile.absolutePath} -> ${width}x$height fullscreen"
                )
            }
        } catch (e: Exception) {
            // 配置修复失败不能阻止游戏启动。
            AppLog.w(TAG, "首次启动分辨率修复失败，继续启动游戏", e)
        }
    }

    private fun resolveConfigCandidates(
        dataDir: String,
        gameType: String?,
        assemblyPath: String
    ): List<File> {
        val root = File(dataDir)
        val normalizedType = gameType?.trim()?.lowercase().orEmpty()
        val exeName = File(assemblyPath).name.lowercase()

        val isTerraria = normalizedType == "terraria" || exeName == "terraria.exe"
        val isTModLoader = normalizedType == "tmodloader" ||
            normalizedType == "tmodloader13" ||
            exeName == "tmodloader.exe" ||
            exeName == "tmodloader.dll"

        return when {
            isTModLoader -> {
                // tModLoader 1.4 默认目录为 tModLoader；已有 dev 目录时同步处理 dev。
                buildList {
                    add(File(root, "Terraria/tModLoader/config.json"))
                    if (File(root, "Terraria/tModLoader-dev").isDirectory) {
                        add(File(root, "Terraria/tModLoader-dev/config.json"))
                    }
                    // tModLoader 1.3 沿用 Terraria 的配置目录。
                    add(File(root, "Terraria/config.json"))
                }
            }
            isTerraria -> listOf(File(root, "Terraria/config.json"))
            else -> emptyList()
        }
    }
}
