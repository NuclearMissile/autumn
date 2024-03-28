package com.example.autumn.web

import jakarta.servlet.http.HttpServletResponse

class ModelAndView(
    val viewName: String,
    model: Map<String, Any> = mutableMapOf(),
    val status: Int = HttpServletResponse.SC_OK,
) {
    private val model: MutableMap<String, Any> = model.toMutableMap()

    fun getModel(): Map<String, Any> = model

    fun addModel(map: Map<String, Any>) {
        model += map
    }

    fun addModel(key: String, value: Any) {
        model[key] = value
    }
}