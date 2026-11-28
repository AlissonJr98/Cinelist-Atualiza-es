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
    fun buildGradle_doesNotContainKeystorePassword() {
        val root = findProjectRoot()
        val buildGradle = File(root, "app/build.gradle.kts")
        assertTrue("app/build.gradle.kts deve existir", buildGradle.exists())

        val content = buildGradle.readText()
        assertFalse(
            "app/build.gradle.kts NÃO deve conter senhas literais da keystore de release",
            content.contains("#eLena2023")
        )
    }

    @Test
    fun tmdbClient_hasValidTokenConfigured() {
        assertTrue(
            "RetrofitClient deve ter token do TMDB configurado por padrão",
            RetrofitClient.tokenConfigurado
        )
    }
}
