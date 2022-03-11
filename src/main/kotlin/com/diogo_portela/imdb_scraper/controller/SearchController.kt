package com.diogo_portela.imdb_scraper.controller

import com.diogo_portela.imdb_scraper.model.SearchResult
import com.diogo_portela.imdb_scraper.service.SearchService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.math.min

@RestController
class SearchController(
    val searchService: SearchService,
    @Value("\${SEARCH_RESULT_LIMIT}") val searchLimit: Int
) {
    val logger = LoggerFactory.getLogger(this::class.java)



    @GetMapping("/search/name")
    fun searchByName(
        @RequestParam(name = "q") searchInput: String?,
        @RequestParam(name = "limit", required = false) limitRequest: Int?
    ) : ResponseEntity<Set<SearchResult>> {
        MDC.put("request_id", UUID.randomUUID().toString())
        logger.info("Received searchByName request for $searchInput")

        val limit = limitRequest?.let{ min(it, searchLimit) } ?: searchLimit

        val imdbIds = searchService.searchByName(searchInput, limit)

        return ResponseEntity.ok(imdbIds)
    }
}