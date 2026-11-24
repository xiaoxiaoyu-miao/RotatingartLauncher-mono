package com.app.ralaunch.core.platform.runtime

/**
 * Mono 运行时标识串（Runtime Spec）
 *
 * 这个分支只保留 Mono 运行时。为了兼容旧版本设置里保存的
 * `mono:<版本>` 或裸版本号，这里仍然提供统一的编解码入口。
 */
object RuntimeSpec {

    const val ENGINE_MONO = "mono"

    private const val MONO_PREFIX = "$ENGINE_MONO:"

    /** Mono 运行时使用的版本目录名（对应 mono 的 mono/4.5 profile） */
    const val MONO_DEFAULT_VERSION = "4.5"

    data class Decoded(
        val version: String
    ) {
        /** 该分支只支持 Mono；保留字段便于旧调用方平滑迁移。 */
        val isMono: Boolean get() = true
    }

    /** 编码一个 Mono 运行时标识串 */
    fun encodeMono(version: String = MONO_DEFAULT_VERSION): String =
        "$MONO_PREFIX${version.trim().ifBlank { MONO_DEFAULT_VERSION }}"

    /**
     * 解析运行时标识串。
     * 兼容旧格式（裸版本号、`mono:<版本>`），未知值回落到默认 Mono 版本。
     */
    fun decode(spec: String?): Decoded {
        val raw = spec?.trim().orEmpty()
        val version = when {
            raw.startsWith(MONO_PREFIX, ignoreCase = true) ->
                raw.substring(MONO_PREFIX.length).trim()
            else -> raw
        }.ifBlank { MONO_DEFAULT_VERSION }
        return Decoded(version)
    }

    /** 判断标识串是否指向 Mono（在此分支中恒为 true）。 */
    fun isMono(spec: String?): Boolean = true

    /** 展示用名称 */
    fun displayName(spec: String?): String = "Mono ${decode(spec).version}"
}
