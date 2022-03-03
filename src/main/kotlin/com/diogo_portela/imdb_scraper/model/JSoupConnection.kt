package com.diogo_portela.imdb_scraper.model

import org.jsoup.Jsoup

class JSoupConnection (
    private val baseURL: String,
    private val headers: MutableMap<String, String> = mutableMapOf(),
    private var userAgent: String? = null
) {
    fun header(key: String, value: String) : JSoupConnection {
        headers[key] = value
        return this
    }

    fun userAgent(agent: String) : JSoupConnection {
        userAgent = agent
        return this
    }

    fun newConnection(path: String) = Jsoup
        .newSession()
        .url(baseURL + path)
        .timeout(10000)
        .headers(headers)
        .ignoreHttpErrors(true)
        .also { conn ->
            userAgent?.apply { conn.userAgent(this) }
        }
}