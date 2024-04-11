package org.example.autumn.app

import org.example.autumn.annotation.*
import org.example.autumn.boot.AutumnApplication
import org.example.autumn.servlet.ModelAndView
import org.example.autumn.servlet.WebMvcConfiguration
import org.example.autumn.utils.JsonUtils.toJson

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

@Controller
class MvcController {
    @Get("/")
    fun index(@Header("Cookie") a: String?): ModelAndView {
        return ModelAndView("/index.html")
    }

    @Get("/hello")
    fun hello(): ModelAndView {
        return ModelAndView("/hello.html")
    }

    @Get("/error/{errorCode}")
    fun error(@PathVariable("errorCode") errorCode: Int): ModelAndView {
        return ModelAndView("/hello.html", emptyMap(), errorCode)
    }
}

@RestController
class RestApiController {
    @Get("/api/hello/{name}")
    @ResponseBody
    fun hello(@PathVariable("name") name: String): String {
        return mapOf("name" to name).toJson()
    }

    @Get("/api/params")
    fun params(@RequestParam("test") test: String): Map<String, String> {
        return mapOf("test" to test)
    }
}