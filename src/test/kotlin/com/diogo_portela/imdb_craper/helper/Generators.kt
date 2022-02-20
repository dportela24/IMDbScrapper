package com.diogo_portela.imdb_craper.helper

import com.diogo_portela.imdb_craper.model.ApplicationLinkedData
import kotlin.random.Random

fun generateImdbId() = "tt" + Random.nextInt(1000000, 9999999)

fun generateApplicationLinkedData(
    type: String = "TVSeries",
    name: String = "My TV Show",
    alternateName: String = "My Alternate TV Show",
    image: String = "http://myposter.com",
    description: String = "A simple TV Show",
    aggregateRating: ApplicationLinkedData.AggregateRating = generateAggregasteRating(),
    genre: Set<String> = setOf("Comedy", "Drama", "Romance")
) = ApplicationLinkedData(
    type = type,
    name = name,
    alternateName = alternateName,
    image = image,
    description = description,
    aggregateRating = aggregateRating,
    genre = genre
)

fun generateAggregasteRating(
    ratingCount: Int = 1234,
    ratingValue: Float = 8.2F
) = ApplicationLinkedData.AggregateRating(
    ratingCount = ratingCount,
    ratingValue = ratingValue
)