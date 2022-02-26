package com.diogo_portela.imdb_scraper.service

import com.diogo_portela.imdb_scraper.helper.*
import com.diogo_portela.imdb_scraper.model.JSoupConnection
import com.diogo_portela.imdb_scraper.model.Season
import com.diogo_portela.imdb_scraper.model.SeriesScrappedData
import com.diogo_portela.imdb_scraper.model.exception.*
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

    fun setupMocks(seriesData: SeriesScrappedData, seasons: Set<Season> = emptySet()) {
        every { jSoupConnection.newConnection(any()).get() } returns doc
        every { doc.getElementsByAttributeValueStarting("type", "application/ld+json").first() } returns linkedDataElement
        every { doc.getElementsByAttributeValueStarting("data-testid", "hero-title-block__metadata").first()?.children() } returns undertitleElements

        every { linkedDataElement.data() } returns seriesData.linkedData!!
        every { undertitleElements[1]?.children()?.last()?.text() } returns seriesData.runtime
        every { undertitleElements.last()?.text() } returns seriesData.episodeDuration
        every { seasonService.getSeasonsOfSeries(any(), seasons.size) } returns seasons

        if (seriesData.numberSeasons == "1 Season") {
            every { doc.getElementsByAttributeValueStarting("class", "BrowseEpisodes__BrowseLinksContainer").first() } returns numberSeasonsElement
            every { doc.getElementsByAttributeValueStarting("for", "browse-episodes-season").first() } returns null
            every { numberSeasonsElement.child(1).text() } returns seriesData.numberSeasons!!
        } else {
            every { doc.getElementsByAttributeValueStarting("for", "browse-episodes-season").first() } returns numberSeasonsElement
            every { doc.getElementsByAttributeValueStarting("class", "BrowseEpisodes__BrowseLinksContainer").first() } returns null
            every { numberSeasonsElement.text() } returns seriesData.numberSeasons!!
        }
    }

    fun verifyMocks() {
        verify(exactly = 1) { jSoupConnection.newConnection(any()).get() }
        verify(exactly = 1) { linkedDataElement.data() }
        verify(exactly = 1) { undertitleElements[1]?.children()?.last()?.text() }
        verify(exactly = 1) { undertitleElements.last()?.text() }
        verify(atLeast = 1) { seasonService.getSeasonsOfSeries(any(), any()) }
        /*try {
            verify(exactly = 1) { numberSeasonsElement.text() }
        } catch (_:Exception) {
            verify(exactly = 1) { numberSeasonsElement.child(1).text() }
        }*/
        confirmVerified(jSoupConnection, linkedDataElement, undertitleElements, /*numberSeasonsElement,*/ seasonService)
    }

    @Test
    fun `Happy flow - name - Both name and alternate name mapped to series original name and name, respectively`() {
        val imdbId = generateImdbId()
        val name = "My name"
        val alternateName = "My alternate name"
        val linkedData = generateApplicationLinkedData(name = name, alternateName = alternateName)
        val seriesData = generateSeriesScrappedData(generateApplicationLinkedDataJson(linkedData))
        val expectedSeries = generateSeries(imdbId, seriesData)

        setupMocks(seriesData, expectedSeries.seasons)

        val actualSeries = subject.scrapTitle(imdbId)

        assertEquals(expectedSeries, actualSeries)
        assertEquals(alternateName, actualSeries.name)
        assertEquals(name, actualSeries.originalName)
        verifyMocks()
    }

    @Test
    fun `Happy flow - name - Only name, mapped to series name`() {
        val imdbId = generateImdbId()
        val name = "My name"
        val linkedData = generateApplicationLinkedData(name = name, alternateName = null)
        val seriesData = generateSeriesScrappedData(generateApplicationLinkedDataJson(linkedData))
        val expectedSeries = generateSeries(imdbId, seriesData)

        setupMocks(seriesData, expectedSeries.seasons)

        val actualSeries = subject.scrapTitle(imdbId)

        assertEquals(expectedSeries, actualSeries)
        assertEquals(name, actualSeries.name)
        assertNull(actualSeries.originalName)
        verifyMocks()
    }

    @Test
    fun `Happy flow - runtime - Still ongoing series`() {
        val imdbId = generateImdbId()
        val startYear = 2014
        val seriesData = generateSeriesScrappedData(runtime = "$startYear–")
        val expectedSeries = generateSeries(imdbId, seriesData)

        setupMocks(seriesData, expectedSeries.seasons)

        val actualSeries = subject.scrapTitle(imdbId)

        assertEquals(expectedSeries, actualSeries)
        assertEquals(startYear, actualSeries.startYear)
        assertNull(actualSeries.endYear)
        verifyMocks()
    }

    @Test
    fun `Happy flow - runtime - Ended series with only one year`() {
        val imdbId = generateImdbId()
        val year = 2014
        val seriesData = generateSeriesScrappedData(runtime = "$year")
        val expectedSeries = generateSeries(imdbId, seriesData)

        setupMocks(seriesData, expectedSeries.seasons)

        val actualSeries = subject.scrapTitle(imdbId)

        assertEquals(expectedSeries, actualSeries)
        assertEquals(year, actualSeries.startYear)
        assertEquals(year, actualSeries.endYear)
        verifyMocks()
    }

    @Test
    fun `Happy flow - runtime - Ended series with only multiple years`() {
        val imdbId = generateImdbId()
        val startYear = 2014
        val endYear = 2019
        val seriesData = generateSeriesScrappedData(runtime = "$startYear–$endYear")
        val expectedSeries = generateSeries(imdbId, seriesData)

        setupMocks(seriesData, expectedSeries.seasons)

        val actualSeries = subject.scrapTitle(imdbId)

        assertEquals(expectedSeries, actualSeries)
        assertEquals(startYear, actualSeries.startYear)
        assertEquals(endYear, actualSeries.endYear)
        verifyMocks()
    }

    @Test
    fun `Happy flow - duration - Series with only minutes`() {
        val imdbId = generateImdbId()
        val minutes = 23
        val seriesData = generateSeriesScrappedData(episodeDuration = "${minutes}m")
        val expectedSeries = generateSeries(imdbId, seriesData)
        val expectedDuration = Duration.ofMinutes(minutes.toLong())

        setupMocks(seriesData, expectedSeries.seasons)
        val actualSeries = subject.scrapTitle(imdbId)

        assertEquals(expectedSeries, actualSeries)
        assertEquals(expectedDuration, actualSeries.episodeDuration)
        verifyMocks()
    }

    @Test
    fun `Happy flow - duration - Series with only hours`() {
        val imdbId = generateImdbId()
        val hours = 2
        val seriesData = generateSeriesScrappedData(episodeDuration = "${hours}h")
        val expectedSeries = generateSeries(imdbId, seriesData)
        val expectedDuration = Duration.ofHours(hours.toLong())

        setupMocks(seriesData, expectedSeries.seasons)
        val actualSeries = subject.scrapTitle(imdbId)

        assertEquals(expectedSeries, actualSeries)
        assertEquals(expectedDuration, actualSeries.episodeDuration)
        verifyMocks()
    }

    @Test
    fun `Happy flow - duration - Series with both hours and minutes`() {
        val imdbId = generateImdbId()
        val hours = 1
        val minutes = 3
        val seriesData = generateSeriesScrappedData(episodeDuration = "${hours}h ${minutes}m")
        val expectedSeries = generateSeries(imdbId, seriesData)
        val expectedDuration = Duration.ofHours(hours.toLong()).plusMinutes(minutes.toLong())

        setupMocks(seriesData, expectedSeries.seasons)
        val actualSeries = subject.scrapTitle(imdbId)

        assertEquals(expectedSeries, actualSeries)
        assertEquals(expectedDuration, actualSeries.episodeDuration)
        verifyMocks()
    }

    @Test
    fun `Happy flow - numberSeasons - Series with only one season`() {
        val imdbId = generateImdbId()
        val seriesData = generateSeriesScrappedData(numberSeasons = "1 Season")
        val expectedSeries = generateSeries(imdbId, seriesData)

        setupMocks(seriesData, expectedSeries.seasons)
        val actualSeries = subject.scrapTitle(imdbId)

        assertEquals(expectedSeries, actualSeries)
        assertEquals(1, actualSeries.numberSeasons)
        verifyMocks()
    }

    @Test
    fun `Happy flow - numberSeasons - Series with multiple seasons`() {
        val imdbId = generateImdbId()
        val numberSeasons = 2
        val seriesData = generateSeriesScrappedData(numberSeasons = "$numberSeasons Seasons")
        val expectedSeries = generateSeries(imdbId, seriesData)

        setupMocks(seriesData, expectedSeries.seasons)
        val actualSeries = subject.scrapTitle(imdbId)

        assertEquals(expectedSeries, actualSeries)
        assertEquals(numberSeasons, actualSeries.numberSeasons)
        verifyMocks()
    }

    @Test
    fun `Connection - If could not retrieve HTML throws JSoupConnectionException`() {
        val imdbId = generateImdbId()
        val seriesData = generateSeriesScrappedData()
        val connectionErrorMessage = "Could not fetch HTML."
        val expectedErrorMessage = "Could not retrieve Series HTML."

        setupMocks(seriesData)

        every { jSoupConnection.newConnection(any()).get() } throws JSoupConnectionException(connectionErrorMessage)

        val ex = assertThrows<JSoupConnectionException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedErrorMessage))
        assertTrue(ex.message.contains(connectionErrorMessage))
    }

    @Test
    fun `LinkedData - If linkedDataElement is not found throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val seriesData = generateSeriesScrappedData()
        val expectedExceptionMessage = "Could not find LinkedData element."

        setupMocks(seriesData)
        every { doc.getElementsByAttributeValueStarting("type", "application/ld+json").first() } returns null

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `LinkedData - If linkedDataElement deserialization fails throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val seriesData = generateSeriesScrappedData(linkedData = "An invalid linkedDataJsonString")
        val expectedExceptionMessage = "An error occurred while deserializing the linkedData element."

        setupMocks(seriesData)

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `Type - If title not a TV Series throws NotATVSeriesException`() {
        val imdbId = generateImdbId()
        val wrongTitleType = "Movie"
        val seriesData = generateSeriesScrappedData(linkedData = generateApplicationLinkedDataJson(type = wrongTitleType))
        val expectedErrorMessage = "The imdb id $imdbId given is not a TV Series but a $wrongTitleType"

        setupMocks(seriesData)
        val ex = assertThrows<NotATvSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `UnderTitleElements - If underTitleElements could not be found throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val seriesData = generateSeriesScrappedData()
        val expectedExceptionMessage = "Could not find undertitle elements"

        setupMocks(seriesData)
        every { doc.getElementsByAttributeValueStarting("data-testid", "hero-title-block__metadata").first()?.children() } returns null

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `RuntimeText - If runtimeText could not be found throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val seriesData = generateSeriesScrappedData(runtime = null)
        val expectedExceptionMessage = "Could not find runtime text element"

        setupMocks(seriesData)
        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `RuntimeText - If runtimeText is blank throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val runtime = ""
        val seriesData = generateSeriesScrappedData(runtime = runtime)
        val expectedExceptionMessage = "Could not parse runtime. Input string was $runtime."

        setupMocks(seriesData)
        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `RuntimeText - If runtimetext could not be parsed throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val runtime = "An invalid runtime"
        val seriesData = generateSeriesScrappedData(runtime = runtime)
        val expectedExceptionMessage = "Could not parse runtime. Input string was $runtime."

        setupMocks(seriesData)
        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `EpisodeDuration - If episodeDuration could not be found throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val seriesData = generateSeriesScrappedData(episodeDuration = null)
        val expectedErrorMessage = "Could not find episodeDuration text element"

        setupMocks(seriesData)
        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `EpisodeDuration - If episodeDuration is blank throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val episodeDuration = ""
        val seriesData = generateSeriesScrappedData(episodeDuration = episodeDuration)
        val expectedErrorMessage = "Could not parse episodeDuration. Input string was $episodeDuration."

        setupMocks(seriesData)
        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `EpisodeDuration - If episodeDuration could not be parsed throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val episodeDuration = "An invalid episode duration"
        val seriesData = generateSeriesScrappedData(episodeDuration = episodeDuration)
        val expectedErrorMessage = "Could not parse episodeDuration. Input string was $episodeDuration."

        setupMocks(seriesData)
        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `NumberSeasons - If numberSeasons could not be found throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val seriesData = generateSeriesScrappedData()
        val expectedErrorMessage = "Could not find numberSeasons text element"

        setupMocks(seriesData)
        every { doc.getElementsByAttributeValueStarting("class", "BrowseEpisodes__BrowseLinksContainer").first() } returns null
        every { doc.getElementsByAttributeValueStarting("for", "browse-episodes-season").first() } returns null

        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `NumberSeasons - If numberSeasons is blank throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val numberSeasons = ""
        val seriesData = generateSeriesScrappedData(numberSeasons = numberSeasons)
        val expectedErrorMessage = "Could not parse numberSeasons. Input string was $numberSeasons."

        setupMocks(seriesData)
        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `NumberSeasons - If numberSeasons could not be parsed throws ErrorBuildingSeriesException`() {
        val imdbId = generateImdbId()
        val numberSeasons = "An invalid numberSeasons"
        val seriesData = generateSeriesScrappedData(numberSeasons = numberSeasons)
        val expectedErrorMessage = "Could not parse numberSeasons. Input string was $numberSeasons."

        setupMocks(seriesData)
        val ex = assertThrows<ErrorBuildingSeriesException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Seasons - If an error occurs fetching one of the seasons it bubbles up exception`() {
        val imdbId = generateImdbId()
        val seriesData = generateSeriesScrappedData()
        val series = generateSeries(imdbId, seriesData)
        val expectedErrorMessage = "Could not build season"

        setupMocks(seriesData, series.seasons)

        every { seasonService.getSeasonsOfSeries(imdbId, series.numberSeasons) } throws ErrorBuildingSeasonException(expectedErrorMessage)

        val ex = assertThrows<ErrorBuildingSeasonException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Episodes - If an error occurs fetching one of the episodes it bubbles up exception`() {
        val imdbId = generateImdbId()
        val seriesData = generateSeriesScrappedData()
        val series = generateSeries(imdbId, seriesData)
        val expectedErrorMessage = "Could not build episode"

        setupMocks(seriesData, series.seasons)
        every { seasonService.getSeasonsOfSeries(imdbId, series.numberSeasons) } throws ErrorBuildingEpisodeException(expectedErrorMessage)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.scrapTitle(imdbId) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }
}