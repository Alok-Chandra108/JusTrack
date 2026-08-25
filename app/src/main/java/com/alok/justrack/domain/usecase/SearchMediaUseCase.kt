package com.alok.justrack.domain.usecase

import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.domain.repository.ExploreRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class SearchMediaUseCase @Inject constructor(
    private val exploreRepository: ExploreRepository
) {
    suspend operator fun invoke(query: String): List<MediaItem> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()

        // 1. Extract potential year (e.g., "Welcome 2007" -> "Welcome", 2007)
        val yearRegex = Regex("\\b(19|20)\\d{2}\\b")
        val yearMatch = yearRegex.find(query)
        val year = yearMatch?.value?.toIntOrNull()
        val cleanQuery = if (year != null) query.replace(yearMatch.value, "").trim() else query

        try {
            // 2. Parallel Targeted Searching
            val multiGlobal = async { 
                exploreRepository.searchMulti(query = query) 
            }
            
            val multiIndia = async { 
                exploreRepository.searchMulti(query = query, region = "IN") 
            }

            val movieYear = if (year != null && cleanQuery.isNotBlank()) async {
                exploreRepository.searchMovie(query = cleanQuery, year = year)
            } else null

            val tvYear = if (year != null && cleanQuery.isNotBlank()) async {
                exploreRepository.searchTv(query = cleanQuery, year = year)
            } else null

            // 3. Collect Results
            val results = mutableListOf<MediaItem>()
            
            // Prioritize year matches if searched with year
            movieYear?.await()?.let { results.addAll(it) }
            tvYear?.await()?.let { results.addAll(it) }
            
            // Add India targeted and Global results
            results.addAll(multiIndia.await())
            results.addAll(multiGlobal.await())

            // 4. Smart Deduplication & Ranking
            val finalItems = results
                .distinctBy { it.id + it.mediaType.name }
                .take(40)

            finalItems
        } catch (e: Exception) {
            emptyList()
        }
    }
}
