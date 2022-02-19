package com.diogo_portela.imdb_craper.model

import java.time.Duration
import java.time.Year

data class Series (
    val imdbId: String? = null,
    val name: String? = null,
    val originalName: String? = null,
    val summary: String? = null,
    val episodeDuration: Duration? = null,
    val startYear: Int? = null,
    val endYear: Int? = null,
    val genres: Set<String>? = null,
    val ratingValue: Float? = null,
    val ratingCount: Int? = null,
    val posterURL: String? = null,
    val numberSeasons: Int? = null,
    val seasons: Set<Season>? = null
)