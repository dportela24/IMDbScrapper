package com.diogo_portela.imdb_scraper.advice

import com.diogo_portela.imdb_scraper.model.ErrorDetails
import com.diogo_portela.imdb_scraper.model.exception.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(value = [NotATvSeriesException::class])
    fun handleNotATvSeriesException(req: HttpServletRequest, ex: NotATvSeriesException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.NOT_A_TV_SERIES_ERROR,
            "Title type not valid.",
            ex.message
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [JSoupConnectionException::class])
    fun handleJSoupConnectionException(req: HttpServletRequest, ex: JSoupConnectionException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.CONNECTION_ERROR,
            "Connection Error",
            "Could not retrieve title HTML information"
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_GATEWAY)
    }

    @ExceptionHandler(value = [ErrorBuildingSeriesException::class,
        ErrorBuildingSeasonException::class,
        ErrorBuildingEpisodeException::class
    ])
    fun handleErrorBuildingException(req: HttpServletRequest, ex: BuildingErrorException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.BUILDING_ERROR,
            "Building Error",
            "A problem occurred consolidating the series data."
        )

        return ResponseEntity(errorDetails, HttpStatus.SERVICE_UNAVAILABLE)
    }

    @ExceptionHandler(value = [InvalidImdbIdException::class])
    fun handleInvalidIMDbId(req: HttpServletRequest, ex: InvalidImdbIdException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.INVALID_IMDB_ID,
            "Invalid IMDb Id",
            ex.message
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [TVSeriesNotFoundException::class])
    fun handleTVSeriesNotFound(req: HttpServletRequest, ex: TVSeriesNotFoundException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.TV_SERIES_NOT_FOUND,
            "TV Series not found",
            ex.message
        )

        return ResponseEntity(errorDetails, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(value = [RuntimeException::class])
    fun handleUnknownError(req: HttpServletRequest, ex: RuntimeException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorDetails.ErrorCode.UNEXPECTED_ERROR,
            "Unexpected Error",
            "An unexpected error occurred processing the request..."

        )

        return ResponseEntity(errorDetails, HttpStatus.NOT_FOUND)
    }
}