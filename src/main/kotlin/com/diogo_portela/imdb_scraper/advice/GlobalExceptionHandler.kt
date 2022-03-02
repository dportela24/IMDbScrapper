package com.diogo_portela.imdb_scraper.advice

import com.diogo_portela.imdb_scraper.model.ErrorDetails
import com.diogo_portela.imdb_scraper.model.ErrorDetails.ErrorCode
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
            ErrorCode.NOT_A_TV_SERIES_ERROR,
            "Title type not valid.",
            ex.message
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [JSoupConnectionException::class])
    fun handleJSoupConnectionException(req: HttpServletRequest, ex: NotATvSeriesException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorCode.CONNECTION_ERROR,
            "Connection Error",
            "Could not retrieve title information"
        )

        return ResponseEntity(errorDetails, HttpStatus.BAD_GATEWAY)
    }

    @ExceptionHandler(value = [ErrorBuildingSeriesException::class,
        ErrorBuildingSeasonException::class,
        ErrorBuildingEpisodeException::class
    ])
    fun handleErrorBuildingException(req: HttpServletRequest, ex: BuildingErrorException) : ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            ErrorCode.BUILDING_ERROR,
            "Building Error",
            "A problem occurred consolidating the series data."
        )

        return ResponseEntity(errorDetails, HttpStatus.SERVICE_UNAVAILABLE)
    }
}