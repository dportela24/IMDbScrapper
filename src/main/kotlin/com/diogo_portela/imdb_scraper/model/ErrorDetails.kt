package com.diogo_portela.imdb_scraper.model

data class ErrorDetails(
    val errorCode: String,
    val errorType: String,
    val errorMessage: String? = null
) {
    class ErrorCode {
        companion object {
            val NOT_A_TV_SERIES_ERROR = "000001"
            val CONNECTION_ERROR      = "000002"
            val BUILDING_ERROR        = "000003"
        }
    }
}