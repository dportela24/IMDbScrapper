package com.diogo_portela.imdb_craper.service

import com.diogo_portela.imdb_craper.helper.matchGroupsInRegex
import com.diogo_portela.imdb_craper.model.ApplicationLinkedData
import com.diogo_portela.imdb_craper.model.JSoupConnection
import com.diogo_portela.imdb_craper.model.Series
import com.diogo_portela.imdb_craper.model.exception.ErrorBuildingSeriesException
import com.diogo_portela.imdb_craper.model.exception.JSoupConnectionException
import com.diogo_portela.imdb_craper.model.exception.NotATvSeriesException
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

        return buildSeries(imdbId)
    }

    fun buildSeries(imdbId: String) : Series {
        logger.info("Building series")

        val doc = fetchSeriesHtml(imdbId)

        val linkedDataJson = doc.getElementsByAttributeValueStarting("type", "application/ld+json").first()?.data()
            ?: throw ErrorBuildingSeriesException(imdbId, "Could not find LinkedData element")

        val linkedData : ApplicationLinkedData =
            try {
                linkedDataJson?.run{ jacksonObjectMapper().readValue(linkedDataJson) }
            } catch (ex: Exception) {
                throw ErrorBuildingSeriesException(imdbId, "An error ocurred while parsing the linkedData element. " + ex.message)
            }

        if (linkedData.type != "TVSeries")
            throw NotATvSeriesException("The imdb id $imdbId given is not a TV Series but a ${linkedData.type}")

        val undertitleElements = getUndertitleElements(doc)
            ?: throw ErrorBuildingSeriesException(imdbId, "Could not find undertitle elements")

        val runtimeText = undertitleElements[1].let { it.children().last()?.text() }
            ?: throw ErrorBuildingSeriesException(imdbId, "Could not find runtime text element")


        val episodeDurationText = undertitleElements.last()?.text()
            ?: throw ErrorBuildingSeriesException(imdbId, "Could not find episode duration text element")

        val numberSeasonsText = getNumberSeasonsElement(doc)
            ?: throw ErrorBuildingSeriesException(imdbId, "Could not find number of seasons element")

        val (startYear, endYear) = parseStartAndEndYear(runtimeText)
        val episodeDuration = parseDurationFromString(episodeDurationText)
        val numberSeasons = parseNumberSeasons(numberSeasonsText)


        val seasons = seasonService.getSeasonsOfSeries(imdbId, numberSeasons)

        return Series(
            imdbId = imdbId,
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

    private fun fetchSeriesHtml(imdbId: String) : Document {
        return try {
            logger.info("Making request for series data")
            jSoupConnection
                .newConnection(generateTitleUrl(imdbId))
                .get()
        } catch (ex: Exception) {
            throw JSoupConnectionException("Could not retrieve Series HTML. " + ex.message)
        }
    }

    private fun generateTitleUrl(imdbId: String) = "/title/$imdbId"

    private fun getNumberSeasonsElement(doc: Document) =
        doc.getElementsByAttributeValueStarting("for", "browse-episodes-season").first()?.text()
            ?: run{
                doc.getElementsByAttributeValueStarting("class", "BrowseEpisodes__BrowseLinksContainer").first()?.child(1)?.text()
            }

    private fun getUndertitleElements(doc: Document) =
        doc.getElementsByAttributeValueStarting("data-testid", "hero-title-block__metadata").first()?.children()

    private fun parseNumberSeasons(seasonsText: String) : Int {
        val numberSeasonsGroupValues = matchGroupsInRegex(seasonsText.lowercase(), "(\\d+) seasons?")
            ?: throw ErrorBuildingSeriesException(message = "Could not parse number of seasons. Input string was $seasonsText")

        return numberSeasonsGroupValues[1].toInt()
    }

    private fun parseStartAndEndYear(runtimeText: String) : Pair<Int, Int?> {
        val isSingleYear = Regex("(\\d+)\$").matches(runtimeText)

        return if (isSingleYear) {
            val year = runtimeText.toInt()
            Pair(year, year)
        } else {
            val runtimeGroupValues = matchGroupsInRegex(runtimeText, "(\\d+)–?(\\d+)?")
                ?: throw ErrorBuildingSeriesException(message = "Could not parse runtime. Input string was $runtimeText")

            val startYear = runtimeGroupValues[1].toInt()
            val endYear = runtimeGroupValues[2].toIntOrNull()

            Pair(startYear, endYear)
        }
    }

    private fun parseDurationFromString(input: String) : Duration {
        val groupValues = matchGroupsInRegex(input.replace(" ",""), "(?:(\\d+)h)?(?:(\\d+)m)?")
            ?: throw ErrorBuildingSeriesException(message = "Could not parse time duration. Input string was $input.")

        val hours = groupValues[1].toLongOrNull() ?: 0
        val minutes = groupValues[2].toLongOrNull() ?: 0

        return Duration.ofHours(hours).plusMinutes(minutes)
    }
}