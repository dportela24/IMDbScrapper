package com.diogo_portela.imdb_scraper.service

import com.diogo_portela.imdb_scraper.helper.*
import com.diogo_portela.imdb_scraper.model.Episode
import com.diogo_portela.imdb_scraper.model.EpisodeScrappedData
import com.diogo_portela.imdb_scraper.model.exception.ErrorBuildingEpisodeException
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

            every { episodeNumberElement.attr("content") } returns scrappedData.number.toString()
            every { episodeNameElement.text() } returns scrappedData.name!!
            every { episodeNameElement.attr("href") } returns scrappedData.url!!
            every { episodeAirdateElement.text() } returns scrappedData.airdate!!
            every { episodeRatingValueElement.text() } returns scrappedData.ratingValue!!.toString()
            every { episodeRatingCountElement.text() } returns scrappedData.ratingCount!!.toString()
            every { episodeSummaryElement.text() } returns scrappedData.summary!!
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
    fun `Happy flow - Episodes with first airdate format`(){
        val numberEpisodes = 2
        val (episodesData, expectedEpisodes) = setupEpisodes(numberEpisodes)

        setupMocks(episodesData)

        val actualEpisodes = subject.getEpisodesOfSeason(doc, expectedEpisodes.size)

        assertEquals(expectedEpisodes.toSet(), actualEpisodes)
        verifyMocks()
    }

    @Test
    fun `Happy flow - Episodes with second airdate format`(){
        val numberEpisodes = 2
        val (episodesData, expectedEpisodes) = setupEpisodes(numberEpisodes)
        episodesData.forEach{ episode->
            episode.airdate = episode.airdate!!.replace(".", "")
        }

        setupMocks(episodesData)

        val actualEpisodes = subject.getEpisodesOfSeason(doc, expectedEpisodes.size)

        assertEquals(expectedEpisodes.toSet(), actualEpisodes)
        verifyMocks()
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
        val expectedErrorMessage = "Could not find episode list"
        every { doc.getElementsByClass("list detail eplist").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, numberEpisodes = 1) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

   @Test
   fun `EpisodeNumber - If episode number element could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
       val numberEpisodes = 2
       val episodeWithErrorNumber = 2
       val (episodesData, _) = setupEpisodes(numberEpisodes)
       val expectedErrorMessage = "Could not find episode number"

       setupMocks(episodesData)
       every { episodeElements[episodeWithErrorNumber - 1].getElementsByAttributeValue("itemprop", "episodeNumber").first() } returns null

       val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

       assertTrue(ex.message.contains(expectedErrorMessage))
   }

    @Test
    fun `Name and URL - If name and url element could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val expectedErrorMessage = "Could not find episode name and url element"

        setupMocks(episodesData)
        every { episodeElements[episodeWithErrorNumber - 1].getElementsByAttributeValue("itemprop", "name").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Name - If name is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val expectedErrorMessage = "Episode name text was blank"

        episodesData[episodeWithErrorNumber - 1].name = ""
        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `URL - If url is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val inputString = ""
        val expectedErrorMessage = "Could not parse episodeImdbId. Input string was $inputString."

        episodesData[episodeWithErrorNumber - 1].url = inputString
        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `URL - If url could not be parsed for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val inputString = "Not a valid url"
        val expectedErrorMessage = "Could not parse episodeImdbId. Input string was $inputString."

        episodesData[episodeWithErrorNumber - 1].url = inputString
        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Airdate - If airdate could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val expectedErrorMessage = "Could not find airdate text element"

        setupMocks(episodesData)
        every { episodeElements[episodeWithErrorNumber - 1].getElementsByClass("airdate").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Airdate - If airdate is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val inputString = ""
        val expectedErrorMessage = "Could not parse airdate. Input string was $inputString."

        episodesData[episodeWithErrorNumber - 1].airdate = inputString
        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Airdate - If airdate could not be parsed for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val inputString = "Not a valid airdate"
        val expectedErrorMessage = "Could not parse airdate. Input string was $inputString."

        episodesData[episodeWithErrorNumber - 1].airdate = inputString
        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `RatingValue - If rating value could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val expectedErrorMessage = "Could not find ratingValue text element"

        setupMocks(episodesData)
        every { episodeElements[episodeWithErrorNumber - 1].getElementsByClass("ipl-rating-star__rating").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `RatingValue - If rating value is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val inputString = ""
        val expectedErrorMessage = "Could not parse ratingValue. Input string was $inputString."

        episodesData[episodeWithErrorNumber - 1].ratingValue = inputString
        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `RatingValue - If ratingValue could not be parsed for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val inputString = "Not a valid rating value"
        val expectedErrorMessage = "Could not parse ratingValue. Input string was $inputString."

        episodesData[episodeWithErrorNumber - 1].ratingValue = inputString
        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `RatingCount - If rating count could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val expectedErrorMessage = "Could not find ratingCount text element"

        setupMocks(episodesData)
        every { episodeElements[episodeWithErrorNumber - 1].getElementsByClass("ipl-rating-star__total-votes").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `RatingCount - If rating count is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val inputString = ""
        val expectedErrorMessage = "Could not parse ratingCount. Input string was $inputString."

        episodesData[episodeWithErrorNumber - 1].ratingCount = inputString
        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `RatingCount - If rating count could not be parsed for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val inputString = "Not a valid rating count"
        val expectedErrorMessage = "Could not parse ratingCount. Input string was $inputString."

        episodesData[episodeWithErrorNumber - 1].ratingCount = inputString
        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Summary - If summary could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val episodeWithErrorNumber = 2
        val episodeOneData = generateEpisodeScrappedData()
        val episodeTwoData = generateEpisodeScrappedData()
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not find summary text element"

        setupMocks(episodesData)
        every { episodeElements[episodeWithErrorNumber - 1].getElementsByClass("item_description").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `Summary - If summary is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val numberEpisodes = 2
        val episodeWithErrorNumber = 2
        val (episodesData, _) = setupEpisodes(numberEpisodes)
        val inputString = ""
        val expectedErrorMessage = "Episode summary text was blank"

        episodesData[episodeWithErrorNumber - 1].summary = inputString
        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }
}