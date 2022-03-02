package com.diogo_portela.imdb_scraper.model.exception

class ErrorBuildingSeriesException(
    override val message: String
) : BuildingErrorException(message) {
}