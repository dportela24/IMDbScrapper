package com.diogo_portela.imdb_scraper.helper

fun matchGroupsInRegex(input: String, pattern: String) : List<String>? {
    return if (Regex(pattern).matches(input)) {
        val regex = Regex(pattern).find(input)
        return regex?.groupValues
    } else {
        null
    }
}

fun generateTitleUrl(imdbId: String) = "/title/$imdbId/"

fun generateSeasonUrl(imdbId: String, seasonNumber: Int) : String
    = "/title/$imdbId/episodes?season=$seasonNumber"

fun generateParseErrorMessage(field: String, input: String?) : String {
    return input?.run { "Could not parse $field. Input string was $input. " }
            ?: "Could not find $field text element"
}