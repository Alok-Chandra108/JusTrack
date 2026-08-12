package com.alok.justrack.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Matches the "watchlist" table schema in Supabase.
 *
 * Required Supabase table DDL (run in SQL Editor):
 * ─────────────────────────────────────────────────
 * create table public.watchlist (
 *   id            text primary key,
 *   user_id       text,               -- for future auth support
 *   title         text not null,
 *   overview      text default '',
 *   poster_path   text,
 *   backdrop_path text,
 *   rating        double precision default 0.0,
 *   release_date  text default '',
 *   media_type    text not null,
 *   added_at      bigint default extract(epoch from now())::bigint,
 *   is_watched    boolean default false,
 *   in_watchlist  boolean default false
 * );
 *
 * -- Enable Row Level Security (recommended once auth is added)
 * alter table public.watchlist enable row level security;
 * ─────────────────────────────────────────────────
 */
@Serializable
data class SupabaseWatchlistItem(
    @SerialName("id")            val id: String,
    @SerialName("user_id")       val userId: String? = null,
    @SerialName("title")         val title: String,
    @SerialName("overview")      val overview: String = "",
    @SerialName("poster_path")   val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("rating")        val rating: Double = 0.0,
    @SerialName("release_date")  val releaseDate: String = "",
    @SerialName("media_type")    val mediaType: String,
    @SerialName("added_at")      val addedAt: Long = System.currentTimeMillis(),
    @SerialName("is_watched")    val isWatched: Boolean = false,
    @SerialName("in_watchlist")  val inWatchlist: Boolean = false,
    @SerialName("is_watch_later") val isWatchLater: Boolean = false
)
