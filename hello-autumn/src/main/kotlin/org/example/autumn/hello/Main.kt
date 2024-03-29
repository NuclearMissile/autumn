package org.example.autumn.hello

import com.example.autumn.annotation.*
import com.example.autumn.boot.AutumnApplication
import com.example.autumn.servlet.FilterRegistrationBean
import com.example.autumn.servlet.ModelAndView
import com.example.autumn.servlet.WebMvcConfiguration
import com.example.autumn.utils.JsonUtils.toJson
import com.example.autumn.utils.JsonUtils.writeJson
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        AutumnApplication.run(
            "src/main/webapp", "target/classes", HelloConfiguration::class.java, *args
        )
    }
}

@ComponentScan
@Configuration
@Import(WebMvcConfiguration::class)
class HelloConfiguration

@Order(200)
@Component
class LogFilterRegistrationBean : FilterRegistrationBean() {
    override val urlPatterns: List<String>
        get() = listOf("/*")
    override val filter: Filter
        get() = LogFilter()

    class LogFilter : Filter {
        private val logger = LoggerFactory.getLogger(javaClass)
        override fun doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
            val httpReq = req as HttpServletRequest
            logger.info("{}: {}", httpReq.method, httpReq.requestURI)
            chain.doFilter(req, resp)
        }
    }
}

@Order(200)
@Component
class ApiErrorFilterReg : FilterRegistrationBean() {
    override val urlPatterns: List<String>
        get() = listOf("/api/*")
    override val filter: Filter
        get() = ApiErrorFilter()

    class ApiErrorFilter : Filter {
        private val logger = LoggerFactory.getLogger(javaClass)
        override fun doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
            try {
                chain.doFilter(req, resp)
            } catch (e: ServletException) {
                req as HttpServletRequest
                resp as HttpServletResponse
                logger.warn("api error when handling {}: {}", req.method, req.requestURI)
                if (!resp.isCommitted) {
                    resp.apply {
                        reset()
                        status = 400
                        writer.writeJson(mapOf("error" to true, "type" to (e.cause ?: e).javaClass.simpleName)).flush()
                    }
                }
            }
        }
    }
}

@Controller
class MvcController {
    @Get("/")
    fun index(): String {
        return "redirect:/hello"
    }

    @Get("/hello")
    fun hello(): ModelAndView {
        return ModelAndView("/hello.html")
    }
}

@RestController
class RestApiController {
    @Get("/api/hello/{name}")
    @ResponseBody
    fun hello(@PathVariable("name") name: String): String {
        return mapOf("name" to name).toJson()
    }

    @Get("/api/error")
    @ResponseBody
    fun error() {
        throw AssertionError("test error")
    }
}