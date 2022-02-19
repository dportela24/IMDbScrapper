package com.diogo_portela.imdb_craper.service

import com.diogo_portela.imdb_craper.helper.matchGroupsInRegex
import com.diogo_portela.imdb_craper.model.ApplicationLinkedData
import com.diogo_portela.imdb_craper.model.JSoupConnection
import com.diogo_portela.imdb_craper.model.Series
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class SeriesService(
    val jSoupConnection: JSoupConnection,
    val seasonService: SeasonService
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    fun scrapTitle(imdbId: String) : Series {
        MDC.put("imdb_id", imdbId)
        logger.info("Processing series $imdbId")

        return buildSeries(imdbId)
    }

    fun buildSeries(imdbId: String) : Series {
        logger.info("Building series")

        logger.info("Making request for series data")
        val doc = jSoupConnection
            .newConnection("/title/$imdbId")
            .get()

        val linkedDataJson = doc.getElementsByAttributeValueStarting("type", "application/ld+json").first()?.data()
        val linkedData : ApplicationLinkedData = linkedDataJson?.run{ jacksonObjectMapper().readValue(linkedDataJson) }
            ?: throw RuntimeException("Could not fetch application linked data")

        if (linkedData.type != "TVSeries") throw RuntimeException("Not a TV Show!")

        val undertitleElements = getUndertitleElements(doc)

        val runtimeText = undertitleElements[1].let { it.children().last()?.text() }
            ?: throw RuntimeException("Could not fetch runtime text")
        val (startYear, endYear) = parseStartAndEndYear(runtimeText)

        val episodeDurationText = undertitleElements.last()?.text()
            ?: throw RuntimeException("Could not fetch episode duration text")
        val episodeDuration = parseDurationFromString(episodeDurationText)

        val numberSeasonsText = getNumberSeasonsElement(doc)
        val numberSeasons = parseNumberSeasons(numberSeasonsText)

        val seasons = seasonService.getSeasonsOfSeries(imdbId, numberSeasons)

        return Series(
            imdbId = parseImdbId(linkedData.url),
            name = linkedData.alternateName ?: linkedData.name,
            originalName = linkedData.alternateName?.run { linkedData.name },
            summary = linkedData.description,
            startYear = startYear,
            endYear = endYear,
            episodeDuration = episodeDuration,
            genres = linkedData.genre.toSet(),
            ratingCount = linkedData.aggregateRating.ratingCount,
            ratingValue = linkedData.aggregateRating.ratingValue,
            posterURL = linkedData.image,
            numberSeasons = numberSeasons,
            seasons = seasons
        )
    }


    private fun getNumberSeasonsElement(doc: Document) =
        doc.getElementsByAttributeValueStarting("for", "browse-episodes-season").first()?.text()
            ?: run{
                doc.getElementsByAttributeValueStarting("class", "BrowseEpisodes__BrowseLinksContainer").first()?.child(1)?.text()
            }
            ?: throw RuntimeException("Could not fetch number of seasons element")

    private fun getUndertitleElements(doc: Document) =
        doc.getElementsByAttributeValueStarting("data-testid", "hero-title-block__metadata").first()?.children()
            ?: throw RuntimeException("Could not fetch undertitle elements")

    private fun parseNumberSeasons(seasonsText: String) : Int {
        val numberSeasonsGroupValues = matchGroupsInRegex(seasonsText.lowercase(), "(\\d+) seasons?")
            ?: throw RuntimeException("Could not parse number of seasons. Input string was $seasonsText")

        return numberSeasonsGroupValues[1].toInt()
    }

    private fun parseImdbId(url: String) : String {
        val idGroupValues = matchGroupsInRegex(url, "/title/([a-zA-Z0-9]+)/")
            ?: throw RuntimeException("Could not parse imdb id. Input string was $url")

        return idGroupValues[1]
    }

    private fun parseStartAndEndYear(runtimeText: String) : Pair<Int, Int?> {
        val isSingleYear = Regex("(\\d+)\$").matches(runtimeText)

        return if (isSingleYear) {
            val year = runtimeText.toInt()
            Pair(year, year)
        } else {
            val runtimeGroupValues = matchGroupsInRegex(runtimeText, "(\\d+)–?(\\d+)?")
                ?: throw RuntimeException("Could not parse runtime. Input string was $runtimeText")

            val startYear = runtimeGroupValues[1].toInt()
            val endYear = runtimeGroupValues[2].toIntOrNull()

            Pair(startYear, endYear)
        }
    }

    private fun parseDurationFromString(input: String) : Duration {
        val groupValues = matchGroupsInRegex(input.replace(" ",""), "(?:(\\d+)h)?(?:(\\d+)m)?")
            ?: throw java.lang.RuntimeException("Could not parse time duration. Input string was $input.")

        val hours = groupValues[1].toLongOrNull() ?: 0
        val minutes = groupValues[2].toLongOrNull() ?: 0

        return Duration.ofHours(hours).plusMinutes(minutes)
    }

//    private fun validateTitleType(doc: Document) {
//        val type = doc.selectFirst("meta[property=og:type]")?.attr("content")
//        if (type.isNullOrEmpty() || type != TV_SERIES_METADATA_TYPE) throw java.lang.RuntimeException("Not a TV Show!")
//    }
}