package com.diogo_portela.imdb_craper.helper

fun matchGroupsInRegex(input: String, pattern: String) : List<String>? {
    return if (Regex(pattern).matches(input)) {
        val regex = Regex(pattern).find(input)
        return regex?.groupValues
    } else {
        null
    }
}

fun generateTitleUrl(imdbId: String) = "/title/$imdbId"
