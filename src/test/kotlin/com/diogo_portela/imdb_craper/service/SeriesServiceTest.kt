package com.diogo_portela.imdb_craper.service

import com.diogo_portela.imdb_craper.helper.generateApplicationLinkedDataJson
import com.diogo_portela.imdb_craper.helper.generateImdbId
import com.diogo_portela.imdb_craper.model.JSoupConnection
import com.diogo_portela.imdb_craper.model.exception.NotATvSeriesException
import io.mockk.every
import io.mockk.mockk
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SeriesServiceTest {
    val jSoupConnection = mockk<JSoupConnection>()
    val seasonService = mockk<SeasonService>()
    val doc = mockk<Document>()

    val subject = SeriesService(jSoupConnection, seasonService)

    @Test
    fun `If title not a TV Series throws NotATVSeriesException`() {
        val imdbID = generateImdbId()
        val titleType = "Movie"
        every { jSoupConnection.newConnection(any()).get() } returns doc
        every { doc.getElementsByAttributeValueStarting("type", "application/ld+json").first()?.data() } returns generateApplicationLinkedDataJson(type = titleType)

        val ex = assertThrows<NotATvSeriesException> { subject.scrapTitle(imdbID) }

        assertTrue(ex.message.contains(imdbID) && ex.message.contains(titleType))
    }
}