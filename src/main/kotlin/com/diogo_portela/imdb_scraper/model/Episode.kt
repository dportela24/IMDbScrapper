package com.diogo_portela.imdb_scraper.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.temporal.TemporalAccessor

data class Episode (
    val imdbId: String,
    val number: Int,
    val name: String,
    val airdate: TemporalAccessor?,
    val ratingValue: Float?,
    val ratingCount: Int?,
    val summary: String?
)