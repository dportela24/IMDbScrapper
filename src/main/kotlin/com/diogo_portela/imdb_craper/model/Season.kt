package com.diogo_portela.imdb_craper.model

data class Season (
    val number: Int? = null,
    val numberEpisodes: Int? = null,
    val episodes: Set<Episode>? = null
)