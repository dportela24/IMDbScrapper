package com.diogo_portela.imdb_craper.controller

import com.diogo_portela.imdb_craper.service.ScrapingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("scrap")
class Scrap(
    val scrapingService: ScrapingService
) {
    @GetMapping("/title/id/{imdb_id}")
    fun scrapTitleById(@PathVariable imdb_id: String) : ResponseEntity<String> {
        val result = scrapingService.scrapTitle(imdb_id)
        return ResponseEntity.ok(result)
    }
}