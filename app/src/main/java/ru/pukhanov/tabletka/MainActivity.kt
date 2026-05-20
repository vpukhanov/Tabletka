package ru.pukhanov.tabletka

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.pukhanov.tabletka.ui.screens.MedicationApp
import ru.pukhanov.tabletka.ui.theme.TabletkaTheme
import ru.pukhanov.tabletka.ui.viewmodel.MedicationViewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MedicationViewModel
    private var dateChangeReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as TabletkaApplication).repository
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MedicationViewModel(repository) as T
            }
        })[MedicationViewModel::class.java]

        setContent {
            TabletkaTheme {
                MedicationApp(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setCurrentDate(LocalDate.now())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        dateChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                viewModel.setCurrentDate(LocalDate.now())
            }
        }
        registerReceiver(dateChangeReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        dateChangeReceiver?.let {
            unregisterReceiver(it)
            dateChangeReceiver = null
        }
    }
}