package com.diogo_portela.imdb_scraper.model.exception

class SeasonScrappingErrorException(
    override val message: String
) : ScrappingErrorException(message) {
}