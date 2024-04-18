package org.example.autumn.server.component.mapping

import jakarta.servlet.Filter
import jakarta.servlet.Servlet
import java.util.regex.Pattern

abstract class UrlMapping(val url: String) : Comparable<UrlMapping> {
    val pattern: Pattern = run {
        val sb = StringBuilder(url.length + 16)
        sb.append('^')
        url.forEach {
            when (it) {
                '*' -> sb.append(".*")
                in 'a'..'z', in 'A'..'Z', in '0'..'9' -> sb.append(it)
                else -> sb.append("\\").append(it)
            }
        }
        sb.append('$')
        Pattern.compile(sb.toString())
    }

    private val priority: Int =
        if (this.url == "/") {
            Int.MAX_VALUE
        } else if (this.url.startsWith("*")) {
            Int.MAX_VALUE - 1
        } else 100000 - this.url.length


    fun matches(uri: String): Boolean {
        return pattern.matcher(uri).matches()
    }

    override fun compareTo(other: UrlMapping): Int {
        val ret = priority - other.priority
        return if (ret == 0) url.compareTo(other.url) else ret
    }
}

class FilterMapping(val filter: Filter, val filterName: String, url: String) : UrlMapping(url)

class ServletMapping(val servlet: Servlet, url: String) : UrlMapping(url)