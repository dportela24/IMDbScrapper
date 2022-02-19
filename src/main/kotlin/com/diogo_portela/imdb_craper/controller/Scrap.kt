package com.diogo_portela.imdb_craper.controller

import com.diogo_portela.imdb_craper.model.Series
import com.diogo_portela.imdb_craper.service.SeasonService
import com.diogo_portela.imdb_craper.service.SeriesService
import kotlinx.coroutines.runBlocking
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
class Scrap(
    val seriesService: SeriesService
    //val seasonService: SeasonService
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/title/id/{imdbId}")
    fun scrapTitleById(@PathVariable imdbId: String) : ResponseEntity<Series> {
        MDC.put("request_id", UUID.randomUUID().toString())

        logger.info("Received request for $imdbId")

        val series = seriesService.scrapTitle(imdbId)
        logger.info("Returning request")
        return ResponseEntity.ok(series)
    }
}