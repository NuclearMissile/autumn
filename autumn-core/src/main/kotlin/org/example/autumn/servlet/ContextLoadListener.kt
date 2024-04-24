package org.example.autumn.servlet

import jakarta.servlet.ServletContextEvent
import jakarta.servlet.ServletContextListener
import org.example.autumn.context.AnnotationConfigApplicationContext
import org.example.autumn.context.ApplicationContext
import org.example.autumn.exception.AutumnException
import org.example.autumn.resolver.AppConfig
import org.example.autumn.resolver.PropertyResolver
import org.example.autumn.servlet.DispatcherServlet.Companion.registerDispatcherServlet
import org.example.autumn.servlet.DispatcherServlet.Companion.registerFilters
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class ContextLoadListener : ServletContextListener {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun contextInitialized(sce: ServletContextEvent) {
        logger.info("init {}.", javaClass.name)
        val servletContext = sce.servletContext
        WebMvcConfiguration.servletContext = servletContext

        val config = servletContext.getAttribute("config") as? PropertyResolver ?: AppConfig.load()
        val encoding = config.getRequiredProperty("\${autumn.web.character-encoding:UTF-8}")
        servletContext.requestCharacterEncoding = encoding
        servletContext.responseCharacterEncoding = encoding
        val configClassName = servletContext.getInitParameter("configClassPath")
            ?: config.getRequiredProperty("autumn.config-class-path")
        val applicationContext = createApplicationContext(configClassName, config)
        logger.info("Application context created: {}", applicationContext)

        registerFilters(servletContext)
        registerDispatcherServlet(servletContext)
    }

    private fun createApplicationContext(
        configClassName: String, config: PropertyResolver
    ): ApplicationContext {
        logger.info("init ApplicationContext by configuration: {}", configClassName)
        if (configClassName.isEmpty()) {
            throw AutumnException("Cannot init ApplicationContext for missing configClassName", null)
        }
        val configClass = try {
            Class.forName(configClassName, true, Thread.currentThread().contextClassLoader)
        } catch (e: ClassNotFoundException) {
            throw AutumnException("Could not load autumn config class: $configClassName", null)
        }
        return AnnotationConfigApplicationContext(configClass, config)
    }
}