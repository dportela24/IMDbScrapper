package com.diogo_portela.imdb_scraper.service

import com.diogo_portela.imdb_scraper.helper.generateErrorMessage
import com.diogo_portela.imdb_scraper.helper.generateSearchResult
import com.diogo_portela.imdb_scraper.helper.generateSearchScrappedData
import com.diogo_portela.imdb_scraper.model.JSoupConnection
import com.diogo_portela.imdb_scraper.model.SearchResult
import com.diogo_portela.imdb_scraper.model.SearchScrappedData
import com.diogo_portela.imdb_scraper.model.exception.EmptySearchResultException
import com.diogo_portela.imdb_scraper.model.exception.JSoupConnectionException
import com.diogo_portela.imdb_scraper.model.exception.MissingParametersException
import com.diogo_portela.imdb_scraper.model.exception.SearchScrappingErrorException
import io.mockk.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random.Default.nextInt

class SearchServiceTest {
    val jSoupConnection = mockk<JSoupConnection>()

    val subject = SearchService(jSoupConnection)

    val doc = mockk<Document>()
    val resultsList = Elements()
    val headerElements = Elements()
    val aLinkElements = Elements()

    fun setupMocks(scrappedData: List<SearchScrappedData>){
        every { jSoupConnection.newConnection(any()).get() } returns doc
        every { doc.getElementsByClass("findList").first()?.child(0)?.children() } answers {
            resultsList.ifEmpty { null }
        }

        scrappedData.forEach { result ->
            val resultElement = mockk<Element>().also { resultsList.add(it) }
            val headerElement = mockk<Element>().also { headerElements.add(it) }
            val aLinkElement = mockk<Element>().also { aLinkElements.add(it) }

            every { resultElement.getElementsByClass("result_text").first() } returns headerElement
            every { headerElement.getElementsByTag("a").first() } returns aLinkElement

            result.url?.let { every { aLinkElement.attr("href") } returns it }
            result.name?.let { every { aLinkElement.text() } returns it }
        }
    }

    fun verifyMocks(limit: Int) {
        verify(exactly = 1) { jSoupConnection.newConnection(any()).get() }
        verify(exactly = 1) { doc.getElementsByClass("findList").first()?.child(0)?.children() }

        resultsList.indices.forEach { i ->
            val resultElement = resultsList[i]
            val headerElement = headerElements[i]
            val aLinkElement = aLinkElements[i]

            if (i < limit) {
                verify(exactly = 1) { resultElement.getElementsByClass("result_text").first() }
                verify(exactly = 1) { headerElement.getElementsByTag("a").first() }
                verify(exactly = 1) { aLinkElement.attr("href") }
                verify(exactly = 1) { aLinkElement.text() }
            }
            confirmVerified(resultElement)
            confirmVerified(headerElement)
            confirmVerified(aLinkElement)
        }
        confirmVerified(jSoupConnection, doc)
    }

    fun setupSearchResults(numberResults: Int) : Pair<List<SearchScrappedData>, List<SearchResult>> {
        val resultsData = mutableListOf<SearchScrappedData>()
        val results = mutableListOf<SearchResult>()

        (1..numberResults).map{
            val scrappedData = generateSearchScrappedData()
            val episode = generateSearchResult(scrappedData)
            resultsData.add(scrappedData)
            results.add(episode)
        }

        return Pair(resultsData, results)
    }

    @Test
    fun `Happy path - more results than limit - if there is no errors or exceptions returns the correct limited search results`() {
        val searchLimit = 5 //nextInt(1, 11)
        val numberSearchResults = nextInt( searchLimit + 1, searchLimit + 10)
        val (scrappedData, results) = setupSearchResults(numberSearchResults)
        val searchInput = "My search"
        val expectedResults = results.subList(0, searchLimit)

        setupMocks(scrappedData)

        val actualResults = subject.searchByName(searchInput, searchLimit)

        assertEquals(expectedResults, actualResults)
        verifyMocks(searchLimit)
    }

    @Test
    fun `Happy path - less results than limit - if there is no errors or exceptions returns the correct total search results`() {
        val searchLimit = nextInt(1, 11)
        val numberSearchResults = nextInt(  1, searchLimit)
        val (scrappedData, results) = setupSearchResults(numberSearchResults)
        val searchInput = "My search"
        val expectedResults = results.subList(0, numberSearchResults)

        setupMocks(scrappedData)

        val actualResults = subject.searchByName(searchInput, searchLimit)

        assertEquals(expectedResults, actualResults)
        verifyMocks(searchLimit)
    }

