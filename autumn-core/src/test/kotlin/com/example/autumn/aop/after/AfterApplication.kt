package com.example.autumn.aop.after

import com.example.autumn.annotation.*
import com.example.autumn.aop.AfterInvocationHandlerAdapter
import com.example.autumn.aop.AroundProxyBeanPostProcessor
import java.lang.reflect.Method

@Configuration
@ComponentScan
class AfterApplication {
    @Bean
    fun createAroundProxyBeanPostProcessor(): AroundProxyBeanPostProcessor {
        return AroundProxyBeanPostProcessor()
    }
}

@Component
@Around("politeInvocationHandler")
class GreetingBean {
    fun hello(name: String): String {
        return "Hello, $name."
    }

    fun morning(name: String): String {
        return "Morning, $name."
    }
}

@Component
class PoliteInvocationHandler : AfterInvocationHandlerAdapter() {
    override fun after(proxy: Any, returnValue: Any, method: Method, args: Array<Any>?): Any {
        if (returnValue is String) {
            if (returnValue.endsWith(".")) {
                return returnValue.substring(0, returnValue.length - 1) + "!"
            }
        }
        return returnValue
    }
}

