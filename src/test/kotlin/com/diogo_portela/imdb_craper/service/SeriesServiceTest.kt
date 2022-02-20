package com.diogo_portela.imdb_craper.service

import com.diogo_portela.imdb_craper.helper.*
import com.diogo_portela.imdb_craper.model.ApplicationLinkedData
import com.diogo_portela.imdb_craper.model.JSoupConnection
import com.diogo_portela.imdb_craper.model.exception.ErrorBuildingSeasonException
import com.diogo_portela.imdb_craper.model.exception.ErrorBuildingSeriesException
import com.diogo_portela.imdb_craper.model.exception.JSoupConnectionException
import com.diogo_portela.imdb_craper.model.exception.NotATvSeriesException
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

class SeriesServiceTest {
    val jSoupConnection = mockk<JSoupConnection>()
    val seasonService = mockk<SeasonService>()
    val doc = mockk<Document>()

    val subject = SeriesService(jSoupConnection, seasonService)

    val linkedDataElement = mockk<Element>()
    val undertitleElements = mockk<Elements>()
    val numberSeasonsElement = mockk<Element>()

    fun setupMocks() {
        every { jSoupConnection.newConnection(any()).get() } returns doc
        every { doc.getElementsByAttributeValueStarting("type", "application/ld+json").first() } returns linkedDataElement
        every { doc.getElementsByAttributeValueStarting("data-testid", "hero-title-block__metadata").first()?.children() } returns undertitleElements
        every { doc.getElementsByAttributeValueStarting("for", "browse-episodes-season").first() } returns numberSeasonsElement
    }

    @Test
    fun `If there is not parsing or validation errors, returns a Series as expected`() {
        setupMocks()
        val seasonOneEpisodes = setOf(generateEpisode(), generateEpisode())
        val seasonTwoEpisodes = setOf(generateEpisode(), generateEpisode())
        val seasonOne = generateSeason( number = 1, numberEpisodes = seasonOneEpisodes.size, episodes = seasonOneEpisodes)
        val seasonTwo = generateSeason(number = 2, numberEpisodes = seasonTwoEpisodes.size, episodes = seasonTwoEpisodes)
        val seasons = setOf(seasonOne, seasonTwo)
        val numberSeasons = seasons.size

        val imdbId = generateImdbId()
        val linkedData = generateApplicationLinkedData()
        val linkedDataJson = generateApplicationLinkedDataJson(linkedData)
        val runtimeString = "2015–2019"
        val durationString = "52m"
        val numberSeasonsString = "$numberSeasons Seasons"

        every { linkedDataElement.data() } returns linkedDataJson
        every { undertitleElements[1]?.children()?.last()?.text() } returns runtimeString
        every { undertitleElements.last()?.text() } returns durationString
        every { numberSeasonsElement.text() } returns numberSeasonsString
        every { seasonService.getSeasonsOfSeries(imdbId, numberSeasons) } returns seasons

        val expectedSeries = generateSeries(
            imdbId = imdbId, name = linkedData.alternateName!!, originalName = linkedData.name, summary = linkedData.description,
            episodeDuration = Duration.ofMinutes(52), startYear = 2015, endYear = 2019, genres = linkedData.genre,
            ratingValue = linkedData.aggregateRating.ratingValue, ratingCount = linkedData.aggregateRating.ratingCount,
            posterURL = linkedData.image, numberSeasons = numberSeasons, seasons = seasons
        )

        val actualSeries = subject.scrapTitle(imdbId)

        assertEquals(expectedSeries, actualSeries)
        verify(exactly = 1) { jSoupConnection.newConnection(any()).get() }
        verify(exactly = 1) { linkedDataElement.data() }
        verify(exactly = 1) { undertitleElements[1]?.children()?.last()?.text() }
        verify(exactly = 1) { undertitleElements.last()?.text() }
        verify(exactly = 1) { numberSeasonsElement.text() }
        verify(exactly = 1) { seasonService.getSeasonsOfSeries(imdbId, numberSeasons) }
        confirmVerified(jSoupConnection, linkedDataElement, undertitleElements, numberSeasonsElement, seasonService)
    }

