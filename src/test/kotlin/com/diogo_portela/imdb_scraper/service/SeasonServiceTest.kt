package com.diogo_portela.imdb_scraper.service

import com.diogo_portela.imdb_scraper.helper.generateImdbId
import com.diogo_portela.imdb_scraper.helper.generateSeason
import com.diogo_portela.imdb_scraper.helper.generateSeasonScrappedData
import com.diogo_portela.imdb_scraper.model.Episode
import com.diogo_portela.imdb_scraper.model.JSoupConnection
import com.diogo_portela.imdb_scraper.model.Season
import com.diogo_portela.imdb_scraper.model.SeasonScrappedData
import com.diogo_portela.imdb_scraper.model.exception.ErrorBuildingEpisodeException
import com.diogo_portela.imdb_scraper.model.exception.ErrorBuildingSeasonException
import com.diogo_portela.imdb_scraper.model.exception.JSoupConnectionException
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jsoup.Connection
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.system.measureTimeMillis

class SeasonServiceTest {
    val jSoupConnection = mockk<JSoupConnection>()
    val episodeService = mockk<EpisodeService>()

    val seasonsConnections = mutableListOf<Connection>()
    val seasonDocs = mutableListOf<Document>()
    val seasonNumberEpisodesElements = Elements()

    val subject = SeasonService(jSoupConnection, episodeService)

    fun setupMocks(seasonsElementData: List<SeasonScrappedData>, seasonsEpisodes: List<Collection<Episode>>) {
        (seasonsElementData.indices).forEach{ i ->
            val elementData = seasonsElementData[i]
            val episodes = seasonsEpisodes[i]
            val seasonConnection = mockk<Connection>().also { seasonsConnections.add(it) }
            val seasonDoc = mockk<Document>().also { seasonDocs.add(it) }
            val seasonNumberEpisodesElement = mockk<Element>().also { seasonNumberEpisodesElements.add(it) }

            every { jSoupConnection.newConnection(match { it.contains("season=${i+1}") }) } returns seasonConnection
            every { seasonConnection.get() } returns seasonDoc
            every { seasonDoc.getElementsByAttributeValue("itemprop", "numberofEpisodes").first() } returns seasonNumberEpisodesElement

            every { seasonNumberEpisodesElement.attr("content") } returns elementData.numberEpisodes!!
            every { episodeService.getEpisodesOfSeason(seasonDoc, any()) } returns episodes.toSet()
        }
    }

    fun verifyMocks() {
        (seasonsConnections.indices).forEach{ i ->
            verify(exactly = 1) { jSoupConnection.newConnection(match { it.contains("season=${i+1}") }) }
            verify(exactly = 1) { seasonsConnections[i].get() }
            verify(exactly = 1) { seasonDocs[i].getElementsByAttributeValue("itemprop", "numberofEpisodes").first() }
            verify(exactly = 1) { seasonNumberEpisodesElements[i].attr("content")}
            verify(exactly = 1) { episodeService.getEpisodesOfSeason(seasonDocs[i], any()) }
            excludeRecords { seasonDocs[i].equals(any()) } // Ignore calls because of stubbing episodeService
            confirmVerified(seasonsConnections[i], seasonDocs[i], seasonNumberEpisodesElements[i] )
        }
        confirmVerified(jSoupConnection, episodeService)
    }

    fun setupSeasons(numberSeasons: Int) : Pair<List<SeasonScrappedData>, List<Season>> {
        val seasonsData = mutableListOf<SeasonScrappedData>()
        val seasons = mutableListOf<Season>()

        (1..numberSeasons).map{ i ->
            val elementData = generateSeasonScrappedData()
            val season = generateSeason(elementData, i)
            seasonsData.add(elementData)
            seasons.add(season)
        }

        return Pair(seasonsData, seasons)
    }

    @Test
    fun `Happy flow - returns a set of Seasons as expected`(){
        val numberSeasons = 2
        val (seasonsElementData, expectedSeasons) = setupSeasons(numberSeasons)

        setupMocks(seasonsElementData, expectedSeasons.map { it.episodes })

        val actualSeasons = subject.getSeasonsOfSeries(generateImdbId(), expectedSeasons.size)

        assertEquals(expectedSeasons.toSet(), actualSeasons)
        verifyMocks()
    }

