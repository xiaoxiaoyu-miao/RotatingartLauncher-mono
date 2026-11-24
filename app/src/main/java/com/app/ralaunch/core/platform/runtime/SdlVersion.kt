package com.app.ralaunch.core.platform.runtime

import com.google.gson.JsonParser
import java.io.File

/**
 * SDL 运行时说明。
 *
 * 这个分支只内置 SDL3：
 *  - 游戏/Activity 使用 `org.libsdl.app.SDLActivity`（Android 视频后端）。
 *  - 旧版 FNA 的 SDL2 API 由 SDL3 自带的 sdl2-compat 层承接，
 *    因此不需要再随包分发一份真正的 SDL2。
 */
object SdlVersion {
    const val SDL3 = "sdl3"

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
    fun displayName(): String = "SDL3"
}
