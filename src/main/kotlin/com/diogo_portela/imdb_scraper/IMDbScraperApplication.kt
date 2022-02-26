package com.diogo_portela.imdb_scraper

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ImDbScraperApplication

fun main(args: Array<String>) {
	runApplication<ImDbScraperApplication>(*args)
}
