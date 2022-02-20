package com.diogo_portela.imdb_craper.model.exception

class ErrorBuildingSeriesException(
    override val message: String
) : RuntimeException(message) {
}