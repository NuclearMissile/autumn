package org.example.autumn.eventbus

import org.example.autumn.annotation.Component
import org.example.autumn.annotation.ComponentScan
import org.example.autumn.annotation.Import
import org.example.autumn.annotation.Subscribe
import org.example.autumn.context.AnnotationConfigApplicationContext
import org.example.autumn.resolver.Config
import org.junit.jupiter.api.BeforeEach
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ComponentScan
@Import(EventBusConfiguration::class)
class EventBusTestConfiguration

data class TestEventSync(val message: String)
data class TestEventAsync(val message: String)
data class TestEventUnregister(val message: String)

var testEventMessageSync = ""
var testEventMessageAsync = ""
var testEventMessageUnregister = ""

@Component
class TestEventSyncListener {
    @Subscribe
    fun onTestEventSync(event: TestEventSync) {
        testEventMessageSync = event.message
    }
}

@Component
class TestEventAsyncListener {
    @Subscribe(EventMode.ASYNC)
    fun onTestEventAsync(event: TestEventAsync) {
        Thread.sleep(10)
        testEventMessageAsync = event.message
    }
}

@Component
class TestEventUnregisterListener {
    @Subscribe
    fun onTestEventUnregister(event: TestEventUnregister) {
        testEventMessageUnregister = event.message
    }
}

class EventBusTest {
    @BeforeEach
    fun setUp() {
        testEventMessageSync = ""
        testEventMessageAsync = ""
        testEventMessageUnregister = ""
    }

    @Test
    fun testPostTestEventSync() {
        AnnotationConfigApplicationContext(EventBusTestConfiguration::class.java, Config(Properties())).use {
            val eventBus = it.getBean(EventBus::class.java)
            assertEquals("", testEventMessageSync)
            eventBus.post(TestEventSync("test_sync"))
            assertEquals("test_sync", testEventMessageSync)
        }
    }

    @Test
    fun testPostTestEventAsync() {
        AnnotationConfigApplicationContext(EventBusTestConfiguration::class.java, Config(Properties())).use {
            val eventBus = it.getBean(EventBus::class.java)
            assertEquals("", testEventMessageAsync)
            eventBus.post(TestEventAsync("test_async"))
            assertEquals("", testEventMessageAsync)
            Thread.sleep(100)
            assertEquals("test_async", testEventMessageAsync)
        }
    }

    @Test
    fun testPostTestEventUnregister() {
        AnnotationConfigApplicationContext(EventBusTestConfiguration::class.java, Config(Properties())).use {
            val eventBus = it.getBean(EventBus::class.java)
            assertTrue(eventBus.isRegistered(it.getBean(TestEventUnregisterListener::class.java)))
            assertEquals("", testEventMessageUnregister)
            eventBus.post(TestEventUnregister("test_unregister"))
            assertEquals("test_unregister", testEventMessageUnregister)
            eventBus.unregister(it.getBean(TestEventUnregisterListener::class.java))
            assertFalse(eventBus.isRegistered(it.getBean(TestEventUnregisterListener::class.java)))
            eventBus.post(TestEventUnregister(""))
            assertEquals("test_unregister", testEventMessageUnregister)
        }
    }
}