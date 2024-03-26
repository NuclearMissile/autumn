package com.example.autumn.resolver

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue


class PropertyResolverTest {
    @Test
    fun propertyValue() {
        val pr = PropertyResolver(
            mapOf(
                "app.title" to "Autumn Framework",
                "app.version" to "v1.0",
                "jdbc.url" to "jdbc:mysql://localhost:3306/simpsons",
                "jdbc.username" to "bart",
                "jdbc.password" to "51mp50n",
                "jdbc.pool-size" to "20",
                "jdbc.auto-commit" to "true",
                "scheduler.started-at" to "2023-03-29T21:45:01",
                "scheduler.backup-at" to "03:05:10",
                "scheduler.cleanup" to "P2DT8H21M",
            ).toProperties()
        )
        assertEquals("Autumn Framework", pr.getProperty("app.title"))
        assertEquals("v1.0", pr.getProperty("app.version"))
        assertEquals("v1.0", pr.getProperty("app.version", "unknown"))
        assertNull(pr.getProperty("app.author"))
        assertEquals("Michael Liao", pr.getProperty("app.author", "Michael Liao"))

        assertEquals(true, pr.getProperty("jdbc.auto-commit", Boolean::class.java))
        assertTrue(pr.getProperty("jdbc.auto-commit", Boolean::class.java)!!)
        assertTrue(pr.getProperty("jdbc.detect-leak", true, Boolean::class.java))

        assertEquals(20, pr.getProperty("jdbc.pool-size", Int::class.java))
        assertEquals(20, pr.getProperty("jdbc.pool-size", 999, Int::class.java))
        assertEquals(5, pr.getProperty("jdbc.idle", 5, Int::class.java))

        assertEquals(
            LocalDateTime.parse("2023-03-29T21:45:01"),
            pr.getProperty("scheduler.started-at", LocalDateTime::class.java)
        )
        assertEquals(
            LocalTime.parse("03:05:10"), pr.getProperty("scheduler.backup-at", LocalTime::class.java)
        )
        assertEquals(
            LocalTime.parse("23:59:59"),
            pr.getProperty("scheduler.restart-at", LocalTime.parse("23:59:59"), LocalTime::class.java)
        )
        assertEquals(
            Duration.ofMinutes(((2 * 24 + 8) * 60 + 21).toLong()),
            pr.getProperty("scheduler.cleanup", Duration::class.java)
        )
    }

    @Test
    fun requiredProperty() {
        val props = Properties()
        props.setProperty("app.title", "Autumn Framework")
        props.setProperty("app.version", "v1.0")

        val pr = PropertyResolver(props)
        assertThrows(NullPointerException::class.java) {
            pr.getRequiredProperty("not.exist")
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    fun propertyHolder() {
        val home = System.getenv("HOME")
        println("env HOME=$home")

        val props = Properties()
        props.setProperty("app.title", "Autumn Framework")

        val pr = PropertyResolver(props)
        assertEquals("Autumn Framework", pr.getProperty("\${app.title}"))
        assertThrows(NullPointerException::class.java) {
            pr.getProperty("\${app.version}")
        }
        assertEquals("v1.0", pr.getProperty("\${app.version:v1.0}"))
        assertEquals(1, pr.getProperty("\${app.version:1}", Int::class.java))
        assertThrows(NumberFormatException::class.java) {
            pr.getProperty("\${app.version:x}", Int::class.java)
        }

        assertEquals(home, pr.getProperty("\${app.path:\${HOME}}"))
        assertEquals(home, pr.getProperty("\${app.path:\${app.home:\${HOME}}}"))
        assertEquals("/not-exist", pr.getProperty("\${app.path:\${app.home:\${ENV_NOT_EXIST:/not-exist}}}"))
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun propertyHolderOnWin() {
        val os = System.getenv("OS")
        println("env OS=$os")
        val pr = PropertyResolver(Properties())
        assertEquals("Windows_NT", pr.getProperty("\${app.os:\${OS}}"))
    }
}