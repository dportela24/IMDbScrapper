package com.diogo_portela.imdb_craper.model.exception

class ErrorBuildingSeriesException(
    imdbId: String = "123",
    override val message: String
) : RuntimeException(generateErrorMessageHeader(imdbId) + message) {
    companion object {
        fun generateErrorMessageHeader(imdbId: String) = "Error while scraping Series $imdbId. "
    }
}