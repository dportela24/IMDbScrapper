package com.diogo_portela.imdb_craper.model.exception

class ErrorBuildingSeasonException(
    override val message: String
) : RuntimeException(message) {
}