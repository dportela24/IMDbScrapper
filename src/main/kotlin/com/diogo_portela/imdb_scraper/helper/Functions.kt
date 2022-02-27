package com.diogo_portela.imdb_scraper.helper

import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

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

fun generateErrorMessage(field: String, input: String? = null) : String {
    return input?.run { "Could not parse $field. Input string was $input. " }
            ?: "Could not find $field element"
}

fun getParseDateFunctions() : Set<(String) -> TemporalAccessor> = setOf(
    { input -> LocalDate.parse(input, DateTimeFormatter.ofPattern("d MMM[.] yyyy")) },
    { input -> YearMonth.parse(input, DateTimeFormatter.ofPattern("MMM[.] yyyy")) },
    { input -> Year.parse(input, DateTimeFormatter.ofPattern("yyyy")) }
)