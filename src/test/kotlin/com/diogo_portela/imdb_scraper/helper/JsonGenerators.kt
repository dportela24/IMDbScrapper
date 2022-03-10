package com.diogo_portela.imdb_scraper.helper

import com.diogo_portela.imdb_scraper.model.TitleApplicationLinkedData
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun generateApplicationLinkedDataJson(
    type: String = "TVSeries",
    name: String = "My TV Show",
    alternateName: String? = null,
    image: String? = "http://myposter.com",
    description: String? = "A simple TV Show",
    aggregateRating: TitleApplicationLinkedData.AggregateRating? = generateAggregasteRating(),
    genre: Set<String> = setOf("Comedy", "Drama", "Romance")
) = jacksonObjectMapper().writeValueAsString(
    mutableMapOf(
        "@type" to type,
        "name" to name,
        "genre" to genre
    ).also { map ->
        alternateName?.run {
            map["alternateName"] = this
        }
        aggregateRating?.run {
            map["aggregateRating"] = mapOf(
                "ratingCount" to this.ratingCount,
                "ratingValue" to this.ratingValue
            )
        }
        image?.run {
            map["image"] = this
        }
        description?.run {
            map["description"] = this
        }
    }
)

fun generateApplicationLinkedDataJson(
    linkedData: TitleApplicationLinkedData
) = jacksonObjectMapper().writeValueAsString(
    mutableMapOf(
        "@type" to linkedData.type,
        "name" to linkedData.name,
        "genre" to linkedData.genre
    ).also { map ->
        linkedData.alternateName?.run {
            map["alternateName"] = this
        }
        linkedData.aggregateRating?.run {
            map["aggregateRating"] = mapOf(
                "ratingCount" to this.ratingCount,
                "ratingValue" to this.ratingValue
            )
        }
        linkedData.image?.run {
            map["image"] = this
        }
        linkedData.description?.run {
            map["description"] = this
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