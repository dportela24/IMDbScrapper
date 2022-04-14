package com.diogo_portela.imdb_scraper.advice

import com.diogo_portela.imdb_scraper.model.ErrorDetails
import com.diogo_portela.imdb_scraper.model.exception.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(value = [NoHandlerFoundException::class])
    fun handleNoHandlerFoundException(req: HttpServletRequest,
                                      ex : NoHandlerFoundException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.NO_ENDPOINT_HANDLER,
            "Endpoint not implemented",
            "The requested endpoint is not implemented"
        )

        return ResponseEntity(errorDetails, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(value = [HttpRequestMethodNotSupportedException::class])
    fun handleHttpRequestMethodNotSupported(req: HttpServletRequest,
                                            ex : HttpRequestMethodNotSupportedException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.NO_HTTP_METHOD,
            "Method not found",
            "The requested method is not implemented for this endpoint"
        )

        return ResponseEntity(errorDetails, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(value = [NotATvSeriesException::class])
    fun handleNotATvSeriesException(req: HttpServletRequest,
                                    ex: NotATvSeriesException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.NOT_A_TV_SERIES_ERROR,
            "Title type not valid.",
            ex.message
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [JSoupConnectionException::class])
    fun handleJSoupConnectionException(req: HttpServletRequest,
                                       ex: JSoupConnectionException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.CONNECTION_ERROR,
            "Connection Error",
            "Could not retrieve title HTML information"
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_GATEWAY)
    }

    @ExceptionHandler(value = [SeriesScrappingErrorException::class,
        SeasonScrappingErrorException::class,
        EpisodeScrappingErrorException::class,
        SearchScrappingErrorException::class
    ])
    fun handleErrorBuildingException(req: HttpServletRequest,
                                     ex: ScrappingErrorException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.SCRAPPING_ERROR,
            "Scrapping Error",
            "A problem occurred consolidating scrapped data."
        )

        return ResponseEntity(errorDetails, HttpStatus.SERVICE_UNAVAILABLE)
    }

    @ExceptionHandler(value = [InvalidImdbIdException::class])
    fun handleInvalidIMDbId(req: HttpServletRequest,
                            ex: InvalidImdbIdException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.INVALID_IMDB_ID,
            "Invalid IMDb Id",
            ex.message
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [TVSeriesNotFoundException::class])
    fun handleTVSeriesNotFound(req: HttpServletRequest,
                               ex: TVSeriesNotFoundException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.TV_SERIES_NOT_FOUND,
            "TV Series not found",
            ex.message
        )

        return ResponseEntity(errorDetails, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(value = [RuntimeException::class])
    fun handleUnknownError(req: HttpServletRequest,
                           ex: RuntimeException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.UNEXPECTED_ERROR,
            "Unexpected Error",
            "An unexpected error occurred processing the request..."
        )

        return ResponseEntity(errorDetails, HttpStatus.SERVICE_UNAVAILABLE)
    }

    @ExceptionHandler(value = [MissingParametersException::class])
    fun handleUnknownError(req: HttpServletRequest,
                           ex: MissingParametersException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.MISSING_PARAMETERS,
            "Missing required parameters",
            ex.message
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [EmptySearchResultException::class])
    fun handleEmptySearchResult(req: HttpServletRequest,
                                ex: EmptySearchResultException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.NO_SEARCH_RESULTS,
            "No search results",
            "Search returned no results"
        )

        return ResponseEntity(errorDetails, HttpStatus.NOT_FOUND)
    }
}