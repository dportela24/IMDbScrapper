package com.diogo_portela.imdb_scraper.controller

import com.diogo_portela.imdb_scraper.model.Series
import com.diogo_portela.imdb_scraper.service.SeriesService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("scrap")
class ScrapController(
    val seriesService: SeriesService
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/id/{imdbId}")
    fun scrapTitleById(@PathVariable imdbId: String) : ResponseEntity<Series> {
        MDC.put("request_id", UUID.randomUUID().toString())
        logger.info("Received scrap request for $imdbId")

        val series = seriesService.scrapTitle(imdbId)

        return ResponseEntity.ok(series)
    }
}