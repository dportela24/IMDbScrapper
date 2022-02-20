package com.diogo_portela.imdb_craper.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationLinkedData (
    @JsonProperty("@type")
    val type: String,
    val name: String,
    val alternateName: String?,
    val image: String,
    val description: String,
    val aggregateRating: AggregateRating,
    val genre: Set<String>
){
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AggregateRating(
        val ratingCount: Int,
        val ratingValue: Float
    )

    companion object {
        fun fromJSON(json: String) : ApplicationLinkedData =
                jacksonObjectMapper().readValue(json)
    }
}
