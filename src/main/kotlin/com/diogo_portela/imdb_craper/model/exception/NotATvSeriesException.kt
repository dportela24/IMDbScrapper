package com.diogo_portela.imdb_craper.model.exception

class NotATvSeriesException(
    override val message: String
) : RuntimeException(message) {
}