package io.github.demiterhugo.weatherflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.demiterhugo.weatherflow.ui.theme.WeatherFlowTheme
import io.github.demiterhugo.weatherflow.ui.weather.WeatherRoute
import io.github.demiterhugo.weatherflow.ui.weather.WeatherViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val weatherViewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherFlowTheme {
                WeatherRoute(weatherViewModel)
            }
        }
    }
}