    @Test
    fun `searchInput - If searchInput does not exist throws MissingParametersException`() {
        val searchInput = null
        val searchLimit = nextInt(1, 11)

        val ex = assertThrows<MissingParametersException> { subject.searchByName(searchInput, searchLimit) }

        assertTrue(ex.message.contains("searchInput"))
    }

    @Test
    fun `searchInput - If searchInput is blank throws MissingParametersException`() {
        val searchInput = ""
        val searchLimit = nextInt(1, 11)

        val ex = assertThrows<MissingParametersException> { subject.searchByName(searchInput, searchLimit) }

        assertTrue(ex.message.contains("searchInput"))
    }

    @Test
    fun `No search results - if search returns no result throws EmptySearchResultException`() {
        val searchLimit = nextInt(1, 11)
        val searchInput = "My search"

        setupMocks(emptyList())

        assertThrows<EmptySearchResultException> { subject.searchByName(searchInput, searchLimit) }

        verifyMocks(searchLimit)
    }

    @Test
    fun `Connection - If could not request search throws JSoupConnectionException`() {
        val searchLimit = nextInt(1, 11)
        val searchStr = "My search"
        val exceptionMessage = "Exception message"

        every { jSoupConnection.newConnection(any()).get() } throws RuntimeException(exceptionMessage)

        val ex = assertThrows<JSoupConnectionException> { subject.searchByName(searchStr, searchLimit) }

        assertTrue(ex.message.contains(exceptionMessage))
    }

    @Test
    fun `header - If header element could not be found, it throws SearchErrorException`() {
        val searchLimit = nextInt(1, 11)
        val searchStr = "My search"
        val scrappedData = generateSearchScrappedData()
        val expectedErrorMessage = generateErrorMessage("searchResultText")

        setupMocks(listOf(scrappedData))
        every { resultsList.first()!!.getElementsByClass("result_text").first() } returns null

        val ex = assertThrows<SearchScrappingErrorException> { subject.searchByName(searchStr, searchLimit) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `aLink - If aLink element could not be found, it throws SearchErrorException`() {
        val searchLimit = nextInt(1, 11)
        val searchStr = "My search"
        val scrappedData = generateSearchScrappedData()
        val expectedErrorMessage = generateErrorMessage("searchResultALink")

        setupMocks(listOf(scrappedData))
        every { headerElements.first()!!.getElementsByTag("a").first() } returns null

        val ex = assertThrows<SearchScrappingErrorException> { subject.searchByName(searchStr, searchLimit) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `searchResultImdbId - If url is blank throws SearchErrorException`() {
        val url = ""
        val searchLimit = nextInt(1, 11)
        val searchStr = "My search"
        val scrappedData = generateSearchScrappedData(url = url)
        val expectedErrorMessage = generateErrorMessage("searchResultImdbId", url)

        setupMocks(listOf(scrappedData))

        val ex = assertThrows<SearchScrappingErrorException> { subject.searchByName(searchStr, searchLimit) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `searchResultImdbId - If url could not be parsed blank throws SearchErrorException`() {
        val url = "Not a valid url"
        val searchLimit = nextInt(1, 11)
        val searchStr = "My search"
        val scrappedData = generateSearchScrappedData(url = url)
        val expectedErrorMessage = generateErrorMessage("searchResultImdbId", url)

        setupMocks(listOf(scrappedData))

        val ex = assertThrows<SearchScrappingErrorException> { subject.searchByName(searchStr, searchLimit) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }

    @Test
    fun `name - If name is blank throws SearchErrorException`() {
        val name = ""
        val searchLimit = nextInt(1, 11)
        val searchStr = "My search"
        val scrappedData = generateSearchScrappedData(name = name)
        val expectedErrorMessage = generateErrorMessage("searchResultName", name)

        setupMocks(listOf(scrappedData))

        val ex = assertThrows<SearchScrappingErrorException> { subject.searchByName(searchStr, searchLimit) }

        assertTrue(ex.message.contains(expectedErrorMessage))
    }
}