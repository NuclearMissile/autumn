package org.example.autumn.hello

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.annotation.WebListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.example.autumn.annotation.*
import org.example.autumn.aop.InvocationHandlerAdapter
import org.example.autumn.eventbus.Event
import org.example.autumn.eventbus.EventBus
import org.example.autumn.eventbus.EventMode
import org.example.autumn.exception.ResponseErrorException
import org.example.autumn.resolver.Config
import org.example.autumn.server.AutumnServer
import org.example.autumn.servlet.*
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

@WebListener
class HelloContextLoadListener : ContextLoadListener()

//@ComponentScan
//@Configuration
//@Import(WebMvcConfiguration::class, DbConfiguration::class, AroundAopConfiguration::class, EventBusConfig::class)
//class HelloConfiguration

@Component
class BeforeLogInvocationHandler : InvocationHandlerAdapter {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun before(proxy: Any, method: Method, args: Array<Any?>?) {
        logger.info("[Before] ${method.declaringClass.toString().removePrefix("class ")}.${method.name}")
    }
}

@Order(100)
@Component
class LogFilter : FilterRegistrationBean() {
    override val urlPatterns = listOf("/*")
    override val filter = object : Filter {
        private val logger = LoggerFactory.getLogger(javaClass)
        override fun doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
            val startTime = System.currentTimeMillis()
            val httpReq = req as HttpServletRequest
            val httpResp = resp as HttpServletResponse
            chain.doFilter(req, resp)
            logger.info(
                "{}: {} [{}, ${System.currentTimeMillis() - startTime}ms]",
                httpReq.method, httpReq.requestURI, httpResp.status
            )
        }
    }
}

@Component
class LoginEventListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Subscribe(EventMode.SYNC)
    fun onLogin(e: LoginEvent) {
        logger.info("[Login] ${e.user}")
    }

    @Subscribe(EventMode.ASYNC)
    fun onLogoff(e: LogoffEvent) {
        logger.info("[Logoff] ${e.user}")
    }
}

data class LoginEvent(val user: User) : Event
data class LogoffEvent(val user: User) : Event

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
class IndexController @Autowired constructor(private val userService: UserService) {
    companion object {
        const val USER_SESSION_KEY = "USER_SESSION_KEY"
    }

    @Autowired
    private lateinit var eventBus: EventBus

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
    fun register(session: HttpSession): ModelAndView {
        val user = session.getAttribute(USER_SESSION_KEY)
        return if (user == null)
            ModelAndView("/register.ftl") else ModelAndView("redirect:/")
    }

    @Post("/register")
    fun register(
        @RequestParam email: String, @RequestParam name: String, @RequestParam password: String,
    ): ModelAndView {
        if (name.isBlank()) return ModelAndView("/register.ftl", mapOf("error" to "name is blank"))
        return if (userService.register(email, name, password) != null)
            ModelAndView("redirect:/login")
        else
            ModelAndView("/register.ftl", mapOf("error" to "email is already registered"))
    }

    @Get("/login")
    fun login(session: HttpSession): ModelAndView {
        val user = session.getAttribute(USER_SESSION_KEY)
        return if (user == null)
            ModelAndView("/login.ftl") else ModelAndView("redirect:/")
    }

    @Post("/login")
    fun login(@RequestParam email: String, @RequestParam password: String, session: HttpSession): ModelAndView {
        val user = userService.login(email, password)
            ?: return ModelAndView("/login.ftl", mapOf("error" to "email or password is incorrect"))
        session.setAttribute(USER_SESSION_KEY, user)
        eventBus.post(LoginEvent(user))
        return ModelAndView("redirect:/")
    }

    @Get("/logoff")
    fun logoff(session: HttpSession): String {
        val user = session.getAttribute(USER_SESSION_KEY) as User
        session.removeAttribute(USER_SESSION_KEY)
        eventBus.post(LogoffEvent(user))
        return "redirect:/login"
    }

    @Get("/error/{errorCode}")
    fun error(@PathVariable errorCode: Int) {
        throw ResponseErrorException(errorCode, "test")
    }

    @Get("/echo")
    fun echo(req: RequestEntity): ResponseEntity {
        return ResponseEntity(req, 200, "application/json")
    }
}

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

    @Get("/400")
    fun error400(req: RequestEntity): ResponseEntity {
        return ResponseEntity("400 error", 400)
    }

    @Get("/error/{errorCode}/{errorResp}")
    fun error(@PathVariable errorCode: Int, @PathVariable errorResp: String) {
        throw ResponseErrorException(
            errorCode, "test", mapOf("errorCode" to errorCode, "errorResp" to errorResp).toJson()
        )
    }
}