package com.diogo_portela.imdb_scraper.model.exception

class EmptySearchResultException (
    override val message: String = "Search returned no results"
) : RuntimeException(message)