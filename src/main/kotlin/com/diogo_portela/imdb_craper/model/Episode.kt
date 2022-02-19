package com.diogo_portela.imdb_craper.model

import java.time.LocalDate

data class Episode (
    val imdbId: String? = null,
    val number: Int? = null,
    val name: String? = null,
    val airdate: LocalDate? = null,
    val ratingValue: Float? = null,
    val ratingCount: Int? = null,
    val summary: String? = null
)