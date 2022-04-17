package com.diogo_portela.imdb_scraper.controller

import com.diogo_portela.imdb_scraper.helper.generateSearchResult
import com.diogo_portela.imdb_scraper.model.ErrorDetails
import com.diogo_portela.imdb_scraper.model.exception.*
import com.diogo_portela.imdb_scraper.service.SearchService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(SearchController::class)
class SearchControllerTest {
    @TestConfiguration
    class SearchControllerTestConfig {
        @Bean
        fun service() = mockk<SearchService>()
    }

    @Autowired
    lateinit var mockMVc: MockMvc

    @Autowired
    lateinit var searchService: SearchService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Value("\${SEARCH_RESULT_LIMIT}")
    lateinit var defaultSearchLimit: Integer

    @Test
    fun `Happy path - no limit - calls searchService and returns the search results scrapped by it, limited by default limit`() {
        val searchQuery = "my%20search%20query"
        val expectedResults = (1..defaultSearchLimit.toInt()).map { generateSearchResult() }
        val expectedResultsJson = objectMapper.writeValueAsString(expectedResults)

        val limitSlot = slot<Int>()
        every { searchService.searchByName(searchQuery, capture(limitSlot)) } returns expectedResults

        mockMVc.get("/search/name?q=$searchQuery")
            .andExpect{
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedResultsJson) }
            }

        assertEquals(limitSlot.captured, defaultSearchLimit)
    }

    @Test
    fun `Happy path - limit smaller than default - calls searchService and returns the search results scrapped by it, limited by request limit`() {
        val searchQuery = "my%20search%20query"
        val requestLimit = defaultSearchLimit.toInt() - 1
        val expectedResults = (1..defaultSearchLimit.toInt()).map { generateSearchResult() }
        val expectedResultsJson = objectMapper.writeValueAsString(expectedResults)

        val limitSlot = slot<Int>()
        every { searchService.searchByName(searchQuery, capture(limitSlot)) } returns expectedResults

        mockMVc.get("/search/name?q=$searchQuery&limit=$requestLimit")
            .andExpect{
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedResultsJson) }
            }

        assertEquals(limitSlot.captured, requestLimit)
    }

    @Test
    fun `Happy path - limit greater than default - calls searchService and returns the search results scrapped by it, limited by default limit`() {
        val searchQuery = "my%20search%20query"
        val requestLimit = defaultSearchLimit.toInt() + 1
        val expectedResults = (1..defaultSearchLimit.toInt()).map { generateSearchResult() }
        val expectedResultsJson = objectMapper.writeValueAsString(expectedResults)

        val limitSlot = slot<Int>()
        every { searchService.searchByName(searchQuery, capture(limitSlot)) } returns expectedResults

        mockMVc.get("/search/name?q=$searchQuery&limit=$requestLimit")
            .andExpect{
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedResultsJson) }
            }

        assertEquals(limitSlot.captured, defaultSearchLimit)
    }

    @Test
    fun `MissingParametersException - if service throws MissingParametersException returns correct errorDetails and http code`() {
        val searchQuery = "my%20search%20query"
        val exceptionMessage = "My exception message"

        val expectedErrorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.MISSING_PARAMETERS,
            "Missing required parameters",
            exceptionMessage
        )
        val expectedErrorDetailsJson = objectMapper.writeValueAsString(expectedErrorDetails)

        every { searchService.searchByName(any(), any()) } throws MissingParametersException(exceptionMessage)

        mockMVc.get("/search/name?q=$searchQuery")
            .andExpect{
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedErrorDetailsJson) }
            }
    }

    @Test
    fun `EmptySearchResultException - if service throws EmptySearchResultException returns correct errorDetails and http code`() {
        val searchQuery = "my%20search%20query"

        val expectedErrorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.NO_SEARCH_RESULTS,
            "No search results",
            "Search returned no results"
        )
        val expectedErrorDetailsJson = objectMapper.writeValueAsString(expectedErrorDetails)

        every { searchService.searchByName(any(), any()) } throws EmptySearchResultException()

        mockMVc.get("/search/name?q=$searchQuery")
            .andExpect{
                status { isNotFound() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedErrorDetailsJson) }
            }
    }

    @Test
    fun `JSoupConnectionException - if service throws JSoupConnectionException returns correct errorDetails and http code`() {
        val searchQuery = "my%20search%20query"
        val exceptionMessage = "My exception message"

        val expectedErrorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.CONNECTION_ERROR,
            "Connection Error",
            "Could not retrieve title HTML information"
        )

        val expectedErrorDetailsJson = objectMapper.writeValueAsString(expectedErrorDetails)

        every { searchService.searchByName(any(), any()) } throws JSoupConnectionException(exceptionMessage)

        mockMVc.get("/search/name?q=$searchQuery")
            .andExpect{
                status { isBadGateway() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedErrorDetailsJson) }
            }
    }

    @Test
    fun `SearchErrorException - if service throws SearchErrorException returns correct errorDetails and http code`() {
        val searchQuery = "my%20search%20query"
        val exceptionMessage = "My exception message"

        val expectedErrorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.SCRAPPING_ERROR,
            "Scrapping Error",
            "A problem occurred consolidating scrapped data."
        )

        val expectedErrorDetailsJson = objectMapper.writeValueAsString(expectedErrorDetails)

        every { searchService.searchByName(any(), any()) } throws SearchScrappingErrorException(exceptionMessage)

        mockMVc.get("/search/name?q=$searchQuery")
            .andExpect{
                status { isServiceUnavailable() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedErrorDetailsJson) }
            }
    }
}