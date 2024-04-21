package org.example.autumn.utils

import org.example.autumn.annotation.Bean
import org.example.autumn.annotation.Component
import org.example.autumn.exception.BeanDefinitionException
import org.example.autumn.resolver.ResourceResolver
import java.lang.reflect.Method

object ClassUtils {
    /**
     * 递归查找Annotation
     *
     * 示例：Annotation A可以直接标注在Class定义:
     *
     * @A
     * public class Hello {}
     *
     * 或者Annotation B标注了A，Class标注了B:
     *
     * @A
     * public @interface B {}
     *
     * @B
     * public class Hello {}
     *
     */
    fun <A : Annotation> findAnnotation(target: Class<*>, annoClass: Class<A>): A? {
        var res = target.getAnnotation(annoClass)
        for (anno in target.annotations) {
            val annoType = anno.annotationClass.java
            if (annoType.packageName.startsWith("java") || annoType.packageName.startsWith("kotlin"))
                continue
            val found = findAnnotation(annoType, annoClass)
            if (found != null) {
                if (res != null) {
                    throw BeanDefinitionException("Duplicate @${annoClass.simpleName} found on class ${target.simpleName}")
                }
                res = found
            }
        }
        return res
    }

    fun <A : Annotation> findAnnotation(annos: List<Annotation>, annoClass: Class<A>): A? {
        for (anno in annos) {
            if (annoClass.isInstance(anno)) {
                return anno as A
            }
        }
        return null
    }


    /**
     * Get bean name by:
     *
     *
     * @Bean
     * Hello createHello() {}
     *
     */
    fun getBeanName(method: Method): String {
        return method.getAnnotation(Bean::class.java).value.ifEmpty { method.name }
    }

    /**
     * Get bean name by:
     *
     *
     * @Component
     * public class Hello {}
     *
     */
    fun getBeanName(clazz: Class<*>): String {
        // 查找@Component:
        var name = ""
        val component = clazz.getAnnotation(Component::class.java)
        if (component != null) {
            name = component.value
        }
        if (component == null) {
            // 未找到@Component，继续在其他注解中查找@Component:
            for (anno in clazz.annotations) {
                if (findAnnotation(anno.annotationClass.java, Component::class.java) != null) {
                    try {
                        name = anno.annotationClass.java.getMethod("value").invoke(anno) as String
                    } catch (e: ReflectiveOperationException) {
                        throw BeanDefinitionException("Cannot get annotation value.", e)
                    }
                }
            }
        }
        return name.ifEmpty {
            clazz.simpleName.replaceFirstChar { it.lowercase() }
        }
    }

    /**
     * Get non-arg method by @PostConstruct or @PreDestroy. Not search in super
     * class.
     *
     *
     * @PostConstruct void init() {}
     *
     */
    fun findAnnotationMethod(clazz: Class<*>, annoClass: Class<out Annotation>): Method? {
        // try get declared method:
        val ms = clazz.declaredMethods.filter { it.isAnnotationPresent(annoClass) }
        ms.forEach {
            if (it.parameterCount != 0) {
                throw BeanDefinitionException(
                    "Method '${it.name}' with @${annoClass.simpleName} must not have argument: ${clazz.name}"
                )
            }
        }
        if (ms.size > 1) {
            throw BeanDefinitionException(
                "Multiple methods with @${annoClass.simpleName} found in class: ${clazz.name}"
            )
        }
        return ms.firstOrNull()
    }

    /**
     * Get non-arg method by method name. Not search in super class.
     */
    fun getNamedMethod(clazz: Class<*>, methodName: String): Method {
        return try {
            clazz.getDeclaredMethod(methodName)
        } catch (e: ReflectiveOperationException) {
            throw BeanDefinitionException("Method '$methodName' not found in class: ${clazz.name}")
        }
    }

    fun scanClassNames(basePackagePaths: List<String>): Set<String> {
        return basePackagePaths.flatMap {
            ResourceResolver(it).scanResources { res ->
                if (res.name.endsWith(".class"))
                    res.name.removeSuffix(".class").replace("/", ".").replace("\\", ".")
                else null
            }
        }.toSet()
    }

    fun <T> createInstance(className: String): T {
        return createInstance(Class.forName(className)) as T
    }

    fun <T> createInstance(clazz: Class<T>): T {
        return clazz.getConstructor().newInstance()
    }
}
