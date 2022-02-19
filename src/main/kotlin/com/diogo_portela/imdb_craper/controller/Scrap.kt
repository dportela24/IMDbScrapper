package com.diogo_portela.imdb_craper.controller

import com.diogo_portela.imdb_craper.model.Series
import com.diogo_portela.imdb_craper.service.ScrapingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("scrap")
class Scrap(
    val scrapingService: ScrapingService
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/title/id/{imdb_id}")
    fun scrapTitleById(@PathVariable imdb_id: String) : ResponseEntity<Series> {
        logger.info("Received request for $imdb_id")

        val result = scrapingService.scrapTitle(imdb_id)
        return ResponseEntity.ok(result)
    }
}