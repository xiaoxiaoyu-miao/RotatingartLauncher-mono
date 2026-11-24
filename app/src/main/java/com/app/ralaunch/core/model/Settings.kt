package com.app.ralaunch.core.model

import kotlinx.serialization.Serializable

/**
 * 主题模式
 */
@Serializable
enum class ThemeMode(val value: Int) {
    FOLLOW_SYSTEM(0),
    DARK(1),
    LIGHT(2);

    companion object {
        fun fromValue(value: Int): ThemeMode = entries.find { it.value == value } ?: LIGHT
    }
}

/**
 * 背景类型
 */
@Serializable
enum class BackgroundType(val value: String) {
    DEFAULT("default"),
    COLOR("color"),
    IMAGE("image"),
    VIDEO("video");

    companion object {
        fun fromValue(value: String): BackgroundType =
            entries.find { it.value == value } ?: DEFAULT
    }
}

/**
 * 画质预设
 */
@Serializable
enum class QualityLevel(val value: Int) {
    HIGH(0),
    MEDIUM(1),
    LOW(2);

    companion object {
        fun fromValue(value: Int): QualityLevel = entries.find { it.value == value } ?: HIGH
    }
}

/**
 * 帧率限制
 */
@Serializable
enum class FpsLimit(val value: Int) {
    UNLIMITED(0),
    FPS_30(30),
    FPS_45(45),
    FPS_60(60);

    companion object {
        fun fromValue(value: Int): FpsLimit = entries.find { it.value == value } ?: UNLIMITED
    }
}

/**
 * 键盘类型
 */
@Serializable
enum class KeyboardType(val value: String, val displayName: String) {
    SYSTEM("system", "System"),
    VIRTUAL("virtual", "Virtual");

    companion object {
        fun fromValue(value: String): KeyboardType =
            entries.find { it.value == value } ?: VIRTUAL
    }
}

enum class TouchInputMode(val value: String) {
    MOD("mod"),
    SDL("sdl");

    companion object {
        fun fromValue(value: String): TouchInputMode =
            entries.find { it.value == value.trim().lowercase() } ?: MOD
    }
}

/**
 * 应用设置 (跨平台版本)
 */
@Serializable
data class AppSettings(
    // 外观设置
    var themeMode: ThemeMode = ThemeMode.LIGHT,
    var themeColor: Int = 0xFF6750A4.toInt(),
    var backgroundType: BackgroundType = BackgroundType.DEFAULT,
    var backgroundColor: Int = 0xFFFFFFFF.toInt(),
    var backgroundImagePath: String = "",
    var backgroundVideoPath: String = "",
    var backgroundOpacity: Int = 0,
    var videoPlaybackSpeed: Float = 1.0f,
    var language: String = "auto",

    // 控制设置
    var controlsOpacity: Float = 0.7f,
    var vibrationEnabled: Boolean = true,
    var virtualControllerVibrationEnabled: Boolean = false,
    var virtualControllerVibrationIntensity: Float = 1.0f,
    var virtualControllerAsFirst: Boolean = false,
    var backButtonOpenMenu: Boolean = false,
    var touchMultitouchEnabled: Boolean = true,
    var touchInputMode: String = TouchInputMode.MOD.value,
    var fpsDisplayEnabled: Boolean = false,
    var fpsDisplayX: Float = -1f,
    var fpsDisplayY: Float = -1f,
    var keyboardType: KeyboardType = KeyboardType.VIRTUAL,
    var touchEventEnabled: Boolean = true,

    // 触屏设置
    var mouseRightStickSpeed: Int = 200,
    var mouseRightStickRangeLeft: Float = 1.0f,
    var mouseRightStickRangeTop: Float = 1.0f,
    var mouseRightStickRangeRight: Float = 1.0f,
    var mouseRightStickRangeBottom: Float = 1.0f,

    // 开发者设置
    var logSystemEnabled: Boolean = true,
    var verboseLogging: Boolean = false,
    var setThreadAffinityToBigCore: Boolean = false,

    // 渲染实验
    var halfResolutionRendering: Boolean = false,
    /** Opt-in Android display refresh override; old settings retain the system policy. */
    var unlockRefreshRateLimit: Boolean = false,

    // FNA 设置
    var fnaRenderer: String = "native",
    var fnaMapBufferRangeOptimization: Boolean = true,
    var fnaGlPerfDiagnosticsEnabled: Boolean = false,

    // 画质设置
    var qualityLevel: Int = 0,
    var fnaTextureLodBias: Float = 0f,
    var fnaMaxAnisotropy: Int = 4,
    var fnaRenderScale: Float = 1.0f,
    var shaderLowPrecision: Boolean = false,
    var targetFps: Int = 0,

    // 内存优化
    var killLauncherUIAfterLaunch: Boolean = false,

    // 音频设置
    var sdlAaudioLowLatency: Boolean = false,
    var ralAudioBufferSize: Int? = null,

    // 联机设置
    var multiplayerEnabled: Boolean = false,
    var multiplayerDisclaimerAccepted: Boolean = false,

    // 公告
    var lastAnnouncementId: String = "",
    var isAnnouncementBadgeShown: Boolean = false,

    // Mono 运行时设置
    /** 让 Mono 使用 LLVM 后端（MONO_ENV_OPTIONS=--llvm） */
    var monoLlvmEnabled: Boolean = false,
    /** 输出 Mono 运行时详细日志（MONO_LOG_LEVEL/MONO_LOG_MASK） */
    var monoVerboseLoggingEnabled: Boolean = false,
    /** 启用 SGen 大 nursery + 并发并行 major GC，减少游戏内 GC 卡顿 */
    var monoGcTuningEnabled: Boolean = true,
    /** SGen nursery 大小（MB），必须为 2 的幂 */
    var monoGcNurserySizeMb: Int = 128,
    /** 启用更激进但收益更高的 JIT 优化（aggressive-inlining / alias-analysis / unsafe） */
    var monoAggressiveJitEnabled: Boolean = true,
    /** 首次进入游戏时把常见 Mono 游戏配置成设备原生分辨率 + 全屏 */
    var monoFixFirstLaunchResolution: Boolean = true,

    // Runtime 设置
    var selectedMonoRuntimeVersion: String = "",
    /**
     * 内部 SDL 后端选择："sdl3"（默认）或 "sdl2"（仅 tModLoader 1.3 等旧游戏自动使用）。
     * 不暴露在设置页里，由 GameLaunchManager 根据游戏入口自动决定。
     */
    var selectedSdlVersion: String = "sdl3",
    var selectedBox64RuntimeVersion: String = ""
) {
    companion object {
        val Default: AppSettings
            get() = AppSettings()
    }
}
