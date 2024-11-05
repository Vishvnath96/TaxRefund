package com.interview.taxrefund.core.di

import com.interview.taxrefund.core.network.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

//# API and network configurations
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        securityManager: SecurityManager
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(securityManager))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideRefundApi(retrofit: Retrofit): RefundApi =
        retrofit.create(RefundApi::class.java)

    @Provides
    @Singleton
    fun providePredictionApi(retrofit: Retrofit): PredictionApi =
        retrofit.create(PredictionApi::class.java)
}