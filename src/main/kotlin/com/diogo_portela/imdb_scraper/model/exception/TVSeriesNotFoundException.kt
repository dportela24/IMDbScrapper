package com.diogo_portela.imdb_scraper.model.exception

class TVSeriesNotFoundException (
    override val message: String
) : RuntimeException(message)