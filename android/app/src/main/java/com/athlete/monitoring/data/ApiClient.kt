package com.athlete.monitoring.data

import android.content.Context
import com.athlete.monitoring.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val PREFS = "athlete_api_prefs"
    private const val KEY_BASE_URL = "api_base_url"
    private const val KEY_BUILD_URL = "api_build_url"
    private const val KEY_USER_OVERRIDE = "api_user_override"

    @Volatile
    private var cached: ApiService? = null

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        migrateStoredUrl()
    }

    private fun migrateStoredUrl() {
        if (!::appContext.isInitialized) return
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val buildUrl = normalize(BuildConfig.API_BASE_URL)
        val savedBuild = prefs.getString(KEY_BUILD_URL, null)
        val userOverride = prefs.getBoolean(KEY_USER_OVERRIDE, false)

        if (!userOverride || savedBuild != buildUrl) {
            prefs.edit()
                .putString(KEY_BASE_URL, buildUrl)
                .putString(KEY_BUILD_URL, buildUrl)
                .putBoolean(KEY_USER_OVERRIDE, false)
                .apply()
            cached = null
        }
    }

    fun getBaseUrl(): String {
        val buildUrl = normalize(BuildConfig.API_BASE_URL)
        if (!::appContext.isInitialized) return buildUrl
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_BASE_URL, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        return normalize(saved ?: buildUrl)
    }

    fun setBaseUrl(url: String) {
        require(::appContext.isInitialized) { "ApiClient.init() required" }
        val normalized = normalize(url)
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, normalized)
            .putString(KEY_BUILD_URL, normalize(BuildConfig.API_BASE_URL))
            .putBoolean(KEY_USER_OVERRIDE, true)
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
        val client = OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(log)
            .build()
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
            u = "https://$u"
        }
        if (!u.endsWith("/")) u += "/"
        return u
    }
}
