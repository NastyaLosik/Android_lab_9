package com.example.retrofitforecaster

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

data class WeatherStore(
    var weathers: List<WeatherItem>? = null
)

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private val weatherAdapter = WeatherAdapter()
    private var weatherStore = WeatherStore()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupToolbar()
        setupRecyclerView()

        if (savedInstanceState == null) {
            fetchweatherItem()
        } else {
            val savedweatherItemJson = savedInstanceState.getString("weather_data")
            Log.d("savedInstanceState", "Получены сохраненные данные погоды из savedInstanceState: ${savedweatherItemJson ?: "нет данных"}")

            if (savedweatherItemJson != null) {
                weatherStore.weathers = Gson().fromJson(savedweatherItemJson, Array<WeatherItem>::class.java).toList()
                weatherAdapter.submitList(weatherStore.weathers)
                Log.d("MainActivity", "Restored weather data")
            } else {
                Log.w("MainActivity", "Сохраненные данные пусты")
            }
        }
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.r_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = weatherAdapter
    }

    private fun fetchweatherItem() {
        val apiService = createApiService()
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Запрос погоды для города: Шклов")
                val response = apiService.getWeatherForecast("Шклов", "b34d57cbea81313213c07de26a250289")

                if (response.list.isNotEmpty()) {
                    Log.d("MainActivity", "Получены данные погоды: ${response.list.size} записей")
                    weatherStore.weathers = response.list
                    weatherAdapter.submitList(weatherStore.weathers)
                } else {
                    Log.w("MainActivity", "Ответ от сервера пустой")
                    showToast("No data available")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка при загрузке данных: ${e.message}", e)
                showToast("Error loading data")
            }
        }
    }

    private fun createApiService(): WeatherApi {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(WeatherApi::class.java)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        weatherStore.weathers?.let {
            val weatherItemJson = Gson().toJson(it)
            outState.putString("weather_data", weatherItemJson)
            Log.d("MainActivity", "Saved weather data")
        }
    }
}