   @Test
   fun `Parallelism - Seasons are processed in parallel in different coroutines`(){
       val subjectSpy = spyk(subject)
       val numberSeasons = 2
       val millisPerSeason = 500L

       coEvery { subjectSpy.buildSeason(any(), any()) } answers {
           runBlocking {
               delay(millisPerSeason)
               generateSeason()
           }
       }

       val computingTime = measureTimeMillis {
           subjectSpy.getSeasonsOfSeries(generateImdbId(), numberSeasons)
       }

       assert(computingTime < millisPerSeason + 100) // 100 ms of overhead computation
   }

    @Test
    fun `Connection - If fetching the HTML of one seasons fails, it throws JSoupConnectionException`() {
        val numberSeasons = 2
        val seasonWithErrorNumber = 2
        val (seasonsElementData, seasons) = setupSeasons(numberSeasons)
        val expectedErrorMessage = "Could not retrieve Season HTML."
        val connectionErrorMessage = "Could not connect."

        setupMocks(seasonsElementData, seasons.map { it.episodes })
        every { seasonsConnections[seasonWithErrorNumber - 1].get() } throws RuntimeException(connectionErrorMessage)

        val ex = assertThrows<JSoupConnectionException> { subject.getSeasonsOfSeries(generateImdbId(), numberSeasons) }

        assertTrue(ex.message.contains(expectedErrorMessage))
        assertTrue(ex.message.contains(connectionErrorMessage))
    }

    @Test
    fun `NumberEpisodes - If number of episodes could not be found for one season, it throws ErrorBuildingSeasonException`() {
        val numberSeasons = 2
        val seasonWithErrorNumber = 2
        val (seasonsElementData, seasons) = setupSeasons(numberSeasons)
        val expectedExceptionMessage = "Could not find numberEpisodes text element"

        setupMocks(seasonsElementData, seasons.map { it.episodes })
        every { seasonDocs[seasonWithErrorNumber - 1].getElementsByAttributeValue("itemprop", "numberofEpisodes").first() } returns null

        val ex = assertThrows<ErrorBuildingSeasonException> { subject.getSeasonsOfSeries(generateImdbId(), numberSeasons) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `NumberEpisodes - If number of episodes is blank for one season, it throws ErrorBuildingSeasonException`() {
        val numberSeasons = 2
        val seasonWithErrorNumber = 2
        val (seasonsElementData, seasons) = setupSeasons(numberSeasons)
        val expectedExceptionMessage = "Could not parse numberEpisodes. Input string was "

        seasonsElementData[seasonWithErrorNumber - 1].numberEpisodes = ""
        setupMocks(seasonsElementData, seasons.map { it.episodes })

        val ex = assertThrows<ErrorBuildingSeasonException> { subject.getSeasonsOfSeries(generateImdbId(), numberSeasons) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `NumberEpisodes - If number of episodes could not be parsed for one season, it throws ErrorBuildingSeasonException`() {
        val numberSeasons = 2
        val seasonWithErrorNumber = 2
        val (seasonsElementData, seasons) = setupSeasons(numberSeasons)
        val numberEpisodesText = "Not a valid numberEpisodes"
        val expectedExceptionMessage = "Could not parse numberEpisodes. Input string was $numberEpisodesText."

        seasonsElementData[seasonWithErrorNumber - 1].numberEpisodes = numberEpisodesText
        setupMocks(seasonsElementData, seasons.map { it.episodes })

        val ex = assertThrows<ErrorBuildingSeasonException> { subject.getSeasonsOfSeries(generateImdbId(), numberSeasons) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `Episodes - If episodesServices for one season throws ErrorBuildingEpisodeException`(){
        val numberSeasons = 2
        val seasonWithErrorNumber = 2
        val (seasonsElementData, seasons) = setupSeasons(numberSeasons)
        val expectedExceptionMessage = "Error building episode."

        setupMocks(seasonsElementData, seasons.map { it.episodes })
        every { episodeService.getEpisodesOfSeason(seasonDocs[seasonWithErrorNumber - 1], any()) } throws ErrorBuildingEpisodeException(expectedExceptionMessage)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getSeasonsOfSeries(generateImdbId(), numberSeasons) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }
}