package com.diogo_portela.imdb_scraper.model.exception

class SearchScrappingErrorException (
    override val message: String
) : ScrappingErrorException(message)