package com.app.ralaunch.core.common

import android.content.Context
import com.app.ralaunch.core.model.GameItem
import com.app.ralaunch.feature.game.ui.legacy.GameActivity
import com.app.ralaunch.feature.game.ui.sdl2.GameActivitySdl2
import com.app.ralaunch.core.logging.AppLog
import java.io.File

/**
 * 游戏启动管理器
 *
 * 使用新的存储结构: games/{GameDirName}/game_info.json
 * 所有路径都是相对于游戏目录的。
 *
 * 运行时固定为 Mono；SDL 后端默认 SDL3，tModLoader 1.3 等自带 SDL2/FNA 的
 * 旧游戏自动切到 SDL2。
 */
class GameLaunchManager(private val context: Context) {

    companion object {
        private const val TAG = "GameLaunchManager"

        /** tModLoader 1.3 的包装启动器（它的 FNA 是 SDL2 后端） */
        private const val LEGACY_TML_LAUNCHER = "TML13Launcher.exe"

        /** tModLoader 1.4 的入口（1.4.4 自带 SDL2，1.4.5 可能带 SDL3） */
        private const val TML_14_ENTRY = "tModLoader.dll"
    }

    /** 游戏包内是否自带 SDL3 原生库（tModLoader 1.4.5 用 SDL3，1.4.4 用 SDL2） */
    private fun gameShipsSdl3(gameDir: File): Boolean {
        val native = File(gameDir, "Libraries/Native")
        if (!native.isDirectory) return false
        return runCatching {
            native.walkTopDown().any { it.isFile && it.name.startsWith("libSDL3") }
        }.getOrDefault(false)
    }

    fun launchGame(game: GameItem): Boolean {
        AppLog.d(TAG, ">>> launchGame called for: ${game.displayedName}")
        AppLog.i(TAG, "launchGame called for: ${game.displayedName}, path: ${game.gameExePathRelative}")

        if (game.id.isBlank()) {
            AppLog.e(TAG, "Game storage ID is blank, cannot launch")
            return false
        }

        val gamePathFull = game.gameExePathFull
        if (gamePathFull == null) {
            AppLog.e(TAG, "Game storage path is null for game: ${game.displayedName}")
            return false
        }

        val gameFile = File(gamePathFull)

        if (!gameFile.exists() || !gameFile.isFile) {
            AppLog.e(TAG, "Assembly file not found: ${gameFile.absolutePath}")
            return false
        }

        AppLog.i(TAG, "Game runtime: mono")
        startGameActivity(game, gameFile)
        return true
    }

    private fun startGameActivity(game: GameItem, gameFile: File) {
        // tModLoader 要按入口和包内原生库决定 SDL 后端，不能写死：
        //   1.3                = TML13Launcher.exe（net40 FNA3D_sdl2）→ 固定 SDL2
        //   1.4 (net8)         = 带 libSDL2-2.0.so → FNA 走 SDL2 → GameActivitySdl2
        //   1.4.5-dev (net10)  = 带 libSDL3.so → FNA 走 SDL3 → GameActivity
        // 入口和后端不一致时会卡死在 SDL_Init(VIDEO)（拿不到 Activity/Surface）。
        val exe = game.gameExePathRelative.orEmpty()
        val gameDir = gameFile.parentFile
        val sdl3Shipped = gameDir != null && gameShipsSdl3(gameDir)
        val forcedSdl2 = exe.endsWith(LEGACY_TML_LAUNCHER, ignoreCase = true) ||
            (exe.endsWith(TML_14_ENTRY, ignoreCase = true) && !sdl3Shipped)
        val forcedSdl3 = exe.endsWith(TML_14_ENTRY, ignoreCase = true) && sdl3Shipped

        when {
            forcedSdl3 -> {
                AppLog.i(TAG, "SDL version: SDL3 (forced: $exe, 包内带 libSDL3)")
                GameActivity.launch(context = context, gameStorageId = game.id)
            }
            forcedSdl2 -> {
                AppLog.i(TAG, "SDL version: SDL2 (forced: $exe)")
                GameActivitySdl2.launch(context = context, gameStorageId = game.id)
            }
            else -> {
                AppLog.i(TAG, "SDL version: SDL3 (default)")
                GameActivity.launch(context = context, gameStorageId = game.id)
            }
        }
    }
}
