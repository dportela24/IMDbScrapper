package com.diogo_portela.imdb_scraper.model.exception

class ErrorBuildingSeasonException(
    override val message: String
) : BuildingErrorException(message) {
}