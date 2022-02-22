package com.diogo_portela.imdb_craper.service

import com.diogo_portela.imdb_craper.helper.generateEpisode
import com.diogo_portela.imdb_craper.helper.generateEpisodeElementData
import com.diogo_portela.imdb_craper.model.EpisodeElementData
import com.diogo_portela.imdb_craper.model.exception.ErrorBuildingEpisodeException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    fun setupMocks(episodesData: Collection<EpisodeElementData>) {
        every { doc.getElementsByClass("list detail eplist").first() } returns episodeListElement
        every { episodeListElement.children() } returns episodeElements

        episodesData.forEach{ episodeData ->
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

            every { episodeNumberElement.attr("content") } returns episodeData.number.toString()
            every { episodeNameElement.text() } returns episodeData.name!!
            every { episodeNameElement.attr("href") } returns episodeData.url!!
            every { episodeAirdateElement.text() } returns episodeData.airdate!!
            every { episodeRatingValueElement.text() } returns episodeData.ratingValue!!.toString()
            every { episodeRatingCountElement.text() } returns episodeData.ratingCount!!.toString()
            every { episodeSummaryElement.text() } returns episodeData.summary!!
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
        }
        episodeNumberElements.forEach { verify(exactly = 1) { it.attr("content") } }
        episodeNameElements.forEach {
            verify(exactly = 1) { it.text() }
            verify(exactly = 1) { it.attr("href") }
        }
        episodeAirdateElements.forEach { verify(exactly = 1) { it.text() } }
        episodeRatingValueElements.forEach { verify(exactly = 1) { it.text() } }
        episodeRatingCountElements.forEach { verify(exactly = 1) { it.text() } }
        episodeSummaryElements.forEach { verify(exactly = 1) { it.text() } }
    }

    @Test
    fun `If there is not parsing or validation errors, returns a set of Seasons as expected`(){
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData()
        setupMocks(setOf(episodeOneData, episodeTwoData))

        val episodeOne = generateEpisode(episodeOneData)
        val episodeTwo = generateEpisode(episodeTwoData)
        val expectedEpisodes = setOf(episodeOne, episodeTwo)

        val actualEpisodes = subject.getEpisodesOfSeason(doc, expectedEpisodes.size)

        assertEquals(expectedEpisodes, actualEpisodes)
        verifyMocks()
    }

    @Test
    fun `If episode list could not be found, it throws ErrorBuildingEpisodeException`(){
        val expectedErrorMessage = "Could not find episode list"
        every { doc.getElementsByClass("list detail eplist").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, numberEpisodes = 1) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

   @Test
   fun `If episode number element could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
       val episodeWithErrorNumber = 2
       val episodeOneData = generateEpisodeElementData()
       val episodeTwoData = generateEpisodeElementData()
       val episodesData = setOf(episodeOneData, episodeTwoData)
       val expectedErrorMessage = "Could not find episode number"

       setupMocks(episodesData)
       every { episodeElements[episodeWithErrorNumber - 1].getElementsByAttributeValue("itemprop", "episodeNumber").first() } returns null

       val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

       assertTrue(ex.message.contains(expectedErrorMessage))
   }

    @Test
    fun `If name and url element could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val episodeWithErrorNumber = 2
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData()
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not find episode name and url element"

        setupMocks(episodesData)
        every { episodeElements[episodeWithErrorNumber - 1].getElementsByAttributeValue("itemprop", "name").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If name text element is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData(name = "")
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Episode name text was blank"

        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If url text element is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val inputString = ""
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData(url = inputString)
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not parse episodeImdbId. Input string was $inputString."

        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If url text element parsing fails for one episode, it throws ErrorBuildingEpisodeException`(){
        val inputString = "Not a valid url"
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData(url = inputString)
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not parse episodeImdbId. Input string was $inputString."

        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If airdate element could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val episodeWithErrorNumber = 2
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData()
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not find airdate text element"

        setupMocks(episodesData)
        every { episodeElements[episodeWithErrorNumber - 1].getElementsByClass("airdate").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If airdate element is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val inputString = ""
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData(airdate = inputString)
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not parse airdate. Input string was $inputString."

        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If airdate text element parsing fails for one episode, it throws ErrorBuildingEpisodeException`(){
        val inputString = "Not a valid airdate"
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData(airdate = inputString)
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not parse airdate. Input string was $inputString."

        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If rating value element could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val episodeWithErrorNumber = 2
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData()
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not find ratingValue text element"

        setupMocks(episodesData)
        every { episodeElements[episodeWithErrorNumber - 1].getElementsByClass("ipl-rating-star__rating").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If rating value element is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val inputString = ""
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData(ratingValue = inputString)
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not parse ratingValue. Input string was $inputString."

        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If ratingValue text element parsing fails for one episode, it throws ErrorBuildingEpisodeException`(){
        val inputString = "Not a valid rating value"
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData(ratingValue = inputString)
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not parse ratingValue. Input string was $inputString."

        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If rating count element could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val episodeWithErrorNumber = 2
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData()
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not find ratingCount text element"

        setupMocks(episodesData)
        every { episodeElements[episodeWithErrorNumber - 1].getElementsByClass("ipl-rating-star__total-votes").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If rating count element is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val inputString = ""
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData(ratingCount = inputString)
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not parse ratingCount. Input string was $inputString."

        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If rating count text element parsing fails for one episode, it throws ErrorBuildingEpisodeException`(){
        val inputString = "Not a valid rating count"
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData(ratingCount = inputString)
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not parse ratingCount. Input string was $inputString."

        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If summary element could not be found for one episode, it throws ErrorBuildingEpisodeException`(){
        val episodeWithErrorNumber = 2
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData()
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Could not find summary text element"

        setupMocks(episodesData)
        every { episodeElements[episodeWithErrorNumber - 1].getElementsByClass("item_description").first() } returns null

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `If summary element is blank for one episode, it throws ErrorBuildingEpisodeException`(){
        val inputString = ""
        val episodeOneData = generateEpisodeElementData()
        val episodeTwoData = generateEpisodeElementData(summary = inputString)
        val episodesData = setOf(episodeOneData, episodeTwoData)
        val expectedErrorMessage = "Episode summary text was blank"

        setupMocks(episodesData)

        val ex = assertThrows<ErrorBuildingEpisodeException> { subject.getEpisodesOfSeason(doc, episodesData.size) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }
}