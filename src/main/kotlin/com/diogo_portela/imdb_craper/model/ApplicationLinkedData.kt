package com.diogo_portela.imdb_craper.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationLinkedData (
    @JsonProperty("@type")
    val type: String,
    val url: String,
    val name: String,
    val alternateName: String?,
    val image: String,
    val description: String,
    val aggregateRating: AggregateRating,
    val genre: List<String>
){
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AggregateRating(
        val ratingCount: Int,
        val ratingValue: Float
    )
}
