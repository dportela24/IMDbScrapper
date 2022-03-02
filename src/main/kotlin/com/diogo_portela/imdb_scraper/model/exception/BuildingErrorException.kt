package com.diogo_portela.imdb_scraper.model.exception

open class BuildingErrorException(
    override val message: String
) : RuntimeException(message)