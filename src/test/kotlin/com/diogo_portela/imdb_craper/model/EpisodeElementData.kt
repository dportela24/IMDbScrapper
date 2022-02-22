package com.diogo_portela.imdb_craper.model

import java.time.LocalDate

data class EpisodeElementData (
    val url: String?,
    val number: String?,
    val name: String?,
    val airdate: String?,
    val ratingValue: String?,
    val ratingCount: String?,
    val summary: String?
)