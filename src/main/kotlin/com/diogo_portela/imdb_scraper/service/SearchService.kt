package com.diogo_portela.imdb_scraper.service

import com.diogo_portela.imdb_scraper.helper.generateErrorMessage
import com.diogo_portela.imdb_scraper.helper.generateSearchUrl
import com.diogo_portela.imdb_scraper.helper.matchGroupsInRegex
import com.diogo_portela.imdb_scraper.model.JSoupConnection
import com.diogo_portela.imdb_scraper.model.SearchResult
import com.diogo_portela.imdb_scraper.model.exception.JSoupConnectionException
import com.diogo_portela.imdb_scraper.model.exception.MissingParametersException
import com.diogo_portela.imdb_scraper.model.exception.SearchErrorException
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SearchService(
    val jSoupConnection: JSoupConnection
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    fun searchByName(name: String?, limit: Int): Set<SearchResult> {
        if (name.isNullOrEmpty()) throw MissingParametersException(listOf("name"))

        val document = fetchSearchResults(name)

        val resultsList = getSearchResults(document, limit)

        return resultsList
            .map { getSearchItem(it) }
            .toSet()
    }

    private fun raiseSearchError(message: String) : SearchErrorException {
        val errorMessage = "Error while performing search. $message"
        logger.error(errorMessage)
        return SearchErrorException(message)
    }

    private fun fetchSearchResults(name: String) : Document {
        return try {
            logger.trace("Making request for season")
            jSoupConnection
                .newConnection(generateSearchUrl(name))
                .get()
        } catch (ex: Exception) {
            val errorMessage = "Could not fetch search results. ${ex.message}"
            logger.error(errorMessage)
            throw JSoupConnectionException(errorMessage)
        }
    }

    private fun getSearchResults(document: Document, limit: Int) =
        document.getElementsByClass("lister-list").first()?.children()?.subList(0, limit)
            ?: throw raiseSearchError("searchResultList")

    private fun getSearchItem(element: Element) : SearchResult {
        val header = element.getElementsByClass("lister-item-header").first()
            ?: throw raiseSearchError(generateErrorMessage("searchResultHeader"))

        val aLink = header.getElementsByTag("a").first()
            ?: throw raiseSearchError(generateErrorMessage("searchResultALink"))

        val url = aLink.attr("href")
        val imdbId = parseImdbId(url)

        val name = aLink.text()
        if (name.isBlank()) throw raiseSearchError(generateErrorMessage("searchResultUrl", name))

        return SearchResult(imdbId, name)
    }

    private fun parseImdbId(url: String) : String {
        val groupValues = try {
            matchGroupsInRegex(url, ".+(tt\\d{7,8}).+")!!
        } catch (_: Exception) {
            throw raiseSearchError(generateErrorMessage("episodeDuration", url))
        }

        return groupValues[1]
    }
}