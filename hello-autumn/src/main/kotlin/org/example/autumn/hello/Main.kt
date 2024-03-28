package org.example.autumn.hello

import com.example.autumn.annotation.*
import com.example.autumn.servlet.ModelAndView
import com.example.autumn.servlet.WebMvcConfiguration
import com.example.autumn.utils.JsonUtils.toJson
import org.example.autumn.boot.AutumnApplication

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
}