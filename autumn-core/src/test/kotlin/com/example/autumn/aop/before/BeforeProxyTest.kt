package com.example.autumn.aop.before

import com.example.autumn.context.AnnotationConfigApplicationContext
import com.example.autumn.io.PropertyResolver
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class BeforeProxyTest {
    @Test
    fun testBeforeProxy() {
        AnnotationConfigApplicationContext(BeforeApplication::class.java, PropertyResolver(Properties())).use { ctx ->
            val proxy: BusinessBean = ctx.getBean(BusinessBean::class.java)
            // should print log:
            assertEquals("Hello, Bob.", proxy.hello("Bob"))
            assertEquals("Morning, Alice.", proxy.morning("Alice"))
        }
    }
}