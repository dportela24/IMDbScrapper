package com.diogo_portela.imdb_craper.helper

import com.diogo_portela.imdb_craper.model.*
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random.Default.nextFloat
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

fun generateImdbId() = "tt" + nextInt(1000000, 9999999)

fun generateEpisode(
    imdbId: String = generateImdbId(),
    number: Int = nextInt(1, 25),
    name: String = "My episode ${nextInt(1, 101)}",
    airdate: LocalDate = LocalDate.of(nextInt(1960, 2021), nextInt(1, 13), nextInt(1, 29)),
    ratingValue: Float = nextFloat() * 10,
    ratingCount: Int = nextInt(1, 1000001),
    summary: String = "My summary ${nextInt(1, 101)}"
) = Episode(
    imdbId = imdbId,
    number = number,
    name = name,
    airdate = airdate,
    ratingValue = ratingValue,
    ratingCount = ratingCount,
    summary = summary
)

fun generateEpisode(episodeElementData: EpisodeElementData) =
    generateEpisode(
        imdbId = episodeElementData.url!!.removePrefix("/title/").removeSuffix("/"),
        number = episodeElementData.number!!.toInt(),
        name = episodeElementData.name!!,
        airdate = LocalDate.parse(episodeElementData.airdate!!, DateTimeFormatter.ofPattern("d MMM. yyyy")),
        ratingValue = episodeElementData.ratingValue!!.toFloat(),
        ratingCount = episodeElementData.ratingCount!!.toInt(),
        summary = episodeElementData.summary!!
    )

fun generateEpisodeElementData(
    url: String = "/title/${generateImdbId()}/",
    number: String = nextInt(1, 25).toString(),
    name: String = "My episode ${nextInt(1, 101)}",
    airdate: String = LocalDate.of(nextInt(1960, 2021), nextInt(1, 13), nextInt(1, 29))
        .format(DateTimeFormatter.ofPattern("d MMM. yyyy")),
    ratingValue: String = (nextFloat() * 10).toString(),
    ratingCount: String = nextInt(1, 1000001).toString(),
    summary: String = "My summary ${nextInt(1, 101)}"
) = EpisodeElementData(
    url = url,
    number = number,
    name = name,
    airdate = airdate,
    ratingValue = ratingValue,
    ratingCount = ratingCount,
    summary = summary
)

fun generateSeason(
    number: Int = nextInt(1, 16),
    numberEpisodes: Int = nextInt(1, 4),
    episodes: Set<Episode> = (1..numberEpisodes).map { generateEpisode() }.toSet()
) = Season(
    number = number,
    numberEpisodes = numberEpisodes,
    episodes = episodes
)

fun generateSeries(
    imdbId: String = generateImdbId(),
    name: String = "My series ${nextInt(1, 101)}",
    originalName: String? = null,
    summary: String = "My summary ${nextInt(1, 101)}",
    episodeDuration: Duration = Duration.ofMinutes(nextLong(20, 90)),
    startYear: Int = nextInt(1960, 2020),
    endYear: Int? = null,
    genres: Set<String> = setOf("Comedy", "Drama", "Romance"),
    ratingValue: Float = nextFloat() * 10,
    ratingCount: Int = nextInt(1, 1000001),
    posterURL: String = "My poster ${nextInt(1, 101)}",
    numberSeasons: Int = nextInt(1, 4),
    seasons: Set<Season> = (1..numberSeasons).map { generateSeason() }.toSet()
) = Series(
    imdbId = imdbId,
    name = name,
    originalName = originalName,
    summary = summary,
    episodeDuration = episodeDuration,
    startYear = startYear,
    endYear = endYear,
    genres = genres,
    ratingValue = ratingValue,
    ratingCount = ratingCount,
    posterURL = posterURL,
    numberSeasons = numberSeasons,
    seasons = seasons
)

fun generateApplicationLinkedData(
    type: String = "TVSeries",
    name: String = "My TV Show",
    alternateName: String? = "My Alternate TV Show",
    image: String = "http://myposter.com",
    description: String = "A simple TV Show",
    aggregateRating: ApplicationLinkedData.AggregateRating = generateAggregasteRating(),
    genre: Set<String> = setOf("Comedy", "Drama", "Romance")
) = ApplicationLinkedData(
    type = type,
    name = name,
    alternateName = alternateName,
    image = image,
    description = description,
    aggregateRating = aggregateRating,
    genre = genre
)

fun generateAggregasteRating(
    ratingCount: Int = 1234,
    ratingValue: Float = 8.2F
) = ApplicationLinkedData.AggregateRating(
    ratingCount = ratingCount,
    ratingValue = ratingValue
)