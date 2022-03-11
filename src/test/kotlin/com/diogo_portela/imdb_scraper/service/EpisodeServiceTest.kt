package com.diogo_portela.imdb_scraper.service

import com.diogo_portela.imdb_scraper.helper.*
import com.diogo_portela.imdb_scraper.model.Episode
import com.diogo_portela.imdb_scraper.model.EpisodeScrappedData
import com.diogo_portela.imdb_scraper.model.exception.EpisodeScrappingErrorException
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Year
import kotlin.system.measureTimeMillis

class EpisodeServiceTest {
    val subject = EpisodeService()

    val doc = mockk<Document>()
    val episodeListElement = mockk<Element>()

    val episodeElements = Elements()
    val episodeNumberElements = Elements()
    val episodeNameElements = Elements()
    val episodeAirdateElements = Elements()
    val episodeRatingValueElements = Elements()
    val episodeRatingCountElements = Elements()
    val episodeSummaryElements = Elements()

    fun setupMocks(episodesScrappedData: Collection<EpisodeScrappedData>) {
        every { doc.getElementsByClass("list detail eplist").first() } returns episodeListElement
        every { episodeListElement.children() } returns episodeElements

        episodesScrappedData.forEach{ scrappedData ->
            val episodeElement = mockk<Element>().also { episodeElements.add(it) }
            val episodeNumberElement = mockk<Element>().also { episodeNumberElements.add(it) }
            val episodeNameElement = mockk<Element>().also { episodeNameElements.add(it) }
            val episodeAirdateElement = mockk<Element>().also { episodeAirdateElements.add(it) }
            val episodeRatingValueElement = mockk<Element>().also { episodeRatingValueElements.add(it) }
            val episodeRatingCountElement = mockk<Element>().also { episodeRatingCountElements.add(it) }
            val episodeSummaryElement = mockk<Element>().also { episodeSummaryElements.add(it) }

            every { episodeElement.getElementsByAttributeValue("itemprop", "episodeNumber").first() } returns episodeNumberElement
            every { episodeElement.getElementsByAttributeValue("itemprop", "name").first() } returns episodeNameElement
            every { episodeElement.getElementsByClass("airdate").first() } returns episodeAirdateElement
            every { episodeElement.getElementsByClass("ipl-rating-star__rating").first() } returns episodeRatingValueElement
            every { episodeElement.getElementsByClass("ipl-rating-star__total-votes").first() } returns episodeRatingCountElement
            every { episodeElement.getElementsByClass("item_description").first() } returns episodeSummaryElement

            scrappedData.number?.let { every { episodeNumberElement.attr("content") } returns it }
            scrappedData.name?.let { every { episodeNameElement.text() } returns it }
            scrappedData.url?.let { every { episodeNameElement.attr("href") } returns it }
            scrappedData.airdate?.let { every { episodeAirdateElement.text() } returns it }
            scrappedData.ratingValue?.let { every { episodeRatingValueElement.text() } returns it }
            scrappedData.ratingCount?.let { every { episodeRatingCountElement.text() } returns it }
            scrappedData.summary?.let {
                every { episodeSummaryElement.text() } returns it

                if (it == NO_SUMMARY_PLACEHOLDER)
                    every { episodeSummaryElement.getElementsByTag("a") } returns Elements(Element("a"))
                else
                    every { episodeSummaryElement.getElementsByTag("a") } returns Elements()
            }
        }
    }

    fun verifyMocks() {
        verify(exactly = 1) { doc.getElementsByClass("list detail eplist").first() }
        verify(exactly = 1) { episodeListElement.children() }

        episodeElements.forEach {
            verify(exactly = 1) { it.getElementsByAttributeValue("itemprop", "episodeNumber").first() }
            verify(exactly = 1) { it.getElementsByAttributeValue("itemprop", "name").first() }
            verify(exactly = 1) { it.getElementsByClass("airdate").first() }
            verify(exactly = 1) { it.getElementsByClass("ipl-rating-star__rating").first() }
            verify(exactly = 1) { it.getElementsByClass("ipl-rating-star__total-votes").first() }
            verify(exactly = 1) { it.getElementsByClass("item_description").first() }
            confirmVerified(it)
        }
        episodeNumberElements.forEach {
            verify(exactly = 1) { it.attr("content") }
            confirmVerified(it)
        }
        episodeNameElements.forEach {
            verify(exactly = 1) { it.text() }
            verify(exactly = 1) { it.attr("href") }
            confirmVerified(it)
        }
        episodeAirdateElements.forEach {
            verify(exactly = 1) { it.text() }
            confirmVerified(it)
        }
        episodeRatingValueElements.forEach {
            verify(exactly = 1) { it.text() }
            confirmVerified(it)
        }
        episodeRatingCountElements.forEach {
            verify(exactly = 1) { it.text() }
            confirmVerified(it)
        }
        episodeSummaryElements.forEach {
            verify(exactly = 1) { it.text() }
            verify(exactly = 1) { it.getElementsByTag("a") }
            confirmVerified(it)
        }
    }

