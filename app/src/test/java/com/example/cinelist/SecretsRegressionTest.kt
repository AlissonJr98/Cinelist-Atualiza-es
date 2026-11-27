package com.example.cinelist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Teste de regressão defensivo para garantir que segredos (tokens JWT, senhas de Keystore)
 * não sejam introduzidos como valores literais no código-fonte ou scripts de build rastreados.
 */
class SecretsRegressionTest {

    private fun findProjectRoot(): File {
        var current: File = File(System.getProperty("user.dir") ?: ".")
        while (!File(current, "settings.gradle.kts").exists() && current.parentFile != null) {
            current = current.parentFile ?: break
        }
        return current
    }

    @Test
    fun buildGradle_doesNotContainHardcodedSecrets() {
        val root = findProjectRoot()
        val buildGradle = File(root, "app/build.gradle.kts")
        assertTrue("app/build.gradle.kts deve existir", buildGradle.exists())

        val content = buildGradle.readText()
        val jwtRegex = Regex("ey[A-Za-z0-9_-]{30,}")

        assertFalse(
            "app/build.gradle.kts NÃO deve conter token JWT literal de fallback",
            jwtRegex.containsMatchIn(content)
        )
        assertFalse(
            "app/build.gradle.kts NÃO deve conter senhas literais da keystore de release",
            content.contains("#eLena2023")
        )
    }

    @Test
    fun tmdbClient_doesNotContainHardcodedDefaultToken() {
        val root = findProjectRoot()
        val clientFile = File(root, "shared/src/commonMain/kotlin/com/example/cinelist/shared/network/TmdbMultiplatformClient.kt")
        assertTrue("TmdbMultiplatformClient.kt deve existir", clientFile.exists())

        val content = clientFile.readText()
        val jwtRegex = Regex("ey[A-Za-z0-9_-]{30,}")

        assertFalse(
            "TmdbMultiplatformClient.kt NÃO deve conter DEFAULT_TMDB_TOKEN com token JWT de produção",
            jwtRegex.containsMatchIn(content)
        )
    }
}
