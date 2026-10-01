package net.ririfa.fabricord.i18n

import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import kotlin.test.assertEquals

class LocalizationResourcesTest {
    @Test
    fun `all languages provide the same message keys`() {
        val languages = listOf("en", "ja", "de", "fr")
        val keysByLanguage = languages.associateWith { loadValues(messageResource(it)).keys }
        val expected = keysByLanguage.getValue("en")

        keysByLanguage.forEach { (language, keys) ->
            assertEquals(expected, keys, "$language.yml does not match en.yml")
        }
    }

    @Test
    fun `all translations preserve message placeholders`() {
        val languages = listOf("en", "ja", "de", "fr")
        val valuesByLanguage = languages.associateWith { loadValues(messageResource(it)) }
        val expected = valuesByLanguage.getValue("en").mapValues { (_, value) -> placeholders(value) }

        valuesByLanguage.forEach { (language, values) ->
            val actual = values.mapValues { (_, value) -> placeholders(value) }
            assertEquals(expected, actual, "$language.yml changes a message placeholder")
        }
    }

    @Test
    fun `localized configuration templates have the same structure`() {
        val english = loadValues("/assets/fabricord/config/languages/en.yml").keys
        val japanese = loadValues("/assets/fabricord/config/languages/ja.yml").keys

        assertEquals(english, japanese, "ja.yml does not match the English configuration template")
    }

    private fun messageResource(language: String): String = "/assets/fabricord/lang/$language.yml"

    private fun loadValues(resource: String): Map<String, String> {
        val stream = checkNotNull(javaClass.getResourceAsStream(resource)) {
            "Missing localization resource: $resource"
        }
        val root = stream.use { Yaml().load<Map<String, Any?>>(it) }
        return flatten(root)
    }

    private fun flatten(map: Map<*, *>, prefix: String = ""): Map<String, String> = buildMap {
        map.forEach { (rawKey, value) ->
            val key = if (prefix.isEmpty()) rawKey.toString() else "$prefix.${rawKey}"
            if (value is Map<*, *>) {
                putAll(flatten(value, key))
            } else {
                put(key, value?.toString().orEmpty())
            }
        }
    }

    private fun placeholders(value: String): Set<String> = PLACEHOLDER.findAll(value)
        .map { match -> match.groups[1]?.value ?: match.groups[2]!!.value }
        .toSet()

    private companion object {
        val PLACEHOLDER = Regex("\\{([A-Za-z0-9_.-]+)}|%([A-Za-z0-9_.-]+)%")
    }
}
