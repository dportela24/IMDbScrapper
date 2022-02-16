package com.diogo_portela.imdb_craper.model

import java.time.Duration

data class Title (
    val imdbID: String,
    val name: String,
    val originalName: String,
    val summary: String,
    val episodeDuration: Duration,
    val startingYear: Int,
    val endingYear: Int,
    val genres: Set<String>,
    val ratingValue: Float,
    val ratingCount: Int,
    val posterURL: String,
    val numberSeasons: Int,
    val seasons: Set<Season>
)