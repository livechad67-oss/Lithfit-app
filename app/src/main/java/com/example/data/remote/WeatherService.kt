package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class WeatherSnapshot(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val rainProbabilityPct: Int,
    val windSpeedKmh: Double,
    val conditionLabel: String,
    val stylingAdvice: String,
    val forecastSummary: String
)

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
    val current: CurrentWeatherDto? = null,
    val daily: DailyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
    @Json(name = "precipitation_probability") val precipitationProbability: Int? = null,
    @Json(name = "weather_code") val weatherCode: Int? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    @Json(name = "temperature_2m_max") val tempMax: List<Double>? = null,
    @Json(name = "temperature_2m_min") val tempMin: List<Double>? = null,
    @Json(name = "precipitation_probability_max") val precipMax: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResultDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResultDto(
    val name: String? = null,
    val country: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,apparent_temperature,precipitation_probability,weather_code,wind_speed_10m",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        @Query("forecast_days") forecastDays: Int = 3,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoForecastResponse
}

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

class WeatherService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val forecastApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoForecastApi::class.java)

    private val geocodingApi = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoGeocodingApi::class.java)

    suspend fun searchCities(query: String): Result<List<GeocodingResultDto>> =
        withContext(Dispatchers.IO) {
            try {
                if (query.trim().length < 2) return@withContext Result.success(emptyList())
                val response = geocodingApi.searchCity(query.trim())
                Result.success(response.results.orEmpty())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchWeatherForCity(cityQuery: String): Result<WeatherSnapshot> =
        withContext(Dispatchers.IO) {
            try {
                val geo = geocodingApi.searchCity(cityQuery.trim()).results?.firstOrNull()
                    ?: return@withContext Result.failure(IllegalArgumentException("City \"$cityQuery\" not found."))
                val label = listOfNotNull(geo.name, geo.admin1, geo.country).distinct().joinToString(", ")
                fetchWeatherByCoordinates(geo.latitude, geo.longitude, label)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchWeatherByCoordinates(
        latitude: Double,
        longitude: Double,
        locationName: String
    ): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        try {
            val res = forecastApi.getForecast(latitude, longitude)
            val current = res.current ?: return@withContext Result.failure(
                IllegalStateException("Weather service returned no current conditions.")
            )
            val tempC = current.temperature2m ?: 0.0
            val feelsC = current.apparentTemperature ?: tempC
            val rainPct = current.precipitationProbability
                ?: res.daily?.precipMax?.firstOrNull()
                ?: 0
            val wind = current.windSpeed10m ?: 0.0
            val condition = weatherCodeToLabel(current.weatherCode ?: 0)
            val advice = buildStylingAdvice(tempC, rainPct, wind)

            val todayMax = res.daily?.tempMax?.firstOrNull()
            val todayMin = res.daily?.tempMin?.firstOrNull()
            val forecast = if (todayMax != null && todayMin != null) {
                "Today ${todayMin.toInt()}°–${todayMax.toInt()}°C • $rainPct% rain chance"
            } else {
                "$rainPct% rain chance"
            }

            Result.success(
                WeatherSnapshot(
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    temperatureC = tempC,
                    feelsLikeC = feelsC,
                    rainProbabilityPct = rainPct,
                    windSpeedKmh = wind,
                    conditionLabel = condition,
                    stylingAdvice = advice,
                    forecastSummary = forecast
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun weatherCodeToLabel(code: Int): String = when (code) {
        0 -> "Clear sky"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55, 61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Mild"
    }

    private fun buildStylingAdvice(tempC: Double, rainPct: Int, windKmh: Double): String {
        val parts = mutableListOf<String>()
        when {
            tempC >= 24.0 -> parts.add("Breathable cotton or linen silhouettes recommended")
            tempC >= 16.0 -> parts.add("Light single layer or overshirt works well")
            tempC >= 9.0 -> parts.add("Comfortable knitwear and a mid-weight jacket advised")
            else -> parts.add("Warm wool coat or insulated layering recommended")
        }
        if (rainPct >= 40) {
            parts.add("water-resistant outerwear and closed leather/rubber-sole footwear advised")
        }
        if (windKmh >= 22.0) {
            parts.add("wind-blocking outer shell suggested")
        }
        return parts.joinToString("; ") + "."
    }
}
