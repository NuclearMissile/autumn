package org.example.autumn.hello

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.annotation.WebListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.example.autumn.annotation.*
import org.example.autumn.aop.AroundAopConfiguration
import org.example.autumn.aop.BeforeInvocationHandlerAdapter
import org.example.autumn.db.DbConfiguration
import org.example.autumn.exception.ResponseErrorException
import org.example.autumn.resolver.Config
import org.example.autumn.server.AutumnServer
import org.example.autumn.servlet.ContextLoadListener
import org.example.autumn.servlet.FilterRegistrationBean
import org.example.autumn.servlet.ModelAndView
import org.example.autumn.servlet.WebMvcConfiguration
import org.example.autumn.utils.JsonUtils.toJson
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        AutumnServer.start(
            "src/main/webapp", Config.load(), javaClass.classLoader, listOf(HelloContextLoadListener::class.java)
        )
    }
}

@ComponentScan
@Configuration
@Import(WebMvcConfiguration::class, DbConfiguration::class, AroundAopConfiguration::class)
class HelloConfiguration

@Component
class BeforeLogInvocationHandler : BeforeInvocationHandlerAdapter() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun before(proxy: Any, method: Method, args: Array<Any>?) {
        logger.info("[Before] ${method.declaringClass.toString().removePrefix("class ")}.${method.name}")
    }
}

@WebListener
class HelloContextLoadListener : ContextLoadListener()

@Order(100)
@Component
class LogFilter : FilterRegistrationBean() {
    override val urlPatterns = listOf("/*")
    override val filter = object : Filter {
        private val logger = LoggerFactory.getLogger(javaClass)
        override fun doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
            val httpReq = req as HttpServletRequest
            logger.info("{}: {}", httpReq.method, httpReq.requestURI)
            chain.doFilter(req, resp)
        }
    }
}

@Controller("/hello")
class HelloController {
    @Get("/")
    fun hello(): ModelAndView {
        return ModelAndView("/hello.html")
    }

    @Get("/error")
    fun error(): ModelAndView {
        return ModelAndView("/hello.html", mapOf(), 400)
    }

    @Get("/error/{errorCode}/{errorResp}")
    fun error(@PathVariable errorCode: Int, @PathVariable errorResp: String) {
        throw ResponseErrorException(errorCode, "test", errorResp)
    }
}

@Controller
class IndexController(@Autowired private val userService: UserService) {
    companion object {
        const val USER_SESSION_KEY = "USER_SESSION_KEY"
    }

    @PostConstruct
    fun init() {
        // @Transactional proxy of UserService injected
        assert(userService.javaClass != UserService::class.java)
    }

    @Get("/")
    fun index(session: HttpSession): ModelAndView {
        val user = session.getAttribute(USER_SESSION_KEY)
        return if (user == null)
            ModelAndView("redirect:/login") else ModelAndView("/index.ftl", mapOf("user" to user))
    }

    @Get("/register")
    fun register(): ModelAndView {
        return ModelAndView("/register.ftl")
    }

    @Post("/register")
    fun register(
        @RequestParam email: String, @RequestParam name: String, @RequestParam password: String
    ): ModelAndView {
        return if (userService.register(email, name, password) != null)
            ModelAndView("redirect:/login")
        else
            ModelAndView("/register.ftl", mapOf("error" to "$email already registered"))
    }

    @Get("/login")
    fun login(): ModelAndView {
        return ModelAndView("/login.ftl")
    }

    @Post("/login")
    fun login(@RequestParam email: String, @RequestParam password: String, session: HttpSession): ModelAndView {
        val user = userService.login(email, password)
            ?: return ModelAndView("/login.ftl", mapOf("error" to "email or password is incorrect"))
        session.setAttribute(USER_SESSION_KEY, user)
        return ModelAndView("redirect:/")
    }

    @Get("/logoff")
    fun logoff(session: HttpSession): String {
        session.removeAttribute(USER_SESSION_KEY)
        return "redirect:/login"
    }
}

@Around("beforeLogInvocationHandler")
@RestController("/api")
class RestApiController {
    @Get("/hello/{name}")
    @ResponseBody
    fun hello(@PathVariable name: String): String {
        return mapOf("name" to name).toJson()
    }

    @Get("/params")
    fun params(@RequestParam test: String): Map<String, String> {
        return mapOf("test" to test)
    }

    @Get("/error")
    fun error() {
        throw Exception("api test error")
    }

    @Get("/error/{errorCode}/{errorResp}")
    fun error(@PathVariable errorCode: Int, @PathVariable errorResp: String) {
        throw ResponseErrorException(
            errorCode, "test", mapOf("errorCode" to errorCode, "errorResp" to errorResp).toJson()
        )
    }
}