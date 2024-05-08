package org.example.autumn.app.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.example.autumn.annotation.Component
import org.example.autumn.annotation.Order
import org.example.autumn.servlet.FilterRegistrationBean
import org.slf4j.LoggerFactory

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