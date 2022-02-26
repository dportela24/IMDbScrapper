package com.diogo_portela.imdb_scraper.model

import java.time.LocalDate

data class Episode (
    val imdbId: String,
    val number: Int,
    val name: String,
    val airdate: LocalDate,
    val ratingValue: Float,
    val ratingCount: Int,
    val summary: String
)