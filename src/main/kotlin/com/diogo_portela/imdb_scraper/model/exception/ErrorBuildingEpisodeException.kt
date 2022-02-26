package com.diogo_portela.imdb_scraper.model.exception

class ErrorBuildingEpisodeException(
    override val message: String
) : RuntimeException(message) {
}