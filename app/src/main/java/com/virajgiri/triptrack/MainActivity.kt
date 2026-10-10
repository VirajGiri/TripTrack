package com.virajgiri.triptrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.virajgiri.triptrack.data.TripRepository
import com.virajgiri.triptrack.data.local.TripEntity
import com.virajgiri.triptrack.data.local.TripTrackDatabase
import com.virajgiri.triptrack.domain.billing.TripInputValidator
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID

private enum class AppTab(val title: String, val icon: ImageVector) {
    Dashboard("Home", Icons.Default.Dashboard),
    TrackTrip("Track Trip", Icons.Default.DirectionsCar),
    Trips("Trips", Icons.Default.ReceiptLong),
    Clients("Clients", Icons.Default.Groups),
    Profile("Profile", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = TripRepository(TripTrackDatabase.getInstance(applicationContext).tripDao())
        setContent { MaterialTheme { TripTrackApp(repository) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripTrackApp(repository: TripRepository) {
    var selectedTab by remember { mutableStateOf(AppTab.Dashboard) }
    val trips by repository.trips.collectAsStateWithLifecycle(initialValue = emptyList())
    val tripCount by repository.tripCount.collectAsStateWithLifecycle(initialValue = 0)
    val totalKm by repository.totalKm.collectAsStateWithLifecycle(initialValue = 0.0)
    val totalMinutes by repository.totalDurationMinutes.collectAsStateWithLifecycle(initialValue = 0L)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Column {
            Text("TripTrack")
            Text(selectedTab.title, style = MaterialTheme.typography.labelMedium)
        } }) },
        bottomBar = { NavigationBar {
            AppTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                    label = { Text(tab.title) }
                )
            }
        } }
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                AppTab.Dashboard -> DashboardScreen(tripCount, totalKm, totalMinutes) { selectedTab = AppTab.TrackTrip }
                AppTab.TrackTrip -> ManualTripScreen { form ->
                    val validation = TripInputValidator.validate(form.date, form.startTime, form.endTime, form.startKm, form.endKm)
                    validation.fold(
                        onSuccess = { valid ->
                            val tollPaise = form.toll.toPaiseOrNull()
                            val otherPaise = form.other.toPaiseOrNull()
                            when {
                                form.client.isBlank() -> scope.launch { snackbar.showSnackbar("Client name is required.") }
                                form.packageType.isBlank() -> scope.launch { snackbar.showSnackbar("Package is required.") }
                                tollPaise == null || otherPaise == null || tollPaise < 0 || otherPaise < 0 -> scope.launch { snackbar.showSnackbar("Enter valid non-negative expenses with up to 2 decimal places.") }
                                else -> scope.launch {
                                    runCatching {
                                        repository.save(TripEntity(
                                            id = UUID.randomUUID().toString(),
                                            tripDate = valid.date.toString(),
                                            clientName = form.client.trim(),
                                            packageType = form.packageType.trim(),
                                            guestName = form.guest.trim(),
                                            vehicleNumber = form.vehicle.trim(),
                                            startTime = valid.startTime.toString(),
                                            endTime = valid.endTime.toString(),
                                            startOdometerKm = valid.startKm,
                                            endOdometerKm = valid.endKm,
                                            tollPaise = tollPaise,
                                            otherExpensePaise = otherPaise,
                                            distanceKm = valid.distanceKm,
                                            durationMinutes = valid.durationMinutes,
                                            createdAtEpochMillis = System.currentTimeMillis()
                                        ))
                                    }.onSuccess {
                                        snackbar.showSnackbar("Trip saved on this device.")
                                        selectedTab = AppTab.Trips
                                    }.onFailure { error ->
                                        snackbar.showSnackbar("Could not save trip: ${error.message ?: "unknown error"}")
                                    }
                                }
                            }
                        },
                        onFailure = { error -> scope.launch { snackbar.showSnackbar(error.message ?: "Please check trip details.") } }
                    )
                }
                AppTab.Trips -> TripsScreen(trips)
                AppTab.Clients -> PlaceholderScreen("Clients and rate cards", "Client profiles and configurable package rates are the next implementation step.")
                AppTab.Profile -> PlaceholderScreen("Driver profile", "Driver, vehicle and account settings are the next implementation step.")
            }
        }
    }
}

