package com.diogo_portela.imdb_craper.helper

import com.diogo_portela.imdb_craper.model.ApplicationLinkedData
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun generateApplicationLinkedDataJson(
    type: String = "TVSeries",
    name: String = "My TV Show",
    alternateName: String? = null,
    image: String = "http://myposter.com",
    description: String = "A simple TV Show",
    aggregateRating: ApplicationLinkedData.AggregateRating = generateAggregasteRating(),
    genre: Set<String> = setOf("Comedy", "Drama", "Romance")
) = jacksonObjectMapper().writeValueAsString(
    mutableMapOf(
        "@type" to type,
        "name" to name,
        "image" to image,
        "description" to description,
        "aggregateRating" to mapOf(
            "ratingCount" to aggregateRating.ratingCount,
            "ratingValue" to aggregateRating.ratingValue
        ),
        "genre" to genre
    ).also { map ->
        alternateName?.run {
            map["alternateName"] = this
        }
    }
)

fun generateApplicationLinkedDataJson(
    linkedData: ApplicationLinkedData
) = jacksonObjectMapper().writeValueAsString(
    mutableMapOf(
        "@type" to linkedData.type,
        "name" to linkedData.name,
        "image" to linkedData.image,
        "description" to linkedData.description,
        "aggregateRating" to mapOf(
            "ratingCount" to linkedData.aggregateRating.ratingCount,
            "ratingValue" to linkedData.aggregateRating.ratingValue
        ),
        "genre" to linkedData.genre
    ).also { map ->
        linkedData.alternateName?.run {
            map["alternateName"] = this
        }
    }
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