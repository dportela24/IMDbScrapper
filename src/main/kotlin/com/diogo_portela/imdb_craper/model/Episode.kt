package com.diogo_portela.imdb_craper.model

import java.util.Date

data class Episode (
    val imdbId: String,
    val number: Int,
    val name: String,
    val airdate: Date,
    val ratingValue: Float,
    val ratingCount: Int,
    val summary: String
)