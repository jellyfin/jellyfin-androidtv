package org.jellyfin.androidtv.data.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class JellyseerrSearchResult(
	val results: List<JellyseerrSearchItem>,
	val page: Int,
	val totalPages: Int,
	val totalResults: Int,
)

// Genre für Slider/Cards mit Backdrop-Bild
data class JellyseerrGenreSlider(
	val id: Int,
	val name: String,
	val backdropUrl: String?,
)

data class JellyseerrCompany(
	val id: Int,
	val name: String,
	val logoUrl: String? = null,
	val homepage: String? = null,
	val description: String? = null,
)

data class JellyseerrSearchItem(
	val id: Int,
	val mediaType: String,
	val title: String,
	val overview: String?,
	val posterPath: String? = null,
	val backdropPath: String? = null,
	val profilePath: String? = null,
	val releaseDate: String? = null,
	val isRequested: Boolean = false,
	val isAvailable: Boolean = false,
	val isPartiallyAvailable: Boolean = false,
	val requestId: Int? = null,
	val requestStatus: Int? = null,
	val jellyfinId: String? = null,
)

@Serializable
data class JellyseerrRequestPage(
	val results: List<JellyseerrRequestDto>,
)

@Serializable
data class JellyseerrRequestDto(
	val id: Int,
	val status: Int? = null,
	val type: String? = null,
	val media: JellyseerrMediaDto? = null,
)

@Serializable
data class JellyseerrMediaDto(
	val tmdbId: Int? = null,
	val mediaType: String? = null,
	val title: String? = null,
	val name: String? = null,
	val posterPath: String? = null,
	val backdropPath: String? = null,
)

@Serializable
data class JellyseerrPersonDetails(
	val id: Int,
	val name: String,
	val birthday: String? = null,
	val deathday: String? = null,
	val knownForDepartment: String? = null,
	val alsoKnownAs: List<String> = emptyList(),
	val gender: Int? = null,
	val biography: String? = null,
	val popularity: Double? = null,
	val placeOfBirth: String? = null,
	val profilePath: String? = null,
	val adult: Boolean? = null,
	val imdbId: String? = null,
	val homepage: String? = null,
)

data class JellyseerrRequest(
	val id: Int,
	val status: Int?,
	val mediaType: String?,
	val title: String,
	val tmdbId: Int?,
	val posterPath: String? = null,
	val backdropPath: String? = null,
)

@Serializable
data class JellyseerrCreateRequestBody(
	val mediaType: String,
	@SerialName("mediaId")
	val mediaId: Int,
	val userId: Int,
	val seasons: List<Int>? = null,
)

@Serializable
data class JellyseerrGenre(
	val id: Int,
	val name: String,
)

data class JellyseerrGenreDiscovery(
	val genre: JellyseerrGenre,
	val results: List<JellyseerrSearchItem>,
	val page: Int,
	val totalPages: Int,
	val totalResults: Int,
)

data class JellyseerrCompanyDiscovery(
	val company: JellyseerrCompany,
	val results: List<JellyseerrSearchItem>,
	val page: Int,
	val totalPages: Int,
	val totalResults: Int,
)

@Serializable
data class JellyseerrCast(
	val id: Int,
	val castId: Int? = null,
	val character: String? = null,
	val creditId: String? = null,
	val gender: Int? = null,
	val name: String,
	val order: Int? = null,
	val profilePath: String? = null,
)

@Serializable
data class JellyseerrCredits(
	val cast: List<JellyseerrCast> = emptyList(),
)

@Serializable
data class JellyseerrPersonCredit(
	val id: Int,
	val mediaType: String? = null,
	val overview: String? = null,
	val posterPath: String? = null,
	val backdropPath: String? = null,
	val title: String? = null,
	val name: String? = null,
	val adult: Boolean? = null,
)

@Serializable
data class JellyseerrCombinedCredits(
	val id: Int,
	val cast: List<JellyseerrPersonCredit> = emptyList(),
	val crew: List<JellyseerrPersonCredit> = emptyList(),
)

@Serializable
data class JellyseerrGenreSliderItem(
	val id: Int,
	val name: String,
	val backdrops: List<String> = emptyList(),
)

@Serializable
data class JellyseerrSeason(
	val id: Int,
	val name: String? = null,
	val overview: String? = null,
	val posterPath: String? = null,
	val seasonNumber: Int,
	val episodeCount: Int? = null,
	val airDate: String? = null,
	val status: Int? = null,
)

data class JellyseerrEpisode(
	val id: Int,
	val name: String? = null,
	val overview: String? = null,
	val imageUrl: String? = null,
	val episodeNumber: Int? = null,
	val seasonNumber: Int? = null,
	val airDate: String? = null,
	val isMissing: Boolean = false,
	val isAvailable: Boolean = false,
	val jellyfinId: String? = null,
)

@Serializable
data class JellyseerrMovieDetails(
	val id: Int,
	val title: String? = null,
	val name: String? = null,
	val originalTitle: String? = null,
	val overview: String? = null,
	val posterPath: String? = null,
	val backdropPath: String? = null,
	val releaseDate: String? = null,
	val firstAirDate: String? = null,
	val runtime: Int? = null,
	val voteAverage: Double? = null,
	val genres: List<JellyseerrGenre> = emptyList(),
	val credits: JellyseerrCredits? = null,
	val seasons: List<JellyseerrSeason> = emptyList(),
)