    fun setupEpisodes(numberEpisodes: Int) : Pair<List<EpisodeScrappedData>, List<Episode>> {
        val episodesData = mutableListOf<EpisodeScrappedData>()
        val episodes = mutableListOf<Episode>()

        (1..numberEpisodes).map{
            val scrappedData = generateEpisodeScrappedData()
            val episode = generateEpisode(scrappedData)
            episodesData.add(scrappedData)
            episodes.add(episode)
        }

        return Pair(episodesData, episodes)
    }

    @Test
    fun `Happy path`(){
        val numberEpisodes = 5
        val (episodesData, expectedEpisodes) = setupEpisodes(numberEpisodes)

        setupMocks(episodesData)

        val actualEpisodes = subject.getEpisodesOfSeason(doc, expectedEpisodes.size)

        assertEquals(expectedEpisodes.toSet(), actualEpisodes)
        verifyMocks()
    }

    @Test
    fun `Happy path - Airdate - Episodes without dot in airdate format`(){
        val episodeData = generateEpisodeScrappedData()
        val expectedEpisode = generateEpisode(episodeData)
        episodeData.airdate = episodeData.airdate!!.replace(".", "")

        setupMocks(setOf(episodeData))

        val actualEpisodes = subject.getEpisodesOfSeason(doc, 1).first()

        assertEquals(expectedEpisode, actualEpisodes)
        verifyMocks()
    }

    @Test
    fun `Happy path - Airdate - Episodes with only month and year`(){
        val episodeData = generateEpisodeScrappedData(airdate = "Aug 2014")
        val expectedEpisode = generateEpisode(episodeData)

        setupMocks(setOf(episodeData))

        val actualEpisodes = subject.getEpisodesOfSeason(doc, 1).first()

        assertEquals(expectedEpisode, actualEpisodes)
        verifyMocks()
    }

    @Test
    fun `Happy path - Airdate - Episodes with year`(){
        val episodeData = generateEpisodeScrappedData(airdate = "2014")
        val expectedAirdate = Year.of(2014)
        val expectedEpisode = generateEpisode(episodeData)

        setupMocks(setOf(episodeData))

        val actualEpisodes = subject.getEpisodesOfSeason(doc, 1).first()

        assertEquals(expectedEpisode, actualEpisodes)
        assertEquals(expectedAirdate, actualEpisodes.airdate)
        verifyMocks()
    }

    @Test
    fun `Happy path - Airdate - If airdate element exists but is empty text returns null`(){
        val episodeData = generateEpisodeScrappedData(airdate = "")
        val expectedEpisode = generateEpisode(episodeData)

        setupMocks(setOf(episodeData))
        val actualEpisodes = subject.getEpisodesOfSeason(doc, 1).first()

        assertEquals(expectedEpisode, actualEpisodes)
        assertNull(actualEpisodes.airdate)
        verifyMocks()
    }

    @Test
    fun `Happy path - Rating - If both rating value and count could not be found for one episode results in null`(){
        val episodeData = generateEpisodeScrappedData(ratingValue = null, ratingCount = null)
        val expectedEpisode = generateEpisode(episodeData)

        setupMocks(setOf(episodeData))
        every { episodeElements.first()!!.getElementsByClass("ipl-rating-star__rating").first() } returns null
        every { episodeElements.first()!!.getElementsByClass("ipl-rating-star__total-votes").first() } returns null

        val actualEpisode =  subject.getEpisodesOfSeason(doc, 1).first()

        assertEquals(expectedEpisode, actualEpisode)
        assertNull(actualEpisode.ratingValue)
        assertNull(actualEpisode.ratingCount)
    }

    @Test
    fun `Happy path - summary - Summary is IMDb's default 'no summary' results in null`(){
        val episodeData = generateEpisodeScrappedData(summary = NO_SUMMARY_PLACEHOLDER)
        val expectedEpisode = generateEpisode(episodeData)

        setupMocks(setOf(episodeData))
        every { episodeSummaryElements.first()!!.getElementsByTag("a") } returns Elements(Element("a"))

        val actualEpisode =  subject.getEpisodesOfSeason(doc, 1).first()

        assertEquals(expectedEpisode, actualEpisode)
        assertNull(actualEpisode.summary)
    }

    @Test
    fun `Parallelism - Episodes are processed in parallel in different coroutines`(){
        val subjectSpy = spyk(subject)
        val numberEpisodes = 2
        val millisPerEpisode = 500L
        val mockElements = Elements(numberEpisodes)

        every { doc.getElementsByClass("list detail eplist").first() } returns episodeListElement
        every { episodeListElement.children() } returns mockElements
        coEvery { subjectSpy.buildEpisode(any()) } answers {
            runBlocking {
                delay(millisPerEpisode)
                generateEpisode()
            }
        }

        val computingTime = measureTimeMillis {
            subjectSpy.getEpisodesOfSeason(doc, numberEpisodes)
        }

        assert(computingTime < millisPerEpisode + 100) // 100 ms of overhead computation
    }

