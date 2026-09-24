package net.ririfa.fabricord.i18n

import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import kotlin.test.assertEquals

class LocalizationResourcesTest {
    @Test
    fun `all languages provide the same message keys`() {
        val languages = listOf("en", "ja", "de", "fr")
        val keysByLanguage = languages.associateWith(::loadKeys)
        val expected = keysByLanguage.getValue("en")

        keysByLanguage.forEach { (language, keys) ->
            assertEquals(expected, keys, "$language.yml does not match en.yml")
        }
    }

    private fun loadKeys(language: String): Set<String> {
        val resource = "/assets/fabricord/lang/$language.yml"
        val stream = checkNotNull(javaClass.getResourceAsStream(resource)) {
            "Missing localization resource: $resource"
        }
        val root = stream.use { Yaml().load<Map<String, Any?>>(it) }
        return flatten(root)
    }

    private fun flatten(map: Map<*, *>, prefix: String = ""): Set<String> = buildSet {
        map.forEach { (rawKey, value) ->
            val key = if (prefix.isEmpty()) rawKey.toString() else "$prefix.${rawKey}"
            if (value is Map<*, *>) {
                addAll(flatten(value, key))
            } else {
                add(key)
            }
        }
    }
}
