package com.diogo_portela.imdb_scraper.helper

import com.diogo_portela.imdb_scraper.model.*
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

fun generateEpisode(
    episodeScrappedData: EpisodeScrappedData
) =
    generateEpisode(
        imdbId = episodeScrappedData.url!!.removePrefix("/title/").removeSuffix("/"),
        number = episodeScrappedData.number!!.toInt(),
        name = episodeScrappedData.name!!,
        airdate = LocalDate.parse(episodeScrappedData.airdate!!, DateTimeFormatter.ofPattern("d MMM. yyyy")),
        ratingValue = episodeScrappedData.ratingValue!!.toFloat(),
        ratingCount = episodeScrappedData.ratingCount!!.toInt(),
        summary = episodeScrappedData.summary!!
    )

fun generateEpisodeScrappedData(
    url: String = "/title/${generateImdbId()}/",
    number: String = nextInt(1, 25).toString(),
    name: String = "My episode ${nextInt(1, 101)}",
    airdate: String = LocalDate.of(nextInt(1960, 2021), nextInt(1, 13), nextInt(1, 29))
        .format(DateTimeFormatter.ofPattern("d MMM. yyyy")),
    ratingValue: String = (nextFloat() * 10).toString(),
    ratingCount: String = nextInt(1, 1000001).toString(),
    summary: String = "My summary ${nextInt(1, 101)}"
) = EpisodeScrappedData(
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
    episodes: Set<Episode> = (1..numberEpisodes).map { generateEpisode(number = it) }.toSet()
) = Season(
    number = number,
    numberEpisodes = numberEpisodes,
    episodes = episodes
)

fun generateSeason(
    seasonScrappedData: SeasonScrappedData,
    number: Int = nextInt(1, 16),
) = generateSeason(
    number = number,
    numberEpisodes = seasonScrappedData.numberEpisodes!!.toInt(),
)

fun generateSeasonScrappedData(
    numberEpisodes: String? = nextInt(1, 4).toString()
) = SeasonScrappedData(
    numberEpisodes = numberEpisodes
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

fun generateSeries(
    imdbId: String = generateImdbId(),
    seriesScrappedData: SeriesScrappedData
) : Series {
    val linkedData = ApplicationLinkedData.fromJSON(seriesScrappedData.linkedData!!)

    val runtime = seriesScrappedData.runtime!!
    val (startYear, endYear) = if (runtime.contains("–")) {
        val startYear = runtime.substringBefore("–").toInt()
        val endYearStr = runtime.substringAfter("–")
        val endYear = if (endYearStr.isNotBlank()) endYearStr.toInt() else null
        Pair(startYear, endYear)
    } else {
        val year = runtime.toInt()
        Pair(year, year)
    }

    val durationGroups = matchGroupsInRegex(seriesScrappedData.episodeDuration!!.replace(" ", ""), "(?:(\\d+)h)?(?:(\\d+)m)?")!!
    val episodeDuration = Duration
        .ofHours(durationGroups[1].toLongOrNull() ?: 0)
        .plusMinutes(durationGroups[2].toLongOrNull() ?: 0)

    val numberSeasons = seriesScrappedData.numberSeasons!!
        .replace(" Seasons", "")
        .replace(" Season", "")
        .toInt()

    return generateSeries(
        imdbId = imdbId,
        name = linkedData.alternateName ?: linkedData.name,
        originalName = linkedData.alternateName?.run { linkedData.name },
        summary = linkedData.description,
        episodeDuration = episodeDuration,
        startYear = startYear,
        endYear = endYear,
        genres = linkedData.genre,
        ratingValue = linkedData.aggregateRating.ratingValue,
        ratingCount = linkedData.aggregateRating.ratingCount,
        posterURL = linkedData.image,
        numberSeasons = numberSeasons,
        seasons = (1..numberSeasons).map { generateSeason(it) }.toSet()
    )
}

fun generateSeriesScrappedData(
    linkedData : String? = generateApplicationLinkedDataJson(),
    runtime: String? = "${nextInt(1960, 2000)}–${nextInt(2001, 2019)}",
    episodeDuration: String? = "${nextInt(1, 3)}h ${nextInt(1,60)}m",
    numberSeasons: String? = "${nextInt(1, 5)} Seasons"
) = SeriesScrappedData(
    linkedData = linkedData,
    runtime = runtime,
    episodeDuration = episodeDuration,
    numberSeasons = numberSeasons
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