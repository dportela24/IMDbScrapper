package com.diogo_portela.imdb_craper.model.exception

class ErrorBuildingEpisodeException(
    override val message: String
) : RuntimeException(message) {
}