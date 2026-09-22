package com.example.model

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class LiveWeatherInfo(
    val cityName: String = "Detecting...",
    val temperature: String = "28°",
    val temperatureValue: Double = 28.0,
    val feelsLike: String = "29°C",
    val humidity: String = "55%",
    val windSpeed: String = "10 km/h",
    val condition: String = "Cloudy",
    val weatherCode: Int = 3,
    val icon: String = "☁️",
    val latitude: Double = 28.6139,
    val longitude: Double = 77.2090,
    val isGpsAccurate: Boolean = false,
    val summaryHinglish: String = "Mausam theek hai."
)

object LocationWeatherManager {
    private const val TAG = "LocationWeather"

    private val _weatherState = MutableStateFlow(LiveWeatherInfo())
    val weatherState: StateFlow<LiveWeatherInfo> = _weatherState.asStateFlow()

    private var lastFetchTime = 0L

    suspend fun refreshWeather(context: Context, force: Boolean = false): LiveWeatherInfo {
        val now = System.currentTimeMillis()
        if (!force && now - lastFetchTime < 60_000 && _weatherState.value.cityName != "Detecting...") {
            return _weatherState.value
        }

        return withContext(Dispatchers.IO) {
            try {
                // 1. Fetch Location (GPS / Network / IP Fallback)
                val loc = determineCurrentLocation(context)
                val lat = loc.first
                val lon = loc.second
                var cityName = loc.third

                // If cityName is unknown or empty, do reverse geocoding
                if (cityName.isEmpty() || cityName == "Unknown") {
                    cityName = reverseGeocode(context, lat, lon)
                }

                // 2. Fetch Live Weather from Open-Meteo for exact Lat/Lon
                val weather = fetchWeatherForCoordinates(cityName, lat, lon, isGps = loc.fourth)
                _weatherState.value = weather
                lastFetchTime = now
                weather
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing weather", e)
                _weatherState.value
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun determineCurrentLocation(context: Context): Quadruple<Double, Double, String, Boolean> {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (lm != null) {
                    var bestLoc: Location? = null

                    val providers = listOf(
                        LocationManager.GPS_PROVIDER,
                        LocationManager.NETWORK_PROVIDER,
                        LocationManager.PASSIVE_PROVIDER
                    )

                    for (provider in providers) {
                        try {
                            if (lm.isProviderEnabled(provider)) {
                                val loc = lm.getLastKnownLocation(provider)
                                if (loc != null) {
                                    if (bestLoc == null || loc.accuracy < bestLoc.accuracy || loc.time > bestLoc.time) {
                                        bestLoc = loc
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Provider $provider not accessible: ${e.message}")
                        }
                    }

                    if (bestLoc != null) {
                        val city = reverseGeocode(context, bestLoc.latitude, bestLoc.longitude)
                        Log.i(TAG, "Device GPS location found: ${bestLoc.latitude}, ${bestLoc.longitude} ($city)")
                        return Quadruple(bestLoc.latitude, bestLoc.longitude, city, true)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "GPS location lookup failed: ${e.message}")
            }
        }

        // 3. Fallback: IP-based Geolocation (Extremely accurate city location without waiting for GPS fix)
        return try {
            val ipLoc = fetchIpGeolocation()
            if (ipLoc != null) {
                Log.i(TAG, "IP Geolocation location found: ${ipLoc.first}, ${ipLoc.second} (${ipLoc.third})")
                Quadruple(ipLoc.first, ipLoc.second, ipLoc.third, false)
            } else {
                Quadruple(28.6139, 77.2090, "New Delhi", false)
            }
        } catch (e: Exception) {
            Quadruple(28.6139, 77.2090, "New Delhi", false)
        }
    }

    private fun reverseGeocode(context: Context, lat: Double, lon: Double): String {
        return try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Modern Geocoder API
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        formatAddress(addresses[0])
                    } else "Local Area"
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        formatAddress(addresses[0])
                    } else "Local Area"
                }
            } else {
                "Local Area"
            }
        } catch (e: Exception) {
            "Local Area"
        }
    }

    private fun formatAddress(addr: Address): String {
        return when {
            !addr.locality.isNullOrEmpty() -> addr.locality
            !addr.subAdminArea.isNullOrEmpty() -> addr.subAdminArea
            !addr.adminArea.isNullOrEmpty() -> addr.adminArea
            !addr.subLocality.isNullOrEmpty() -> addr.subLocality
            else -> "Current Location"
        }
    }

    private fun fetchIpGeolocation(): Triple<Double, Double, String>? {
        return try {
            val url = URL("https://ipwho.is/")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                if (obj.optBoolean("success", true)) {
                    val lat = obj.optDouble("latitude", 28.6139)
                    val lon = obj.optDouble("longitude", 77.2090)
                    val city = obj.optString("city", "Current City")
                    return Triple(lat, lon, city)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchWeatherForCoordinates(
        cityName: String,
        lat: Double,
        lon: Double,
        isGps: Boolean
    ): LiveWeatherInfo {
        try {
            val endpoint = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&timezone=auto"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val current = root.getJSONObject("current")

                val temp = current.optDouble("temperature_2m", 28.0)
                val apparentTemp = current.optDouble("apparent_temperature", temp)
                val humidityVal = current.optInt("relative_humidity_2m", 50)
                val weatherCode = current.optInt("weather_code", 0)
                val wind = current.optDouble("wind_speed_10m", 10.0)

                val (condition, icon, hindiDesc) = parseWeatherCode(weatherCode, temp)

                val summary = "$cityName mein abhi mausam $hindiDesc hai. Tapman ${temp.toInt()}°C (feels like ${apparentTemp.toInt()}°C), nami $humidityVal% aur hawa ki gati ${wind.toInt()} km/h hai."

                return LiveWeatherInfo(
                    cityName = cityName,
                    temperature = "${temp.toInt()}°",
                    temperatureValue = temp,
                    feelsLike = "${apparentTemp.toInt()}°C",
                    humidity = "$humidityVal%",
                    windSpeed = "${wind.toInt()} km/h",
                    condition = condition,
                    weatherCode = weatherCode,
                    icon = icon,
                    latitude = lat,
                    longitude = lon,
                    isGpsAccurate = isGps,
                    summaryHinglish = summary
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Open-Meteo response", e)
        }

        return LiveWeatherInfo(cityName = cityName, isGpsAccurate = isGps)
    }

    private fun parseWeatherCode(code: Int, temp: Double): Triple<String, String, String> {
        return when (code) {
            0 -> Triple("Clear Sky", "☀️", "ekdum saaf aur dhoop bhara")
            1 -> Triple("Mainly Clear", "🌤️", "aamtaur par saaf")
            2 -> Triple("Partly Cloudy", "⛅", "halke badal chaye hue")
            3 -> Triple("Overcast", "☁️", "ghane badal chaye hue")
            45, 48 -> Triple("Foggy", "🌫️", "kohra (fog) chaya hua")
            51, 53, 55 -> Triple("Drizzle", "🌧️", "halki boondabandi")
            61, 63, 65 -> Triple("Rainy", "🌧️", "barish ho rahi")
            71, 73, 75 -> Triple("Snowy", "❄️", "barfbari")
            80, 81, 82 -> Triple("Showers", "🌦️", "tez bochhar (showers)")
            95, 96, 99 -> Triple("Thunderstorm", "⛈️", "toofani barish aur bijli kadak rahi")
            else -> Triple("Pleasant", "🌤️", "suhana")
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
