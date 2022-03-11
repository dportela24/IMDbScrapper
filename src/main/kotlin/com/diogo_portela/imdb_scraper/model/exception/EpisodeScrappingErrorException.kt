package com.diogo_portela.imdb_scraper.model.exception

class EpisodeScrappingErrorException(
    override val message: String
) : ScrappingErrorException(message) {
}