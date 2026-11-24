package com.app.ralaunch.core.platform.runtime

import com.google.gson.JsonParser
import java.io.File

/**
 * SDL 版本选择。
 *
 * 这个分支默认使用 SDL3，但保留 SDL2 以运行 **tModLoader 1.3** 等
 * 自带旧版 SDL2/FNA 的 Mono 游戏：
 *  - [SDL3]：走 Android 视频后端，需要 org.libsdl.app 的 Java 绑定层（SDLActivity）。
 *  - [SDL2]：使用独立的 org.libsdl.app2 Java 胶水层 + 随包编译的 SDL2/FAudio/FNA3D。
 *
 * 两套各自对应一个入口 Activity（[GameActivity][com.app.ralaunch.feature.game.ui.legacy.GameActivity]
 * 与 GameActivitySdl2）。默认选 SDL3；tModLoader 1.3 等入口由 GameLaunchManager 自动强制 SDL2。
 */
object SdlVersion {
    const val SDL2 = "sdl2"
    const val SDL3 = "sdl3"

    /** 归一化：未知值按 SDL3 处理，保证默认走 SDL3。 */
    fun normalize(raw: String?): String =
        if (raw?.trim()?.lowercase() == SDL2) SDL2 else SDL3

    fun isSdl2(raw: String?): Boolean = normalize(raw) == SDL2

    /** 旧版 FNA 仍链接 libSDL2.so，由 sdl2-compat 在 SDL3 上提供。 */
    fun requiresSdl2Compat(assembly: File): Boolean {
        val fna = findFna(assembly) ?: return false
        val metadata = runCatching { fna.readText(Charsets.ISO_8859_1) }.getOrNull()
            ?: return false
        return metadata.startsWith("MZ") &&
            metadata.contains("\u0000SDL2_FNAPlatform\u0000") &&
            !metadata.contains("\u0000SDL3_FNAPlatform\u0000")
    }

    private fun findFna(assembly: File): File? {
        val directory = assembly.parentFile ?: return null
        directory.resolve("FNA.dll").takeIf { it.isFile }?.let { return it }
        val libraries = directory.resolve("Libraries/FNA")
        val depsFile = directory.resolve("${assembly.nameWithoutExtension}.deps.json")
        val version = runCatching {
            JsonParser.parseString(depsFile.readText()).asJsonObject
                .getAsJsonObject("libraries").keySet()
                .singleOrNull { it.startsWith("FNA/") }?.substringAfter('/')
        }.getOrNull()
        if (version != null) {
            return libraries.resolve("$version/FNA.dll").takeIf { it.isFile }
        }
        return libraries.listFiles()?.map { it.resolve("FNA.dll") }
            ?.filter { it.isFile }?.singleOrNull()
    }

    /** 展示用名称 */
    fun displayName(raw: String?): String =
        if (isSdl2(raw)) "SDL2 · Native" else "SDL3 · Activity"
}
