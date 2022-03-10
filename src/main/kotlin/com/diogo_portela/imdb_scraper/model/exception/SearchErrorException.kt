package com.diogo_portela.imdb_scraper.model.exception

class SearchErrorException (
    override val message: String
) : RuntimeException(message)