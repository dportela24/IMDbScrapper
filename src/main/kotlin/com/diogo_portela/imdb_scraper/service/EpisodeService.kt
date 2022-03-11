package com.diogo_portela.imdb_scraper.service

import com.diogo_portela.imdb_scraper.helper.generateErrorMessage
import com.diogo_portela.imdb_scraper.helper.getParseDateFunctions
import com.diogo_portela.imdb_scraper.helper.matchGroupsInRegex
import com.diogo_portela.imdb_scraper.model.Episode
import com.diogo_portela.imdb_scraper.model.exception.EpisodeScrappingErrorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.temporal.TemporalAccessor

@Service
class EpisodeService{
    val logger = LoggerFactory.getLogger(this::class.java)

    fun getEpisodesOfSeason(doc: Document, numberEpisodes: Int) : Set<Episode> {
        logger.info("Processing episodes")

        val episodesList = doc.getElementsByClass("list detail eplist").first()
            ?: throw raiseBuildingError(generateErrorMessage("episodeList"))

        val episodes = runBlocking(MDCContext()) {
            val jobs = episodesList.children().map { element ->
                async(Dispatchers.IO) {
                    buildEpisode(element)
                }
            }
            try {
                jobs.awaitAll()
            } catch (ex: Exception) {
                jobs.forEach{ it.cancel() }
                throw ex
            }
        }.filterNotNull()

        logger.info("Processed $numberEpisodes episodes")

        return episodes.toSet()
    }

    suspend fun buildEpisode(element: Element) : Episode? {
        // Must exist fields
        val episodeNumber = getEpisodeNumber(element)

        MDC.put("episode", episodeNumber.toString())
        logger.trace("Building episode")

        val nameAndUrlElement = element.getElementsByAttributeValue("itemprop", "name").first()
            ?: throw raiseBuildingError(generateErrorMessage("nameAndUrl"))

        val name = nameAndUrlElement.text()
        if (name.isBlank()) throw raiseBuildingError("Episode name text was blank")

        val imdbId = getImdbId(nameAndUrlElement)

        // Can be null fields
        val airdate = getAirdate(element)

        val (ratingValue, ratingCount) = getRatingData(element)

        val summary = getSummary(element)

        return Episode(
            name = name,
            number = episodeNumber,
            imdbId = imdbId,
            airdate = airdate,
            ratingValue = ratingValue,
            ratingCount = ratingCount,
            summary = summary
        )
    }

    private fun getEpisodeNumber(element: Element) : Int {
        val episodeNumberText = element.getElementsByAttributeValue("itemprop", "episodeNumber").first()?.attr("content")
            ?: throw raiseBuildingError(generateErrorMessage("episodeNumber"))
        return episodeNumberText.toIntOrNull()
            ?: throw raiseBuildingError(generateErrorMessage("episodeNumber", episodeNumberText))
    }

    private fun parseRatingCount(input: String) : Int? =
        input.removePrefix("(")
            .removeSuffix(")")
            .replace(",", "")
            .toIntOrNull()

    private fun getImdbId(element: Element) : String {
        val url = element.attr("href")

        val idGroupValues = matchGroupsInRegex(url, "/title/([a-zA-Z0-9]+)/(?:.+)?")
            ?: throw raiseBuildingError(generateErrorMessage("episodeImdbId", url))

        return idGroupValues[1]
    }

    private fun getAirdate(element: Element) : TemporalAccessor? {
        val airdateText = element.getElementsByClass("airdate").first()?.text()
            ?: throw raiseBuildingError(generateErrorMessage("airdate"))

        return if (airdateText.isNotBlank()) parseAirdate(airdateText) else null
    }

    private fun parseAirdate(input: String) : TemporalAccessor {
        val dateFormats = getParseDateFunctions()

        dateFormats.forEach{ parseFunction ->
            try {
                return parseFunction(input)
            } catch (_: Exception) {}
        }

        throw raiseBuildingError(generateErrorMessage("airdate", input))
    }

    private fun getRatingData(element: Element) : Pair<Float?, Int?> {
        val ratingValueText = element.getElementsByClass("ipl-rating-star__rating").first()?.text()
        val ratingCountText = element.getElementsByClass("ipl-rating-star__total-votes").first()?.text()

        val ratingValue = ratingValueText?.toFloatOrNull()
        val ratingCount = ratingCountText?.let { parseRatingCount(it) }

        if (ratingValue == null && ratingCount != null) {
            throw raiseBuildingError("Rating count exists but no rating value")
        } else if (ratingCount == null && ratingValue != null) {
            throw raiseBuildingError("Rating value exists but no rating count")
        }

        return Pair(ratingValue, ratingCount)
    }

    private fun getSummary(element: Element) : String? {
        val summaryElement = element.getElementsByClass("item_description").first()
            ?: throw raiseBuildingError(generateErrorMessage("summaryText"))

        // If links in summary -> No real summary only default IMDb text to submit one
        return if (summaryElement.getElementsByTag("a").size == 0) {
            summaryElement.text()
        } else {
            null
        }
    }

    private fun raiseBuildingError(message: String) : EpisodeScrappingErrorException {
        val errorMessage = "Error while building Episode. $message"
        logger.error(errorMessage)
        return EpisodeScrappingErrorException(message)
    }
}