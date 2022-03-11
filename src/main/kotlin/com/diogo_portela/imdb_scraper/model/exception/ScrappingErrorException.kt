package com.diogo_portela.imdb_scraper.model.exception

open class ScrappingErrorException(
    override val message: String
) : RuntimeException(message)