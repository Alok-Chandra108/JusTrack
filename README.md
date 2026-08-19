# JusTrack

JusTrack is a modern Android application built with Jetpack Compose designed to help users track their favorite movies and TV shows. It integrates with The Movie Database (TMDB) for rich media content and uses Supabase for cloud synchronization and authentication.

## 🚀 Features

- **Search & Explore**: Discover movies, TV shows, and people.
- **Watchlist & Favourites**: Keep track of what you want to watch and your all-time favorites.
- **Upcoming Shows**: Stay updated with upcoming episodes of your tracked shows.
- **Cloud Sync**: Sync your watchlist and progress across devices using Supabase.
- **Rich Details**: View detailed information about media, including cast, crew, ratings, and trailers.
- **Modern UI**: Fully built with Jetpack Compose and Material 3 for a sleek, responsive experience.

## 🛠 Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose), Material 3
- **Navigation**: Compose Navigation with Hilt integration
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Local Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & OkHttp
- **Backend as a Service**: [Supabase](https://supabase.com/) (Postgrest, Auth, Realtime)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **JSON Serialization**: [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
- **Animations**: Konfetti for celebratory effects

## 📦 Project Structure

- `com.alok.justrack.data`: Data layer containing Room database, TMDB API services, Supabase integration, and repositories.
- `com.alok.justrack.ui`: UI layer with screens, ViewModels, theme, and reusable components.
- `com.alok.justrack.di`: Hilt modules for dependency injection.
- `com.alok.justrack.util`: Utility classes and constants.

## 🚦 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/justrack.git
   ```
2. **Setup Secrets**:
   Create a `secrets.properties` file in the root directory and add your API keys:
   ```properties
   TMDB_API_KEY=your_tmdb_api_key
   SUPABASE_URL=your_supabase_url
   SUPABASE_ANON_KEY=your_supabase_anon_key
   ```
3. **Build & Run**:
   Open the project in Android Studio and run the `:app` module.

## 📝 Recent Updates
- Fixed poster loading issues in the Upcoming Shows tab by correctly prepending TMDB image base URLs in `TmdbMediaRepository`.
- Resolved compilation issues and stabilized build configuration.
