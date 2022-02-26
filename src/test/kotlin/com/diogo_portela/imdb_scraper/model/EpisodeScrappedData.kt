package com.diogo_portela.imdb_scraper.model

data class EpisodeScrappedData (
    var url: String?,
    var number: String?,
    var name: String?,
    var airdate: String?,
    var ratingValue: String?,
    var ratingCount: String?,
    var summary: String?
)