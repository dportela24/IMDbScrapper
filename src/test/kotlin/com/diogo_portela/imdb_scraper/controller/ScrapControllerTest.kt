package com.diogo_portela.imdb_scraper.controller

import com.diogo_portela.imdb_scraper.helper.generateImdbId
import com.diogo_portela.imdb_scraper.helper.generateName
import com.diogo_portela.imdb_scraper.helper.generateSeries
import com.diogo_portela.imdb_scraper.model.ErrorDetails
import com.diogo_portela.imdb_scraper.model.exception.*
import com.diogo_portela.imdb_scraper.service.SeriesService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(ScrapController::class)
class ScrapControllerTest {
    @TestConfiguration
    class ScrapControllerTestConfig {
        @Bean
        fun service() = mockk<SeriesService>()
    }

    @Autowired
    lateinit var mockMVc: MockMvc

    @Autowired
    lateinit var seriesService: SeriesService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `ScrapByTitle - Happy path - calls seriesService and returns the TV Series scrapped by it`() {
        val imdbId = generateImdbId()
        val expectedSeries = generateSeries(imdbId)
        val expectedSeriesJson = objectMapper.writeValueAsString(expectedSeries)

        every { seriesService.scrapTitleById(imdbId) } returns expectedSeries

        mockMVc.get("/scrap/id/$imdbId")
            .andExpect{
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedSeriesJson) }
            }
    }

    @Test
    fun `ScrapByName - Happy path - calls seriesService and returns the TV Series scrapped by it`() {
        val name = generateName()
        val expectedSeries = generateSeries(name = name)
        val expectedSeriesJson = objectMapper.writeValueAsString(expectedSeries)

        every { seriesService.scrapTitleByName(name) } returns expectedSeries

        mockMVc.get("/scrap/name/$name")
            .andExpect{
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedSeriesJson) }
            }
    }

    @Test
    fun `NotATvSeriesException - if service throws NotATvSeriesException returns correct errorDetails and http code`() {
        val imdbId = generateImdbId()
        val exceptionMessage = "My exception message"

        val expectedErrorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.NOT_A_TV_SERIES_ERROR,
            "Title type not valid.",
            exceptionMessage
        )

        val expectedErrorDetailsJson = objectMapper.writeValueAsString(expectedErrorDetails)

        every { seriesService.scrapTitleById(imdbId) } throws NotATvSeriesException(exceptionMessage)

        mockMVc.get("/scrap/id/$imdbId")
            .andExpect{
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedErrorDetailsJson) }
            }
    }

    @Test
    fun `JSoupConnectionException - if service throws JSoupConnectionException returns correct errorDetails and http code`() {
        val imdbId = generateImdbId()
        val exceptionMessage = "My exception message"

        val expectedErrorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.CONNECTION_ERROR,
            "Connection Error",
            "Could not retrieve title HTML information"
        )

        val expectedErrorDetailsJson = objectMapper.writeValueAsString(expectedErrorDetails)

        every { seriesService.scrapTitleById(imdbId) } throws JSoupConnectionException(exceptionMessage)

        mockMVc.get("/scrap/id/$imdbId")
            .andExpect{
                status { isBadGateway() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedErrorDetailsJson) }
            }
    }

    @Test
    fun `ErrorBuildingSeriesException - if service throws ErrorBuildingSeriesException returns correct errorDetails and http code`() {
        val imdbId = generateImdbId()
        val exceptionMessage = "My exception message"

        val expectedErrorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.SCRAPPING_ERROR,
            "Scrapping Error",
            "A problem occurred consolidating scrapped data."
        )

        val expectedErrorDetailsJson = objectMapper.writeValueAsString(expectedErrorDetails)

        every { seriesService.scrapTitleById(imdbId) } throws SeriesScrappingErrorException(exceptionMessage)

        mockMVc.get("/scrap/id/$imdbId")
            .andExpect{
                status { isServiceUnavailable() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedErrorDetailsJson) }
            }
    }

    @Test
    fun `InvalidImdbIdException - if service throws InvalidImdbIdException returns correct errorDetails and http code`() {
        val imdbId = generateImdbId()
        val exceptionMessage = "My exception message"

        val expectedErrorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.INVALID_IMDB_ID,
            "Invalid IMDb Id",
            exceptionMessage
        )

        val expectedErrorDetailsJson = objectMapper.writeValueAsString(expectedErrorDetails)

        every { seriesService.scrapTitleById(imdbId) } throws InvalidImdbIdException(exceptionMessage)

        mockMVc.get("/scrap/id/$imdbId")
            .andExpect{
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedErrorDetailsJson) }
            }
    }

    @Test
    fun `TVSeriesNotFoundException - if service throws TVSeriesNotFoundException returns correct errorDetails and http code`() {
        val imdbId = generateImdbId()
        val exceptionMessage = "My exception message"

        val expectedErrorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.TV_SERIES_NOT_FOUND,
            "TV Series not found",
            exceptionMessage
        )

        val expectedErrorDetailsJson = objectMapper.writeValueAsString(expectedErrorDetails)

        every { seriesService.scrapTitleById(imdbId) } throws TVSeriesNotFoundException(exceptionMessage)

        mockMVc.get("/scrap/id/$imdbId")
            .andExpect{
                status { isNotFound() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedErrorDetailsJson) }
            }
    }

    @Test
    fun `RuntimeException - if service throws unexpected exception handles it gracefully`() {
        val imdbId = generateImdbId()
        val exceptionMessage = "My exception message"

        val expectedErrorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.UNEXPECTED_ERROR,
            "Unexpected Error",
            "An unexpected error occurred processing the request..."
        )

        val expectedErrorDetailsJson = objectMapper.writeValueAsString(expectedErrorDetails)

        every { seriesService.scrapTitleById(imdbId) } throws RuntimeException(exceptionMessage)

        mockMVc.get("/scrap/id/$imdbId")
            .andExpect{
                status { isServiceUnavailable() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedErrorDetailsJson) }
            }
    }
}