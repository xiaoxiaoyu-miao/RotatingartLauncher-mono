package com.app.ralaunch.core.di.service

import com.app.ralaunch.core.common.util.FileUtils
import com.app.ralaunch.core.di.contract.IRuntimeManagerServiceV2
import com.app.ralaunch.core.di.contract.ISettingsRepositoryServiceV2
import com.app.ralaunch.core.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

class RuntimeManagerServiceV2Test {

    @Test
    fun `getSelectedRuntime falls back to newest installed mono version`() {
        val runtimesRoot = createTempDirectory("runtime-root-")
        val legacyDotnetRoot = createTempDirectory("legacy-dotnet-")

        try {
            createMonoRuntimeLayout(runtimesRoot.resolve("mono").resolve("4.5"))

            val repository = FakeSettingsRepository(
                AppSettings(selectedMonoRuntimeVersion = "9.9.9")
            )
            val service = RuntimeManagerServiceV2(repository, runtimesRoot, legacyDotnetRoot)

            val selected = service.getSelectedRuntime(IRuntimeManagerServiceV2.RuntimeType.MONO)

            assertNotNull(selected)
            assertEquals("4.5", selected?.version)
        } finally {
            FileUtils.deleteDirectoryRecursively(runtimesRoot)
            FileUtils.deleteDirectoryRecursively(legacyDotnetRoot)
        }
    }

    @Test
    fun `getSelectedRuntime respects persisted mono version when installed`() {
        val runtimesRoot = createTempDirectory("runtime-root-")
        val legacyDotnetRoot = createTempDirectory("legacy-dotnet-")

        try {
            createMonoRuntimeLayout(runtimesRoot.resolve("mono").resolve("4.5"))

            val repository = FakeSettingsRepository(
                AppSettings(selectedMonoRuntimeVersion = "4.5")
            )
            val service = RuntimeManagerServiceV2(repository, runtimesRoot, legacyDotnetRoot)

            val selected = service.getSelectedRuntime(IRuntimeManagerServiceV2.RuntimeType.MONO)

            assertNotNull(selected)
            assertEquals("4.5", selected?.version)
        } finally {
            FileUtils.deleteDirectoryRecursively(runtimesRoot)
            FileUtils.deleteDirectoryRecursively(legacyDotnetRoot)
        }
    }

    @Test
    fun `box64 discovery accepts versioned directories with contents`() {
        val runtimesRoot = createTempDirectory("runtime-root-")
        val legacyDotnetRoot = createTempDirectory("legacy-dotnet-")

        try {
            val box64RuntimeDir = runtimesRoot.resolve("box64").resolve("2.0.0").createDirectories()
            box64RuntimeDir.resolve("box64").createFile().writeText("binary")

            val service = RuntimeManagerServiceV2(FakeSettingsRepository(), runtimesRoot, legacyDotnetRoot)

            val installed = service.getInstalledRuntimes(IRuntimeManagerServiceV2.RuntimeType.BOX64)

            assertEquals(listOf("2.0.0"), installed.map { it.version })
        } finally {
            FileUtils.deleteDirectoryRecursively(runtimesRoot)
            FileUtils.deleteDirectoryRecursively(legacyDotnetRoot)
        }
    }

    @Test
    fun `migrateLegacyInstallations is a no-op for mono-only builds`() {
        val runtimesRoot = createTempDirectory("runtime-root-")
        val legacyDotnetRoot = createTempDirectory("legacy-dotnet-")

        try {
            createMonoRuntimeLayout(runtimesRoot.resolve("mono").resolve("4.5"))
            val service = RuntimeManagerServiceV2(FakeSettingsRepository(), runtimesRoot, legacyDotnetRoot)

            service.migrateLegacyInstallations()

            assertTrue(runtimesRoot.resolve("mono").resolve("4.5").toFile().exists())
        } finally {
            FileUtils.deleteDirectoryRecursively(runtimesRoot)
            FileUtils.deleteDirectoryRecursively(legacyDotnetRoot)
        }
    }

    private fun createMonoRuntimeLayout(runtimeRoot: java.nio.file.Path) {
        val bcl = runtimeRoot.resolve("bcl").createDirectories()
        bcl.resolve("mscorlib.dll").createFile().writeText("mscorlib")
        bcl.resolve("System.dll").createFile().writeText("System")
    }

    private class FakeSettingsRepository(
        initialSettings: AppSettings = AppSettings()
    ) : ISettingsRepositoryServiceV2 {
        private val backingFlow = MutableStateFlow(initialSettings)

        override val settings: StateFlow<AppSettings> = backingFlow

        override suspend fun getSettingsSnapshot(): AppSettings = backingFlow.value.copy()

        override suspend fun updateSettings(settings: AppSettings) {
            backingFlow.value = settings.copy()
        }

        override suspend fun update(block: AppSettings.() -> Unit) {
            backingFlow.value = backingFlow.value.copy().apply(block)
        }

        override suspend fun resetToDefaults() {
            backingFlow.value = AppSettings.Default
        }
    }
}
