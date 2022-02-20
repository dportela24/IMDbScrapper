package com.diogo_portela.imdb_craper.service

import com.diogo_portela.imdb_craper.helper.generateEpisode
import com.diogo_portela.imdb_craper.helper.generateImdbId
import com.diogo_portela.imdb_craper.helper.generateSeason
import com.diogo_portela.imdb_craper.model.JSoupConnection
import com.diogo_portela.imdb_craper.model.exception.ErrorBuildingEpisodeException
import com.diogo_portela.imdb_craper.model.exception.ErrorBuildingSeasonException
import com.diogo_portela.imdb_craper.model.exception.JSoupConnectionException
import io.mockk.*
import org.jsoup.Connection
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SeasonServiceTest {
    val jSoupConnection = mockk<JSoupConnection>()
    val episodeService = mockk<EpisodeService>()

    val seasonOneConnection = mockk<Connection>()
    val seasonTwoConnection = mockk<Connection>()

    val seasonOneDoc = mockk<Document>()
    val seasonTwoDoc = mockk<Document>()

    val subject = SeasonService(jSoupConnection, episodeService)

    val seasonOneNumberEpisodesElement = mockk<Element>()
    val seasonTwoNumberEpisodesElement = mockk<Element>()

    fun setupMocks() {
        every { jSoupConnection.newConnection(match { it.contains("season=1") }) } returns seasonOneConnection
        every { jSoupConnection.newConnection(match { it.contains("season=2") }) } returns seasonTwoConnection
        every { seasonOneConnection.get() } returns seasonOneDoc
        every { seasonTwoConnection.get() } returns seasonTwoDoc
        every { seasonOneDoc.getElementsByAttributeValue("itemprop", "numberofEpisodes").first() } returns seasonOneNumberEpisodesElement
        every { seasonTwoDoc.getElementsByAttributeValue("itemprop", "numberofEpisodes").first() } returns seasonTwoNumberEpisodesElement
    }

    @Test
    fun `If there is not parsing or validation errors, returns a set of Seasons as expected`(){
        setupMocks()
        val imdbId = generateImdbId()
        val numberSeasons = 2
        val seasonOneNumberEpisodes = 2
        val seasonTwoNumberEpisodes = 3

        val seasonOneEpisodes = (1..seasonOneNumberEpisodes).map { generateEpisode() }.toSet()
        val seasonTwoEpisodes = (1..seasonTwoNumberEpisodes).map { generateEpisode() }.toSet()

        val seasonOne = generateSeason(1, seasonOneNumberEpisodes, seasonOneEpisodes)
        val seasonTwo = generateSeason(2, seasonTwoNumberEpisodes, seasonTwoEpisodes)

        val expectedSeasons = setOf(seasonOne, seasonTwo)

        every { seasonOneNumberEpisodesElement.attr("content") } returns seasonOneNumberEpisodes.toString()
        every { seasonTwoNumberEpisodesElement.attr("content") } returns seasonTwoNumberEpisodes.toString()
        every { episodeService.getEpisodesOfSeason(seasonOneDoc, seasonOneNumberEpisodes) } returns seasonOneEpisodes
        every { episodeService.getEpisodesOfSeason(seasonTwoDoc, seasonTwoNumberEpisodes) } returns seasonTwoEpisodes

        val actualSeasons = subject.getSeasonsOfSeries(imdbId, numberSeasons)

        assertEquals(expectedSeasons, actualSeasons)
        verify(exactly = 1) { jSoupConnection.newConnection(match { it.contains("season=1") }) }
        verify(exactly = 1) { jSoupConnection.newConnection(match { it.contains("season=2") }) }
        verify(exactly = 1) { seasonOneConnection.get() }
        verify(exactly = 1) { seasonTwoConnection.get() }
        verify(exactly = 1) { seasonOneDoc.getElementsByAttributeValue("itemprop", "numberofEpisodes").first() }
        verify(exactly = 1) { seasonTwoDoc.getElementsByAttributeValue("itemprop", "numberofEpisodes").first() }
        verify(exactly = 1) { seasonOneNumberEpisodesElement.attr("content") }
        verify(exactly = 1) { seasonTwoNumberEpisodesElement.attr("content") }
        verify(exactly = 1) { episodeService.getEpisodesOfSeason(seasonOneDoc, seasonOneNumberEpisodes) }
        verify(exactly = 1) { episodeService.getEpisodesOfSeason(seasonTwoDoc, seasonTwoNumberEpisodes) }
        verify(exactly = 1) { seasonOneDoc.equals(seasonTwoDoc) } // call for argmatch in episodeService
        confirmVerified(jSoupConnection, seasonOneConnection, seasonTwoConnection, seasonOneDoc, seasonTwoDoc,
            seasonOneNumberEpisodesElement, seasonTwoNumberEpisodesElement, episodeService)
    }

    @Test
    fun `If fetching the HTML of one seasons fails, it throws JSoupConnectionException and it bubbles up`() {
        setupMocks()
        val imdbId = generateImdbId()
        val numberSeasons = 2
        val expectedErrorMessage = "Could not retrieve Season HTML."
        val connectionErrorMessage = "Could not connect."

        val seasonOneNumberEpisodes = 2
        val seasonOneEpisodes = (1..seasonOneNumberEpisodes).map { generateEpisode() }.toSet()

        every { seasonOneNumberEpisodesElement.attr("content") } returns seasonOneNumberEpisodes.toString()
        every { episodeService.getEpisodesOfSeason(seasonOneDoc, seasonOneNumberEpisodes) } returns seasonOneEpisodes
        every { seasonTwoConnection.get() } throws RuntimeException(connectionErrorMessage)

        val ex = assertThrows<JSoupConnectionException> { subject.getSeasonsOfSeries(imdbId, numberSeasons) }

        assertTrue(ex.message.contains(expectedErrorMessage))
        assertTrue(ex.message.contains(connectionErrorMessage))
    }

    @Test
    fun `If could not find number of episodes for one season, it throws ErrorBuildingSeasonException and it bubbles up`() {
        setupMocks()
        val imdbId = generateImdbId()
        val numberEpisodesText = null
        val expectedExceptionMessage = "Could not find numberEpisodes block"

        val numberSeasons = 2
        val seasonOneNumberEpisodes = 2
        val seasonOneEpisodes = (1..seasonOneNumberEpisodes).map { generateEpisode() }.toSet()

        every { seasonOneNumberEpisodesElement.attr("content") } returns seasonOneNumberEpisodes.toString()
        every { episodeService.getEpisodesOfSeason(seasonOneDoc, seasonOneNumberEpisodes) } returns seasonOneEpisodes
        every { seasonTwoDoc.getElementsByAttributeValue("itemprop", "numberofEpisodes").first() } returns numberEpisodesText

        val ex = assertThrows<ErrorBuildingSeasonException> { subject.getSeasonsOfSeries(imdbId, numberSeasons) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `If could not parse number of episodes for one season, it throws ErrorBuildingSeasonException and it bubbles up`() {
        setupMocks()
        val imdbId = generateImdbId()
        val numberEpisodesText = "Not a valid numberEpisodes"
        val expectedExceptionMessage = "Could not parse numberEpisodes. Input string was $numberEpisodesText."

        val numberSeasons = 2
        val seasonOneNumberEpisodes = 2
        val seasonOneEpisodes = (1..seasonOneNumberEpisodes).map { generateEpisode() }.toSet()

        every { seasonOneNumberEpisodesElement.attr("content") } returns seasonOneNumberEpisodes.toString()
        every { episodeService.getEpisodesOfSeason(seasonOneDoc, seasonOneNumberEpisodes) } returns seasonOneEpisodes
        every { seasonTwoNumberEpisodesElement.attr("content") } returns numberEpisodesText

        val ex = assertThrows<ErrorBuildingSeasonException> { subject.getSeasonsOfSeries(imdbId, numberSeasons) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `If episodesServices for one season throws ErrorBuildingEpisodeException, it bubbles up`(){
        setupMocks()
        val imdbId = generateImdbId()
        val expectedExceptionMessage = "Error building episode."

        val numberSeasons = 2
        val seasonOneNumberEpisodes = 2
        val seasonTwoNumberEpisodes = 3
        val seasonOneEpisodes = (1..seasonOneNumberEpisodes).map { generateEpisode() }.toSet()

        every { seasonOneNumberEpisodesElement.attr("content") } returns seasonOneNumberEpisodes.toString()
        every { seasonTwoNumberEpisodesElement.attr("content") } returns seasonTwoNumberEpisodes.toString()
        every { episodeService.getEpisodesOfSeason(seasonOneDoc, seasonOneNumberEpisodes) } returns seasonOneEpisodes
        every { episodeService.getEpisodesOfSeason(seasonTwoDoc, seasonTwoNumberEpisodes) } throws ErrorBuildingEpisodeException(expectedExceptionMessage)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getSeasonsOfSeries(imdbId, numberSeasons) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }
}