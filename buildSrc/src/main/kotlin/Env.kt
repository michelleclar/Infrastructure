package kotlin

import org.gradle.api.Project
import java.io.File

object EnvLoader {
    val env: Map<String, String> by lazy {
        File(".env").takeIf { it.exists() }
            ?.readLines()
            ?.filterNot { it.trim().startsWith("#") || it.isBlank() }
            ?.map {
                val (k, v) = it.split("=", limit = 2)
                k.trim() to v.trim()
            }?.toMap() ?: emptyMap()
    }

    fun Project.env(key: String): String? = env[key]
}
