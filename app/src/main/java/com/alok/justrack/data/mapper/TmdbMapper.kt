package com.alok.justrack.data.mapper

import com.alok.justrack.data.api.*
import com.alok.justrack.data.model.*
import com.alok.justrack.util.Constants
import java.time.format.DateTimeFormatter
import java.util.Locale

object TmdbMapper {

    fun TmdbMediaDto.toMediaItem(fallbackType: MediaType? = null): MediaItem {
        val detectedType = when {
            mediaType == "tv" -> MediaType.TV
            mediaType == "movie" -> MediaType.MOVIE
            fallbackType != null -> fallbackType
            name != null && title == null -> MediaType.TV
            name != null && firstAirDate != null -> MediaType.TV
            else -> MediaType.MOVIE
        }
        val displayTitle = title ?: name ?: "Untitled"
        val rawDate = releaseDate ?: firstAirDate ?: ""
        val posterUrl = posterPath?.let { 
            if (it.startsWith("http")) it else "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" 
        }
        val backdropUrl = backdropPath?.let { 
            if (it.startsWith("http")) it else "${Constants.TMDB_IMAGE_BASE_URL_W780}$it" 
        }
        
        return MediaItem(
            id = id.toString(),
            title = displayTitle,
            overview = overview ?: "",
            posterPath = posterUrl,
            backdropPath = backdropUrl,
            rating = voteAverage?.let { Math.round(it * 10) / 10.0 } ?: 0.0,
            releaseDate = rawDate,
            mediaType = detectedType
        )
    }

    fun TmdbMediaDto.toMovieDetails(type: MediaType): MovieDetails {
        val detectedType = when {
            mediaType == "tv" -> MediaType.TV
            mediaType == "movie" -> MediaType.MOVIE
            name != null && title == null -> MediaType.TV
            name != null && firstAirDate != null -> MediaType.TV
            else -> type
        }
        val displayTitle = title ?: name ?: "Untitled"
        val rawDate = releaseDate ?: firstAirDate ?: ""
        val posterUrl = posterPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" }
        val backdropUrl = backdropPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W780}$it" }

        val runtimeStr = when {
            runtime != null -> "${runtime / 60}h ${runtime % 60}m"
            episodeRunTime != null && episodeRunTime.isNotEmpty() -> "${episodeRunTime.first()}m"
            else -> "-"
        }

        val castMembers = credits?.cast?.map {
            CastMember(
                id = it.id.toString(),
                name = it.name,
                character = it.character,
                profilePath = it.profilePath?.let { path -> "${Constants.TMDB_IMAGE_BASE_URL_W185}$path" }
            )
        } ?: emptyList()

        val directorPeople = when (detectedType) {
            MediaType.MOVIE -> credits?.crew?.filter { it.job == "Director" }?.map { Person(it.id.toString(), it.name) }?.distinctBy { it.id } ?: emptyList()
            MediaType.TV -> {
                val creators = createdBy?.map { Person(it.id.toString(), it.name) }?.distinctBy { it.id }
                if (!creators.isNullOrEmpty()) creators
                else credits?.crew?.filter { it.job == "Executive Producer" }?.map { Person(it.id.toString(), it.name) }?.distinctBy { it.id } ?: emptyList()
            }
        }.ifEmpty { listOf(Person("-1", "-")) }

        val cert = when (detectedType) {
            MediaType.MOVIE -> {
                val results = releaseDates?.results
                val usCert = results?.find { it.iso31661 == "US" }?.releaseDates?.firstOrNull { it.certification.isNotBlank() }?.certification
                val gbCert = results?.find { it.iso31661 == "GB" }?.releaseDates?.firstOrNull { it.certification.isNotBlank() }?.certification
                usCert ?: gbCert ?: results?.flatMap { it.releaseDates }?.firstOrNull { it.certification.isNotBlank() }?.certification ?: "-"
            }
            MediaType.TV -> {
                val results = contentRatings?.results
                val usCert = results?.find { it.iso31661 == "US" }?.rating
                val gbCert = results?.find { it.iso31661 == "GB" }?.rating
                usCert ?: gbCert ?: results?.firstOrNull { it.rating.isNotBlank() }?.rating ?: "-"
            }
        }

