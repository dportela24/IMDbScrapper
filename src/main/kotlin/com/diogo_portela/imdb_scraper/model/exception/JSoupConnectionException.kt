package com.diogo_portela.imdb_scraper.model.exception

class JSoupConnectionException(
    override val message: String
) : RuntimeException(message) {
}