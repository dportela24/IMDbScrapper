package com.diogo_portela.imdb_craper.service

import com.diogo_portela.imdb_craper.model.JSoupConnection
import com.diogo_portela.imdb_craper.model.Title
import org.springframework.stereotype.Service

@Service
class ScrapingService(
    val jSoupConnection: JSoupConnection
) {
    fun scrapTitle(imdbId: String) : String {
        val jsoup = jSoupConnection.newConnection("/title/$imdbId")
        return jsoup.get().html()
    }
}