        return MovieDetails(
            id = id.toString(),
            title = displayTitle,
            overview = overview ?: "",
            posterPath = posterUrl,
            backdropPath = backdropUrl,
            rating = voteAverage?.let { Math.round(it * 10) / 10.0 } ?: 0.0,
            releaseDate = formatDate(rawDate),
            rawReleaseDate = rawDate,
            runtime = runtimeStr,
            certification = cert,
            director = directorPeople,
            originalLanguage = originalLanguage ?: "en",
            status = status ?: "",
            numberOfEpisodes = numberOfEpisodes ?: 0,
            mediaType = detectedType,
            cast = castMembers,
            ratings = listOf(
                RatingSource("TMDb", String.format(Locale.US, "%.1f", voteAverage ?: 0.0))
            ),
            recommendations = recommendations?.results?.map { it.toMediaItem() } ?: emptyList(),
            seasons = seasons?.map { it.toSeason(emptySet()) } ?: emptyList(),
            watchProviders = watchProviders.toWatchProviders()
        )
    }

    fun TmdbSeasonDto.toSeason(watchedEpisodes: Set<String>): Season {
        val mappedEpisodes = episodes?.map { it.toEpisode(watchedEpisodes.contains("S${it.seasonNumber}E${it.episodeNumber}")) } ?: emptyList()
        return Season(
            id = id.toString(),
            name = name,
            overview = overview ?: "",
            posterPath = posterPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" },
            seasonNumber = seasonNumber,
            episodeCount = if (episodeCount != null && episodeCount > 0) episodeCount else mappedEpisodes.size,
            airDate = airDate,
            episodes = mappedEpisodes
        )
    }

    fun TmdbEpisodeDto.toEpisode(isWatched: Boolean): Episode {
        return Episode(
            id = id.toString(),
            name = name,
            overview = overview ?: "",
            stillPath = stillPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W300}$it" },
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            airDate = airDate,
            voteAverage = voteAverage ?: 0.0,
            isWatched = isWatched
        )
    }

    fun TmdbEpisodeDto.toEntity(showId: String): com.alok.justrack.data.db.EpisodeEntity {
        return com.alok.justrack.data.db.EpisodeEntity(
            showId = showId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            title = name,
            overview = overview,
            airDate = airDate,
            stillPath = stillPath,
            voteAverage = voteAverage
        )
    }

    fun TmdbPersonDto.toPersonDetails(): PersonDetails {
        val movies = (movieCredits?.cast ?: emptyList()) + (movieCredits?.crew ?: emptyList())
        val tv = (tvCredits?.cast ?: emptyList()) + (tvCredits?.crew ?: emptyList())
        
        return PersonDetails(
            id = id.toString(),
            name = name,
            biography = biography ?: "",
            birthday = birthday,
            placeOfBirth = placeOfBirth,
            profilePath = profilePath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" },
            knownForDepartment = knownForDepartment ?: "",
            movieCredits = movies.distinctBy { it.id }.map { it.toMediaItem(MediaType.MOVIE) },
            tvCredits = tv.distinctBy { it.id }.map { it.toMediaItem(MediaType.TV) }
        )
    }

    private fun formatDate(dateStr: String): String {
        val date = com.alok.justrack.util.DateUtils.parseDate(dateStr) ?: return dateStr.ifBlank { "-" }
        return try {
            date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun TmdbWatchProvidersResponse?.toWatchProviders(): WatchProviders? {
        if (this == null || results == null) return null
        
        val countryCode = Locale.getDefault().country
        val regionResult = results[countryCode] ?: results["US"] ?: return null

        return WatchProviders(
            stream = regionResult.flatrate?.map { it.toWatchProvider() } ?: emptyList(),
            rent = regionResult.rent?.map { it.toWatchProvider() } ?: emptyList(),
            buy = regionResult.buy?.map { it.toWatchProvider() } ?: emptyList()
        )
    }

    private fun TmdbWatchProviderItem.toWatchProvider() = WatchProvider(
        id = providerId,
        name = providerName,
        logoUrl = logoPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W154}$it" } ?: ""
    )
}