private data class TripForm(
    val date: String, val client: String, val packageType: String, val guest: String,
    val vehicle: String, val startTime: String, val endTime: String, val startKm: String,
    val endKm: String, val toll: String, val other: String
)

private fun String.toPaiseOrNull(): Long? = runCatching {
    val value = trim().toBigDecimal()
    if (value < BigDecimal.ZERO || value.scale() > 2) null else value.movePointRight(2).longValueExact()
}.getOrNull()

@Composable
private fun DashboardScreen(count: Int, km: Double, minutes: Long, onAddTrip: () -> Unit) {
    Text("Overview", style = MaterialTheme.typography.headlineSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard("Total trips", count.toString(), Modifier.weight(1f))
        SummaryCard("Total KM", "%.1f".format(km), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard("Duty hours", "%.1f".format(minutes / 60.0), Modifier.weight(1f))
        SummaryCard("Billable amount", "—", Modifier.weight(1f))
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Manual trip entry", style = MaterialTheme.typography.titleMedium)
            Text("Save trip details locally, even without internet.")
            Button(onClick = onAddTrip) { Text("Add trip") }
        }
    }
    Text("Billable totals will be enabled after client rate cards and billing rules are implemented.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.headlineSmall)
    } }
}

@Composable
private fun ManualTripScreen(onSave: (TripForm) -> Unit) {
    var date by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var packageType by remember { mutableStateOf("") }
    var guest by remember { mutableStateOf("") }
    var vehicle by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var startKm by remember { mutableStateOf("") }
    var endKm by remember { mutableStateOf("") }
    var toll by remember { mutableStateOf("0") }
    var other by remember { mutableStateOf("0") }

    Text("Enter trip details", style = MaterialTheme.typography.headlineSmall)
    Text("Manual entry only. No GPS tracking is used.")
    FormField("Trip date (YYYY-MM-DD)", date) { date = it }
    FormField("Client name", client) { client = it }
    FormField("Package (Pickup/drop, Local, Outstation)", packageType) { packageType = it }
    FormField("Guest name (optional)", guest) { guest = it }
    FormField("Vehicle registration", vehicle) { vehicle = it }
    FormField("Start time (HH:MM)", startTime) { startTime = it }
    FormField("End time (HH:MM)", endTime) { endTime = it }
    FormField("Start odometer (KM)", startKm) { startKm = it }
    FormField("End odometer (KM)", endKm) { endKm = it }
    FormField("Toll (₹)", toll) { toll = it }
    FormField("Other expenses (₹)", other) { other = it }
    Button(onClick = { onSave(TripForm(date, client, packageType, guest, vehicle, startTime, endTime, startKm, endKm, toll, other)) }, modifier = Modifier.fillMaxWidth()) { Text("Save trip") }
}

@Composable
private fun TripsScreen(trips: List<TripEntity>) {
    Text("Trip history", style = MaterialTheme.typography.headlineSmall)
    if (trips.isEmpty()) Text("No trips saved yet. Use Track Trip to add your first trip.")
    trips.forEach { trip ->
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("${trip.clientName} · ${trip.tripDate}", style = MaterialTheme.typography.titleMedium)
            Text("${trip.packageType} · ${"%.1f".format(trip.distanceKm)} KM")
            Text("${trip.startTime}–${trip.endTime} · ${trip.durationMinutes / 60}h ${trip.durationMinutes % 60}m")
            Text("Toll: ₹${"%.2f".format(trip.tollPaise / 100.0)} · Other: ₹${"%.2f".format(trip.otherExpensePaise / 100.0)}")
            Text("Sync status: ${trip.syncStatus}", style = MaterialTheme.typography.labelSmall)
        } }
    }
}

@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(description) } }
}
