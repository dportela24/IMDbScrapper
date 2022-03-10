package com.diogo_portela.imdb_scraper.model.exception

class MissingParametersException(
    override val message: String
) : RuntimeException() {
    constructor(parameters: List<String>) :
       this("Missing the following required parameters: $parameters")
}