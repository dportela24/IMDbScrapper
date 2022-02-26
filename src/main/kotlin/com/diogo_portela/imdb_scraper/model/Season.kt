package com.diogo_portela.imdb_scraper.model

data class Season (
    val number: Int,
    val numberEpisodes: Int,
    val episodes: Set<Episode>
)