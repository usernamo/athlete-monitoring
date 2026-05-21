package com.athlete.monitoring.data

import android.content.Context
import com.athlete.monitoring.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val PREFS = "athlete_api_prefs"
    private const val KEY_BASE_URL = "api_base_url"

    @Volatile
    private var cached: ApiService? = null

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getBaseUrl(): String {
        if (!::appContext.isInitialized) return normalize(BuildConfig.API_BASE_URL)
        val saved = appContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        return normalize(saved ?: BuildConfig.API_BASE_URL)
    }

    fun setBaseUrl(url: String) {
        require(::appContext.isInitialized) { "ApiClient.init() required" }
        val normalized = normalize(url)
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, normalized)
            .apply()
        cached = null
    }

    fun resetCache() {
        cached = null
    }

    fun service(): ApiService {
        val current = cached
        if (current != null) return current
        return synchronized(this) {
            cached ?: createService(getBaseUrl()).also { cached = it }
        }
    }

    private fun createService(baseUrl: String): ApiService {
        val log = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder().addInterceptor(log).build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun normalize(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u"
        }
        if (!u.endsWith("/")) u += "/"
        return u
    }
}
