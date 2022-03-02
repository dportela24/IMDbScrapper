package com.diogo_portela.imdb_scraper.service

import com.diogo_portela.imdb_scraper.helper.generateErrorMessage
import com.diogo_portela.imdb_scraper.helper.generateTitleUrl
import com.diogo_portela.imdb_scraper.helper.matchGroupsInRegex
import com.diogo_portela.imdb_scraper.model.ApplicationLinkedData
import com.diogo_portela.imdb_scraper.model.JSoupConnection
import com.diogo_portela.imdb_scraper.model.Series
import com.diogo_portela.imdb_scraper.model.exception.ErrorBuildingSeriesException
import com.diogo_portela.imdb_scraper.model.exception.JSoupConnectionException
import com.diogo_portela.imdb_scraper.model.exception.NotATvSeriesException
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.Duration
import javax.print.Doc

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

        val linkedData = getLinkedData(doc)

        validateTitleType(imdbId, linkedData.type)

        val (name, originalName) = getSeriesName(linkedData)

        val (startYear, endYear) = getRuntime(doc)

        val episodeDuration = getEpisodeDuration(doc)

        val numberSeasons = getNumberSeasons(doc)

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
            ratingCount = linkedData.aggregateRating?.ratingCount,
            ratingValue = linkedData.aggregateRating?.ratingValue,
            posterURL = linkedData.image,
            numberSeasons = seasons.size,
            seasons = seasons
        )
    }

    private fun raiseBuildingError(message: String) : ErrorBuildingSeriesException {
        val errorMessage = "Error while building Series. $message"
        logger.error(errorMessage)
        return ErrorBuildingSeriesException(message)
    }

    private fun fetchSeriesHtml(imdbId: String) : Document {
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

    private fun validateTitleType(imdbId: String, type: String) {
        if (type != "TVSeries") {
            val message = "The imdb id $imdbId given is not a TV Series but a $type"
            logger.warn(message)
            throw NotATvSeriesException(message)
        }
    }

    private fun getLinkedData(doc: Document) : ApplicationLinkedData {
        val linkedDataJson = doc.getElementsByAttributeValueStarting("type", "application/ld+json").first()?.data()
            ?: throw raiseBuildingError(generateErrorMessage("linkedData"))

        return try {
            ApplicationLinkedData.fromJSON(linkedDataJson)
        } catch (ex: Exception) {
            throw raiseBuildingError("An error occurred while deserializing the linkedData element. " + ex.message)
        }
    }

    private fun getSeriesName(linkedData: ApplicationLinkedData) : Pair<String, String?> {
        return linkedData.alternateName?.run {
            Pair(linkedData.alternateName, linkedData.name)
        } ?: run {
            Pair(linkedData.name, null)
        }
    }

    private fun getRuntime(doc: Document) : Pair<Int, Int?> {
        val undertitleElements = doc.getElementsByAttributeValueStarting("data-testid", "hero-title-block__metadata").first()?.children()
            ?: throw raiseBuildingError(generateErrorMessage("underTitle"))

        val runtimeText = undertitleElements[1]?.children()?.last()?.text()
            ?: throw raiseBuildingError(generateErrorMessage("runtime"))

        return parseStartAndEndYear(runtimeText)
    }

    private fun parseStartAndEndYear(input: String?) : Pair<Int, Int?> {
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
            throw raiseBuildingError(generateErrorMessage("runtime", input))
        }
    }

    private fun getEpisodeDuration(doc: Document) : Duration? {
        val episodeDurationElement = doc.getElementsByAttributeValueStarting("data-testid", "title-techspec_runtime").first()
        val episodeDurationText = episodeDurationElement?.child(1)?.text()

        return episodeDurationText?.let { parseDurationFromString(it) }
    }

    private fun parseDurationFromString(input: String?) : Duration {
        val groupValues = try {
            matchGroupsInRegex(input!!.replace(" ",""), "^(?!\$)(?:(\\d+)hours?)?(?:(\\d+)minutes)?")!!
        } catch (_:Exception) {
            throw raiseBuildingError(generateErrorMessage("episodeDuration", input))
        }

        val hours = groupValues[1].toLongOrNull() ?: 0
        val minutes = groupValues[2].toLongOrNull() ?: 0

        return Duration.ofHours(hours).plusMinutes(minutes)
    }

    private fun getNumberSeasons(doc: Document) : Int {
        val numberSeasonsText = doc.getElementsByAttributeValueStarting("for", "browse-episodes-season").first()?.text() // For multiple seasons
            ?: run{
                doc.getElementsByAttributeValueStarting("class", "BrowseEpisodes__BrowseLinksContainer").first() // For single seasons
                    ?.getElementsByAttributeValueContaining("href", "season")?.text()
            }
            ?: throw raiseBuildingError(generateErrorMessage("numberSeasons"))

        return parseNumberSeasons(numberSeasonsText)
    }

    private fun parseNumberSeasons(input: String?) : Int {
        val numberSeasonsGroupValues = try {
            matchGroupsInRegex(input!!.lowercase(), "(\\d+) seasons?")!!
        } catch (_: Exception) {
            throw raiseBuildingError(generateErrorMessage("numberSeasons", input))
        }

        return numberSeasonsGroupValues[1].toInt()
    }
}