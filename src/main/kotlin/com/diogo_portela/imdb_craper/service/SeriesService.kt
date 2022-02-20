package com.diogo_portela.imdb_craper.service

import com.diogo_portela.imdb_craper.helper.generateTitleUrl
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
            ?: throw raiseBuildingError("Could not find LinkedData element. ")

        val linkedData = getLinkedData(linkedDataJson)

        validateTitleType(imdbId, linkedData.type)

        val (name, originalName) = getSeriesName(linkedData)

        val undertitleElements = getUndertitleElements(doc)

        val runtimeText = undertitleElements[1]?.children()?.last()?.text()
        val (startYear, endYear) = parseStartAndEndYear(runtimeText)

        val episodeDurationText = undertitleElements.last()?.text()
        val episodeDuration = parseDurationFromString(episodeDurationText)

        val numberSeasonsText = getNumberSeasonsElement(doc)
        val numberSeasons = parseNumberSeasons(numberSeasonsText)

        val seasons = seasonService.getSeasonsOfSeries(imdbId, numberSeasons)

        return Series(
            imdbId = imdbId,
            name = name,
            originalName = originalName,
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

    fun raiseBuildingError(message: String) : ErrorBuildingSeriesException {
        val errorMessage = "Error while building Series. $message"
        logger.error(errorMessage)
        return ErrorBuildingSeriesException(message)
    }

    fun fetchSeriesHtml(imdbId: String) : Document {
        return try {
            logger.trace("Making request for series data")
            jSoupConnection
                .newConnection(generateTitleUrl(imdbId))
                .get()
        } catch (ex: Exception) {
            val errorMessage = "Could not retrieve Series HTML. ${ex.message}"
            logger.error(errorMessage)
            throw JSoupConnectionException(errorMessage)
        }
    }

    fun validateTitleType(imdbId: String, type: String) {
        if (type != "TVSeries") {
            val message = "The imdb id $imdbId given is not a TV Series but a $type"
            logger.warn(message)
            throw NotATvSeriesException(message)
        }
    }

    fun getLinkedData(json: String) : ApplicationLinkedData {
        return try {
            ApplicationLinkedData.fromJSON(json)
        } catch (ex: Exception) {
            throw raiseBuildingError("An error occurred while deserializing the linkedData element. " + ex.message)
        }
    }

    fun getSeriesName(linkedData: ApplicationLinkedData) : Pair<String, String?> {
        return linkedData.alternateName?.run {
            Pair(linkedData.alternateName, linkedData.name)
        } ?: run {
            Pair(linkedData.name, null)
        }
    }

    fun getUndertitleElements(doc: Document) =
        doc.getElementsByAttributeValueStarting("data-testid", "hero-title-block__metadata").first()?.children()
            ?: throw raiseBuildingError("Could not find undertitle elements")

    fun parseStartAndEndYear(input: String?) : Pair<Int, Int?> {
        return try {
            val assertedInput = input!!
            val isSingleYear = Regex("(\\d+)\$").matches(assertedInput)

            if (isSingleYear) {
                val year = assertedInput.toInt()
                Pair(year, year)
            } else {
                val runtimeGroupValues = matchGroupsInRegex(assertedInput, "(\\d+)–?(\\d+)?")!!

                val startYear = runtimeGroupValues[1].toInt()
                val endYear = runtimeGroupValues[2].toIntOrNull()

                Pair(startYear, endYear)
            }
        } catch (_: Exception) {
            throw raiseBuildingError(generateParseErrorMessage("runtime", input))
        }
    }

    fun parseDurationFromString(input: String?) : Duration {
        val groupValues = try {
            matchGroupsInRegex(input!!.replace(" ",""), "(?:(\\d+)h)?(?:(\\d+)m)?")!!
        } catch (_:Exception) {
            throw raiseBuildingError(generateParseErrorMessage("episodeDuration", input))
        }

        val hours = groupValues[1].toLongOrNull() ?: 0
        val minutes = groupValues[2].toLongOrNull() ?: 0

        return Duration.ofHours(hours).plusMinutes(minutes)
    }

    fun getNumberSeasonsElement(doc: Document) =
        doc.getElementsByAttributeValueStarting("for", "browse-episodes-season").first()?.text()
            ?: run{
                doc.getElementsByAttributeValueStarting("class", "BrowseEpisodes__BrowseLinksContainer").first()?.child(1)?.text()
            }

    fun parseNumberSeasons(input: String?) : Int {
        val numberSeasonsGroupValues = try {
            matchGroupsInRegex(input!!.lowercase(), "(\\d+) seasons?")!!
        } catch (_: Exception) {
            throw raiseBuildingError(generateParseErrorMessage("numberSeasons", input))
        }

        return numberSeasonsGroupValues[1].toInt()
    }

    fun generateParseErrorMessage(field: String, input: String?) : String {
        return if (input.isNullOrBlank())
            "Could not find $field block"
        else
            "Could not parse $field. Input string was $input. "
    }
}