    @Test
    fun `seasons - If an error occurs fetching seasons bubbles up exception`() {
        setupMocks()
        val imdbId = generateImdbId()
        val linkedDataJson = generateApplicationLinkedDataJson()
        val runtimeString = "2015–2019"
        val durationString = "52m"
        val numberSeasonsString = "2 Seasons"
        val errorBuildingSeasonMessage = "Could not build season"

        every { linkedDataElement.data() } returns linkedDataJson
        every { undertitleElements[1]?.children()?.last()?.text() } returns runtimeString
        every { undertitleElements.last()?.text() } returns durationString
        every { numberSeasonsElement.text() } returns numberSeasonsString
        every { seasonService.getSeasonsOfSeries(any(), any()) } throws ErrorBuildingSeasonException(errorBuildingSeasonMessage)

        val ex = assertThrows<ErrorBuildingSeasonException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(errorBuildingSeasonMessage))
        verify(exactly = 1) { jSoupConnection.newConnection(any()).get() }
        verify(exactly = 1) { linkedDataElement.data() }
        verify(exactly = 1) { undertitleElements[1]?.children()?.last()?.text() }
        verify(exactly = 1) { undertitleElements.last()?.text() }
        verify(exactly = 1) { numberSeasonsElement.text() }
        verify(exactly = 1) { seasonService.getSeasonsOfSeries(imdbId, any()) }
        confirmVerified(jSoupConnection, linkedDataElement, undertitleElements, numberSeasonsElement, seasonService)
    }

    @Test
    fun `If cannot retrieve HTML throws JSoupConnectionException`() {
       setupMocks()
       val imdb = generateImdbId()
       val expectedExceptionMessage = "Could not retrieve Series HTML. "
       val connectionErrorMessage = "Could not connect."

       every { jSoupConnection.newConnection(any()).get() } throws RuntimeException(connectionErrorMessage)

       val ex = assertThrows<JSoupConnectionException> { subject.fetchSeriesHtml(imdb) }

       assertTrue(ex.message.contains(expectedExceptionMessage))
       assertTrue(ex.message.contains(connectionErrorMessage))
       verify(exactly = 1) { jSoupConnection.newConnection(any()).get() }
       confirmVerified(jSoupConnection, linkedDataElement, undertitleElements, numberSeasonsElement, seasonService)
    }

    @Test
    fun `If title not a TV Series throws NotATVSeriesException`() {
        val imdbId = generateImdbId()
        val titleType = "Movie"

        val ex = assertThrows<NotATvSeriesException> { subject.validateTitleType(imdbId, titleType) }

        assertTrue(ex.message.contains(imdbId))
        assertTrue(ex.message.contains(titleType))
        confirmVerified(jSoupConnection, linkedDataElement, undertitleElements, numberSeasonsElement, seasonService)
    }

    @Test
    fun `linkedData - If correct json is given deserializes it correctly`() {
        val expectedLinkedData = generateApplicationLinkedData()
        val linkedDataJson = generateApplicationLinkedDataJson(expectedLinkedData)

        val actualLinkedData = subject.getLinkedData(linkedDataJson)

        assertEquals(expectedLinkedData, actualLinkedData)
    }

    @Test
    fun `linkedData - If no linkedDataElement is found throws ErrorBuildingSeriesException`() {
        setupMocks()
        val imdbId = generateImdbId()
        val expectedExceptionMessage = "Could not find LinkedData element."

        every { doc.getElementsByAttributeValueStarting("type", "application/ld+json").first() } returns null

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
        verify(exactly = 1) { jSoupConnection.newConnection(any()).get() }
        confirmVerified(jSoupConnection, linkedDataElement, undertitleElements, numberSeasonsElement, seasonService)
    }

    @Test
    fun `linkedData - If linkedDataElement deserialization fails throws ErrorBuildingSeriesException`() {
        val expectedExceptionMessage = "An error occurred while deserializing the linkedData element."
        val linkedDataJson = "Not a valid linkedDataJson"

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.getLinkedData(linkedDataJson) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
        confirmVerified(jSoupConnection, linkedDataElement, undertitleElements, numberSeasonsElement, seasonService)
    }

    @Test
    fun `seriesName - If no alternateName exists, series name is linkedData name with no original name`() {
        val expectedName = "My name"
        val linkedData = generateApplicationLinkedData(name = expectedName, alternateName = null)

        val (actualName, actualOriginalName) = subject.getSeriesName(linkedData)

        assertEquals(expectedName, actualName)
        assertNull(actualOriginalName)
    }

    @Test
    fun `seriesName - If alternateName exists, series name is alternateName and  original name is linkedData name`() {
        val expectedName = "My name"
        val expectedOriginalName = "My original name"
        val linkedData = generateApplicationLinkedData(name = expectedOriginalName, alternateName = expectedName)

        val (actualName, actualOriginalName) = subject.getSeriesName(linkedData)

        assertEquals(expectedName, actualName)
        assertEquals(expectedOriginalName, actualOriginalName)
    }

    @Test
    fun `underTitleElements - If cannot find underTitleElements throws ErrorBuildingSeriesException`() {
        setupMocks()
        val imdbId = generateImdbId()
        val expectedExceptionMessage = "Could not find undertitle elements"

        every { linkedDataElement.data() } returns generateApplicationLinkedDataJson()
        every { doc.getElementsByAttributeValueStarting("data-testid", "hero-title-block__metadata").first()?.children() } returns null

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
        verify(exactly = 1) { jSoupConnection.newConnection(any()).get() }
        verify(exactly = 1) { linkedDataElement.data() }
        confirmVerified(jSoupConnection, linkedDataElement, undertitleElements, numberSeasonsElement, seasonService)
    }

    @Test
    fun `runtimeText - ongoing series - If correct runtimeText is given parses it correctly`() {
        val expectedStartYear = 2014
        val runtimeText = "$expectedStartYear–"

        val (startYear, endYear) = subject.parseStartAndEndYear(runtimeText)

        assertEquals(expectedStartYear, startYear)
        assertNull(endYear)
    }

    @Test
    fun `runtimeText - ended series single year - If correct runtimeText is given parses it correctly`() {
        val expectedYear = 2014
        val runtimeText = expectedYear.toString()

        val (actualStartYear, actualEndYear) = subject.parseStartAndEndYear(runtimeText)

        assertEquals(expectedYear, actualStartYear)
        assertEquals(expectedYear, actualEndYear)
    }

    @Test
    fun `runtimeText - ended series multiple years - If correct runtimeText is given parses it correctly`() {
        val expectedStartYear = 2014
        val expecredEndYear = 2019
        val runtimeText = "$expectedStartYear–$expecredEndYear"

        val (actualStartYear, actualEndYear) = subject.parseStartAndEndYear(runtimeText)

        assertEquals(expectedStartYear, actualStartYear)
        assertEquals(expecredEndYear, actualEndYear)
    }

    @Test
    fun `runtimeText - If cannot find runtimeText throws ErrorBuildingSeriesException`() {
        val runtimeText = null
        val expectedExceptionMessage = "Could not find runtime block"

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.parseStartAndEndYear(runtimeText) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `runtimeText - If cannot parse runtimeText throws ErrorBuildingSeriesException`() {
        val runtimeText = "2014---2019"
        val expectedExceptionMessage = "Could not parse runtime. Input string was $runtimeText"

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.parseStartAndEndYear(runtimeText) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `episodeDuration - only minutes - If correct episodeDurationText is given parses it correctly`() {
        val minutes = 23
        val episodeDurationText = "${minutes}m"
        val expectedDuration = Duration.ofMinutes(minutes.toLong())

        val actualDuration = subject.parseDurationFromString(episodeDurationText)

        assertEquals(expectedDuration, actualDuration)
    }

    @Test
    fun `episodeDuration - only hours - If correct episodeDurationText is given parses it correctly`() {
        val hours = 1
        val episodeDurationText = "${hours}h"
        val expectedDuration = Duration.ofHours(hours.toLong())

        val actualDuration = subject.parseDurationFromString(episodeDurationText)

        assertEquals(expectedDuration, actualDuration)
    }

    @Test
    fun `episodeDuration - hours and minutes - If correct episodeDurationText is given parses it correctly`() {
        val hours = 1
        val minutes = 23
        val episodeDurationText = "${hours}h ${minutes}m"
        val expectedDuration = Duration.ofHours(hours.toLong()).plusMinutes(minutes.toLong())

        val actualDuration = subject.parseDurationFromString(episodeDurationText)

        assertEquals(expectedDuration, actualDuration)
    }

    @Test
    fun `episodeDuration - If could not find episodeDurationText throws ErrorBuildingSeriesException`() {
        val episodeDurationText = null
        val expectedErrorMessage = "Could not find episodeDuration block"

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.parseDurationFromString(episodeDurationText) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `episodeDuration - If cannot parse episodeDurationText throws ErrorBuildingSeriesException`() {
        val episodeDurationText = "Not a valid duration"
        val expectedErrorMessage = "Could not parse episodeDuration. Input string was $episodeDurationText"

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.parseDurationFromString(episodeDurationText) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `numberSeasons - single season - If correct numberSeasonsText is given parses it correctly`() {
        val expectedNumberSeason = 1
        val numberSeasonsText = "$expectedNumberSeason Season"

        val actualNumberSeason = subject.parseNumberSeasons(numberSeasonsText)

        assertEquals(expectedNumberSeason, actualNumberSeason)
    }

    @Test
    fun `numberSeasons - multiple seasons - If correct numberSeasonsText is given parses it correctly`() {
        val expectedNumberSeason = 3
        val numberSeasonsText = "$expectedNumberSeason Seasons"

        val actualNumberSeason = subject.parseNumberSeasons(numberSeasonsText)

        assertEquals(expectedNumberSeason, actualNumberSeason)
    }

    @Test
    fun `numberSeasons - If could not find numberSeasonsText throws ErrorBuildingSeriesException`() {
        val numberSeasonsText = null
        val expectedErrorMessage = "Could not find numberSeasons block"

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.parseNumberSeasons(numberSeasonsText) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `numberSeasons - If cannot parse numberSeasonsText throws ErrorBuildingSeriesException`() {
        val numberSeasonsText = "Not a valid number of seasons"
        val expectedErrorMessage = "Could not parse numberSeasons. Input string was $numberSeasonsText"

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.parseNumberSeasons(numberSeasonsText) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }
}