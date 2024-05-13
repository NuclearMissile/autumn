package org.example.autumn.eventbus

import org.example.autumn.annotation.Bean
import org.example.autumn.annotation.Configuration
import org.example.autumn.context.ApplicationContextHolder
import org.example.autumn.context.BeanPostProcessor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@Configuration
class EventBusConfig {
    @Bean(destroyMethod = "close")
    fun eventBus(): EventBus {
        return EventBus.instance
    }

    @Bean
    fun createEventSubscribeBeanPostProcessor(): EventSubscribeBeanPostProcessor {
        return EventSubscribeBeanPostProcessor()
    }
}

class EventSubscribeBeanPostProcessor : BeanPostProcessor {
    override fun afterInitialization(bean: Any, beanName: String): Any {
        val eventBus = ApplicationContextHolder.requiredApplicationContext.getBean(EventBus::class.java)
        for (method in bean.javaClass.methods) {
            if (method.annotations.map { it.annotationClass }.contains(Subscribe::class)) {
                eventBus.register(bean)
                return bean
            }
        }
        return bean
    }
}

@Target(AnnotationTarget.FUNCTION)
annotation class Subscribe(val eventMode: EventMode = EventMode.ASYNC)

enum class EventMode { ASYNC, SYNC }

class EventBus private constructor() : AutoCloseable {
    companion object {
        val instance by lazy { EventBus() }
    }

    private val subMap = ConcurrentHashMap<Any, ArrayList<Method>>()
    private val executor = Executors.newCachedThreadPool()

    fun register(subscriber: Any) {
        if (!subMap.containsKey(subscriber)) {
            subMap[subscriber] = ArrayList()
        }
        val methods = subMap[subscriber]!!
        for (method in subscriber.javaClass.methods) {
            if (method.annotations.map { it.annotationClass }.contains(Subscribe::class)) {
                methods.add(method)
            }
        }
    }

    fun unregister(subscriber: Any) {
        subMap.remove(subscriber)
    }

    fun post(event: Any) {
        for ((subscriber, methods) in subMap) {
            for (method in methods) {
                if (method.genericParameterTypes.singleOrNull() == event.javaClass) {
                    val anno = method.getAnnotation(Subscribe::class.java)
                    if (anno.eventMode == EventMode.ASYNC) {
                        executor.submit { method.invoke(subscriber, event) }
                    } else {
                        method.invoke(subscriber, event)
                    }
                }
            }
        }
    }

    override fun close() {
        executor.shutdown()
    }
}