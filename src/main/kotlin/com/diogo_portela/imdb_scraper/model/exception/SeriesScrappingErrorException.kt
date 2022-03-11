package com.diogo_portela.imdb_scraper.model.exception

class SeriesScrappingErrorException(
    override val message: String
) : ScrappingErrorException(message) {
}