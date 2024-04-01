package com.example.autumn.servlet

import jakarta.servlet.http.HttpServletResponse

class ModelAndView(
    val viewName: String, initModel: Map<String, Any> = mutableMapOf(), val status: Int = HttpServletResponse.SC_OK,
) {
    private val model: MutableMap<String, Any> = initModel.toMutableMap()

    fun getModel(): Map<String, Any> = model

    fun addModel(map: Map<String, Any>) {
        model += map
    }

    fun addModel(key: String, value: Any) {
        model[key] = value
    }
}