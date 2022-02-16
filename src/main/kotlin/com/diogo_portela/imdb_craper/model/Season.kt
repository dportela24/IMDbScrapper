package com.diogo_portela.imdb_craper.model

data class Season (
    val number: Int,
    val episodes: Set<Episode>
)