    @Test
    fun `EpisodeList - If episode list could not be found, it throws ErrorBuildingEpisodeException`(){
        val expectedErrorMessage = generateErrorMessage("episodeList")

        every { doc.getElementsByClass("list detail eplist").first() } returns null

        val ex = assertThrows<EpisodeScrappingErrorException> { subject.getEpisodesOfSeason(doc, 1) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `EpisodeNumber - If episode number element could not be found, it throws ErrorBuildingEpisodeException`(){
       val episodeData = generateEpisodeScrappedData()
       val expectedErrorMessage = generateErrorMessage("episodeNumber")

       setupMocks(setOf(episodeData))
       every { episodeElements.first()!!.getElementsByAttributeValue("itemprop", "episodeNumber").first() } returns null

       val ex = assertThrows<EpisodeScrappingErrorException> { subject.getEpisodesOfSeason(doc, 1) }

       assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `EpisodeNumber - If episode number text could not be parsed, it throws ErrorBuildingEpisodeException`(){
        val episodeNumberString = "An invalid number"
        val episodeData = generateEpisodeScrappedData(number = episodeNumberString)
        val expectedErrorMessage = generateErrorMessage("episodeNumber", episodeNumberString)

        setupMocks(setOf(episodeData))

        val ex = assertThrows<EpisodeScrappingErrorException> { subject.getEpisodesOfSeason(doc, 1) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Name and URL - If name and url element could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val episodeData = generateEpisodeScrappedData()
        val expectedErrorMessage = generateErrorMessage("nameAndUrl")

        setupMocks(setOf(episodeData))
        every { episodeElements.first()!!.getElementsByAttributeValue("itemprop", "name").first() } returns null

        val ex = assertThrows<EpisodeScrappingErrorException> { subject.getEpisodesOfSeason(doc, 1) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Name - If name is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val episodeData = generateEpisodeScrappedData(name = "")
        val expectedErrorMessage = "Episode name text was blank"

        setupMocks(setOf(episodeData))

        val ex = assertThrows<EpisodeScrappingErrorException> { subject.getEpisodesOfSeason(doc, 1) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `ImdbId - If url could not be parsed for one episode, it throws ErrorBuildingEpisodeException`(){
        val url = "Not a valid url"
        val episodesData = generateEpisodeScrappedData(url = url)
        val expectedErrorMessage = generateErrorMessage("episodeImdbId", url)

        setupMocks(setOf(episodesData))

        val ex = assertThrows<EpisodeScrappingErrorException> { subject.getEpisodesOfSeason(doc, 1) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Airdate - If airdate element could not be found, it throws ErrorBuildingEpisodeException`(){
        val episodeData = generateEpisodeScrappedData()
        val expectedErrorMessage = generateErrorMessage("airdate")

        setupMocks(setOf(episodeData))
        every { episodeElements.first()!!.getElementsByClass("airdate").first() } returns null

        val ex = assertThrows<EpisodeScrappingErrorException> { subject.getEpisodesOfSeason(doc, 1) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Airdate - If airdate could not be parsed, it throws ErrorBuildingEpisodeException`(){
        val airdate = "Not a valid airdate"
        val episodeData = generateEpisodeScrappedData(airdate = airdate)
        val expectedErrorMessage = generateErrorMessage("airdate", airdate)

        setupMocks(setOf(episodeData))

        val ex = assertThrows<EpisodeScrappingErrorException> { subject.getEpisodesOfSeason(doc, 1) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `RatingValue - If rating count exists but no rating value, it throws ErrorBuildingEpisodeException`() {
        val episodeData = generateEpisodeScrappedData(ratingValue = null)
        val expectedExceptionMessage = "Rating count exists but no rating value"

        setupMocks(setOf(episodeData))
        every { episodeElements.first()!!.getElementsByClass("ipl-rating-star__rating").first() } returns null

        val ex = assertThrows<EpisodeScrappingErrorException> { subject.getEpisodesOfSeason(doc, 1) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `RatingCount - If rating value exists but no rating count, it throws ErrorBuildingEpisodeException`() {
        val episodeData = generateEpisodeScrappedData(ratingCount = null)
        val expectedExceptionMessage = "Rating value exists but no rating count"

        setupMocks(setOf(episodeData))
        every { episodeElements.first()!!.getElementsByClass("ipl-rating-star__total-votes").first() } returns null

        val ex = assertThrows<EpisodeScrappingErrorException> { subject.getEpisodesOfSeason(doc, 1) }

        assertTrue(ex.message.contains(expectedExceptionMessage))
    }

    @Test
    fun `Summary - If summary element could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val episodeData = generateEpisodeScrappedData()
        val expectedErrorMessage = generateErrorMessage("summaryText")

        setupMocks(setOf(episodeData))
        every { episodeElements.first()!!.getElementsByClass("item_description").first() } returns null

        val ex = assertThrows<EpisodeScrappingErrorException> { subject.getEpisodesOfSeason(doc, 1) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }
}