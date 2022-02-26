package com.diogo_portela.imdb_scraper.model.exception

class JSoupConnectionException(
    override val message: String
) : RuntimeException(ERROR_MESSAGE_HEADER + message) {
    companion object {
        val ERROR_MESSAGE_HEADER = "Error while fetching HTML. "
    }
}