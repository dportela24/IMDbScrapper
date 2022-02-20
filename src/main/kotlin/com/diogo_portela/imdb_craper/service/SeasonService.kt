package com.diogo_portela.imdb_craper.service

import com.diogo_portela.imdb_craper.model.JSoupConnection
import com.diogo_portela.imdb_craper.model.Season
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.apache.logging.slf4j.MDCContextMap
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class SeasonService(
    val jSoupConnection: JSoupConnection,
    val episodeService: EpisodeService
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    fun getSeasonsOfSeries(imdbId: String, numberSeasons: Int) : Set<Season> {
        logger.info("Processing $numberSeasons seasons")

        val seasons = runBlocking(MDCContext()) {
           (1..numberSeasons).map { seasonNumber ->
                 async(Dispatchers.IO) {
                    buildSeason(imdbId, seasonNumber)
                }
            }.awaitAll()
        }

        logger.info("Processed all $numberSeasons seasons!")

        return seasons.toSet()
    }

    suspend fun buildSeason(imdbId: String, seasonNumber: Int) : Season {
        MDC.put("season", seasonNumber.toString())

        logger.trace("Making request for season")

        val doc = jSoupConnection
            .newConnection("/title/$imdbId/episodes?season=$seasonNumber")
            .get()

        val numberEpisodesText = doc.getElementsByAttributeValue("itemprop", "numberofEpisodes").attr("content")
        val numberEpisodes = numberEpisodesText.toIntOrNull()
            ?: throw RuntimeException("Could not fetch number of episodes")

        val episodes = episodeService.getEpisodesOfSeason(doc, numberEpisodes)

        return Season(
            number = seasonNumber,
            numberEpisodes = numberEpisodes,
            episodes = episodes
        )
    }
}