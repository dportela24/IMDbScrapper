package com.diogo_portela.imdb_scraper.model.exception

class InvalidImdbIdException (
    override val message : String
) : RuntimeException(message)