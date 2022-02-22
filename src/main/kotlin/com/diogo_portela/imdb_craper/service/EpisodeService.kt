package com.diogo_portela.imdb_craper.service

import com.diogo_portela.imdb_craper.helper.generateParseErrorMessage
import com.diogo_portela.imdb_craper.helper.matchGroupsInRegex
import com.diogo_portela.imdb_craper.model.Episode
import com.diogo_portela.imdb_craper.model.exception.ErrorBuildingEpisodeException
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class EpisodeService{
    val logger = LoggerFactory.getLogger(this::class.java)

    fun getEpisodesOfSeason(doc: Document, numberEpisodes: Int) : Set<Episode> {
        logger.info("Processing episodes")

        val episodesList = doc.getElementsByClass("list detail eplist").first()
            ?: throw raiseBuildingError("Could not find episode list")

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
        }

        logger.info("Processed $numberEpisodes episodes")

        return episodes.toSet()
    }

    private fun buildEpisode(element: Element) : Episode {
        val episodeNumberText = element.getElementsByAttributeValue("itemprop", "episodeNumber").first()?.attr("content")
        val episodeNumber = episodeNumberText?.toIntOrNull()
            ?: throw raiseBuildingError("Could not find episode number")

        MDC.put("episode", episodeNumber.toString())
        logger.trace("Building episode")

        val nameAndUrlElement = element.getElementsByAttributeValue("itemprop", "name").first()
            ?: throw raiseBuildingError("Could not find episode name and url element")

        val name = nameAndUrlElement.text()
        if (!validateNonParsedField(name)) throw raiseBuildingError("Episode name text was blank")

        val url = nameAndUrlElement.attr("href")
        val imdbId = parseImdbId(url)

        val airdateText = element.getElementsByClass("airdate").first()?.text()
        val airdate = parseAirdate(airdateText)

        val (ratingValue, ratingCount) = getRatingData(element)

        val summaryText = element.getElementsByClass("item_description").first()
            ?: throw raiseBuildingError("Could not find summary text element")
        val summary = summaryText.text()
        if (!validateNonParsedField(summary)) throw raiseBuildingError("Episode summary text was blank")

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

    private fun validateNonParsedField(field: String) = field.isNotBlank()

    private fun getRatingData(element: Element) : Pair<Float, Int> {
        val ratingValueText = element.getElementsByClass("ipl-rating-star__rating").first()?.text()
        val ratingCountText = element.getElementsByClass("ipl-rating-star__total-votes").first()?.text()

        val ratingValue = ratingValueText?.toFloatOrNull()
            ?: throw raiseBuildingError(generateParseErrorMessage("ratingValue", ratingValueText))

        val ratingCount = parseRatingCount(ratingCountText)
            ?: throw raiseBuildingError(generateParseErrorMessage("ratingCount", ratingCountText))

        return Pair(ratingValue, ratingCount)
    }

    private fun parseRatingCount(input: String?) : Int? =
        input?.removePrefix("(")
            ?.removeSuffix(")")
            ?.replace(",", "")
            ?.toIntOrNull()


    private fun parseAirdate(input: String?) : LocalDate {
        val dateFormats = setOf("d MMM. yyyy", "d MMM yyyy")

        dateFormats.forEach{ dateFormat ->
            try {
                val formatter = DateTimeFormatter.ofPattern(dateFormat)
                return LocalDate.parse(input, formatter)

            } catch (_: Exception) {}
        }

        throw raiseBuildingError(generateParseErrorMessage("airdate", input))
    }

    private fun parseImdbId(url: String) : String {
        val idGroupValues = matchGroupsInRegex(url, "/title/([a-zA-Z0-9]+)/(?:.+)?")
            ?: throw raiseBuildingError(generateParseErrorMessage("episodeImdbId", url))

        return idGroupValues[1]
    }

    private fun raiseBuildingError(message: String) : ErrorBuildingEpisodeException {
        val errorMessage = "Error while building Episode. $message"
        logger.error(errorMessage)
        return ErrorBuildingEpisodeException(message)
    }
}