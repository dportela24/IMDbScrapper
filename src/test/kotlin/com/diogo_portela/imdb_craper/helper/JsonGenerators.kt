package com.diogo_portela.imdb_craper.helper

import com.diogo_portela.imdb_craper.model.ApplicationLinkedData
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun generateApplicationLinkedDataJson(
    type: String = "TVSeries",
    name: String = "My TV Show",
    alternateName: String = "My Alternate TV Show",
    image: String = "http://myposter.com",
    description: String = "A simple TV Show",
    aggregateRating: ApplicationLinkedData.AggregateRating = generateAggregasteRating(),
    genre: Set<String> = setOf("Comedy", "Drama", "Romance")
) = jacksonObjectMapper().writeValueAsString(
    mapOf(
        "@type" to type,
        "name" to name,
        "alternateName" to alternateName,
        "image" to image,
        "description" to description,
        "aggregateRating" to mapOf(
            "ratingCount" to aggregateRating.ratingCount,
            "ratingValue" to aggregateRating.ratingValue
        ),
        "genre" to genre
    )
)

fun generateAggregasteRatingJson(
    ratingCount: Int = 1234,
    ratingValue: Float = 8.2F
) = jacksonObjectMapper().writeValueAsString(
    mapOf(
        "ratingCount" to ratingCount,
        "ratingValue" to ratingValue
    )
)