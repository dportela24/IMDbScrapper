package com.diogo_portela.imdb_scraper.model

import java.time.Duration

data class Series (
    val imdbId: String,
    val name: String,
    val originalName: String?,
    val summary: String?,
    val episodeDuration: Duration?,
    val startYear: Int,
    val endYear: Int?,
    val genres: Set<String>,
    val ratingValue: Float?,
    val ratingCount: Int?,
    val posterURL: String?,
    val numberSeasons: Int,
    val seasons: Set<Season>
)