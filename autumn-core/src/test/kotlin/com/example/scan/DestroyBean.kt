package com.example.scan

import com.example.autumn.annotation.*
import jakarta.annotation.PreDestroy


@Component
class AnnotationDestroyBean {
    @Value("\${app.title}")
    var appTitle: String? = null

    @PreDestroy
    fun destroy() {
        this.appTitle = null
    }
}

class SpecifyDestroyBean internal constructor(var appTitle: String?) {
    fun destroy() {
        this.appTitle = null
    }
}

@Configuration
class SpecifyDestroyConfiguration {
    @Bean(destroyMethod = "destroy")
    fun createSpecifyDestroyBean(@Value("\${app.title}") appTitle: String?): SpecifyDestroyBean {
        return SpecifyDestroyBean(appTitle)
    }
}