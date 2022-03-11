package com.diogo_portela.imdb_scraper.service

import com.diogo_portela.imdb_scraper.helper.generateErrorMessage
import com.diogo_portela.imdb_scraper.helper.generateSearchUrl
import com.diogo_portela.imdb_scraper.helper.matchGroupsInRegex
import com.diogo_portela.imdb_scraper.model.JSoupConnection
import com.diogo_portela.imdb_scraper.model.SearchResult
import com.diogo_portela.imdb_scraper.model.exception.EmptySearchResultException
import com.diogo_portela.imdb_scraper.model.exception.JSoupConnectionException
import com.diogo_portela.imdb_scraper.model.exception.MissingParametersException
import com.diogo_portela.imdb_scraper.model.exception.SearchScrappingErrorException
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SearchService(
    val jSoupConnection: JSoupConnection
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    fun searchByName(searchInput: String?, limit: Int): Set<SearchResult> {
        if (searchInput.isNullOrEmpty()) throw MissingParametersException(listOf("searchInput"))

        val doc = fetchSearchResults(searchInput)

        val resultsList = getSearchResults(doc, limit)

        return resultsList
            .map { getSearchItem(it) }
            .toSet()
    }

    private fun raiseSearchError(message: String) : SearchScrappingErrorException {
        val errorMessage = "Error while performing search. $message"
        logger.error(errorMessage)
        return SearchScrappingErrorException(message)
    }

    private fun fetchSearchResults(searchInput: String) : Document {
        return try {
            logger.trace("Making request for season")
            jSoupConnection
                .newConnection(generateSearchUrl(searchInput))
                .get()
        } catch (ex: Exception) {
            val errorMessage = "Could not fetch search results. ${ex.message}"
            logger.error(errorMessage)
            throw JSoupConnectionException(errorMessage)
        }
    }

    private fun getSearchResults(doc: Document, limit: Int) : List<Element> {
        val resultsList = doc.getElementsByClass("lister-list").first()?.children()
            ?: throw EmptySearchResultException()

        return if (resultsList.size > limit) {
            resultsList.subList(0, limit)
        } else {
            resultsList
        }
    }

    private fun getSearchItem(element: Element) : SearchResult {
        val header = element.getElementsByClass("lister-item-header").first()
            ?: throw raiseSearchError(generateErrorMessage("searchResultHeader"))

        val aLink = header.getElementsByTag("a").first()
            ?: throw raiseSearchError(generateErrorMessage("searchResultALink"))

        val url = aLink.attr("href")
        val imdbId = parseImdbId(url)

        val name = aLink.text()
        if (name.isBlank()) throw raiseSearchError(generateErrorMessage("searchResultName", name))

        return SearchResult(imdbId, name)
    }

    private fun parseImdbId(url: String) : String {
        val groupValues = try {
            matchGroupsInRegex(url, ".+(tt\\d{7,8}).+")!!
        } catch (_: Exception) {
            throw raiseSearchError(generateErrorMessage("searchResultImdbId", url))
        }

        return groupValues[1]
    }
}