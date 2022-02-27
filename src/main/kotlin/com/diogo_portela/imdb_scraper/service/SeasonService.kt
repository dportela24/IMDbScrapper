package com.diogo_portela.imdb_scraper.service

import com.diogo_portela.imdb_scraper.helper.generateErrorMessage
import com.diogo_portela.imdb_scraper.helper.generateSeasonUrl
import com.diogo_portela.imdb_scraper.model.JSoupConnection
import com.diogo_portela.imdb_scraper.model.Season
import com.diogo_portela.imdb_scraper.model.exception.ErrorBuildingSeasonException
import com.diogo_portela.imdb_scraper.model.exception.JSoupConnectionException
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service

@Service
class SeasonService(
    val jSoupConnection: JSoupConnection,
    val episodeService: EpisodeService
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    fun getSeasonsOfSeries(imdbId: String, numberSeasons: Int) : Set<Season> {
        logger.info("Processing $numberSeasons seasons")

        val seasons = runBlocking(MDCContext()) {
           val jobs = (1..numberSeasons).map { seasonNumber ->
                 async(Dispatchers.IO) {
                    buildSeason(imdbId, seasonNumber)
                }
            }
            try {
                jobs.awaitAll()
            } catch (ex: Exception) {
                jobs.forEach{ it.cancel() }
                throw ex
            }
        }.filterNotNull()

        logger.info("Processed all $numberSeasons seasons!")

        return seasons.toSet()
    }

    suspend fun buildSeason(imdbId: String, seasonNumber: Int) : Season? {
        MDC.put("season", seasonNumber.toString())

        val doc = fetchSeasonHtml(imdbId, seasonNumber)

        val numberEpisodesText = doc.getElementsByAttributeValue("itemprop", "numberofEpisodes").first()?.attr("content")
        val numberEpisodes = numberEpisodesText?.toIntOrNull()
            ?: throw raiseBuildingError(generateErrorMessage("numberEpisodes", numberEpisodesText))

        val episodes = episodeService.getEpisodesOfSeason(doc, numberEpisodes)

        return Season(
            number = seasonNumber,
            numberEpisodes = episodes.size,
            episodes = episodes
        )
    }

    private fun fetchSeasonHtml(imdbId: String, seasonNumber: Int) : Document {
        return try {
            logger.trace("Making request for season")
            jSoupConnection
                .newConnection(generateSeasonUrl(imdbId, seasonNumber))
                .get()
        } catch (ex: Exception) {
            val errorMessage = "Could not retrieve Season HTML. ${ex.message}"
            logger.error(errorMessage)
            throw JSoupConnectionException(errorMessage)
        }
    }

    private fun raiseBuildingError(message: String) : ErrorBuildingSeasonException {
        val errorMessage = "Error while building Season. $message"
        logger.error(errorMessage)
        return ErrorBuildingSeasonException(errorMessage)
    }
}