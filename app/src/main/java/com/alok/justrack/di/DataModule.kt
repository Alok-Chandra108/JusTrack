package com.alok.justrack.di

import android.content.Context
import androidx.room.Room
import com.alok.justrack.BuildConfig
import com.alok.justrack.data.api.TmdbApiService
import com.alok.justrack.data.db.AppDatabase
import com.alok.justrack.data.db.FavouriteDao
import com.alok.justrack.data.db.ListDao
import com.alok.justrack.data.db.WatchlistDao
import com.alok.justrack.data.repository.MediaRepository
import com.alok.justrack.data.repository.TmdbMediaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        tmdbMediaRepository: TmdbMediaRepository
    ): MediaRepository

    companion object {

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "justrack.db"
            ).fallbackToDestructiveMigration().build()
        }

        @Provides
        @Singleton
        fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()

        @Provides
        @Singleton
        fun provideFavouriteDao(db: AppDatabase): FavouriteDao = db.favouriteDao()

        @Provides
        @Singleton
        fun provideListDao(db: AppDatabase): ListDao = db.listDao()

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            val authInterceptor = Interceptor { chain ->
                val originalRequest = chain.request()
                val newUrl = originalRequest.url.newBuilder()
                    .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                    .build()
                chain.proceed(originalRequest.newBuilder().url(newUrl).build())
            }
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            return OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()
        }

        @Provides
        @Singleton
        fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
            return Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        @Provides
        @Singleton
        fun provideTmdbApiService(retrofit: Retrofit): TmdbApiService {
            return retrofit.create(TmdbApiService::class.java)
        }
    }
}
