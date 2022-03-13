package com.diogo_portela.imdb_scraper.model

data class ErrorDetails(
    val errorCode: String,
    val errorType: String,
    val errorMessage: String? = null
) {
    class ErrorCode {
        companion object {
            val NO_ENDPOINT_HANDLER   = "000001"
            val NO_HTTP_METHOD        = "000002"
            val NOT_A_TV_SERIES_ERROR = "000003"
            val CONNECTION_ERROR      = "000004"
            val SCRAPPING_ERROR       = "000005"
            val INVALID_IMDB_ID       = "000006"
            val TV_SERIES_NOT_FOUND   = "000007"
            val MISSING_PARAMETERS    = "000008"
            val NO_SEARCH_RESULTS     = "000009"
            val UNEXPECTED_ERROR      = "111111"
        }
    }
}