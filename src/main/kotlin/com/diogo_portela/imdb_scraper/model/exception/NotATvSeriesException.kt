package com.diogo_portela.imdb_scraper.model.exception

class NotATvSeriesException(
    override val message: String
) : RuntimeException(message) {
}