package com.diogo_portela.imdb_craper.helper

fun matchGroupsInRegex(input: String, pattern: String) : List<String>? {
    val regex = Regex(pattern).find(input)
    return regex?.groupValues
}