package com.virajgiri.triptrack

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.platform.LocalContext
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
import com.virajgiri.triptrack.data.InvoiceSnapshotFactory
import com.virajgiri.triptrack.data.TripRepository
import com.virajgiri.triptrack.data.local.BillEntity
import com.virajgiri.triptrack.data.local.ClientEntity
import com.virajgiri.triptrack.data.local.TripEntity
import com.virajgiri.triptrack.data.local.TripTrackDatabase
import com.virajgiri.triptrack.domain.billing.BillingCalculator
import com.virajgiri.triptrack.domain.billing.BillingResult
import com.virajgiri.triptrack.domain.billing.TripInputValidator
import com.virajgiri.triptrack.domain.billing.TripPackage
import com.virajgiri.triptrack.util.InvoicePdfExporter
import com.virajgiri.triptrack.sync.FirestoreSyncWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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
        val db = TripTrackDatabase.getInstance(applicationContext)
        val initialOwnerUid = runCatching {
            if (FirebaseApp.getApps(applicationContext).isNotEmpty()) {
                FirebaseAuth.getInstance().currentUser?.uid ?: "local"
            } else "local"
        }.getOrDefault("local")
        setContent {
            MaterialTheme {
                var ownerUid by remember { mutableStateOf(initialOwnerUid) }
                val repository = remember(ownerUid) {
                    TripRepository(db.tripDao(), db.clientDao(), db.billDao(), ownerUid)
                }
                TripTrackApp(repository, ownerUid) { newOwnerUid -> ownerUid = newOwnerUid }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripTrackApp(repository: TripRepository, ownerUid: String, onOwnerChanged: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf(AppTab.Dashboard) }
    var editingTrip by remember { mutableStateOf<TripEntity?>(null) }
    val trips by repository.trips.collectAsStateWithLifecycle(initialValue = emptyList())
    val clients by repository.clients.collectAsStateWithLifecycle(initialValue = emptyList())
    val bills by repository.bills.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
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
                AppTab.TrackTrip -> ManualTripScreen(initialTrip = editingTrip) { form ->
                    val validation = TripInputValidator.validate(form.date, form.startTime, form.endTime, form.startKm, form.endKm)
                    validation.fold(
                        onSuccess = { valid ->
                            val tollPaise = form.toll.toPaiseOrNull()
                            val otherPaise = form.other.toPaiseOrNull()
                            when {
                                form.client.isBlank() -> scope.launch { snackbar.showSnackbar("Client name is required.") }
                                form.packageType !in listOf("Pickup/drop", "Local", "Outstation") -> scope.launch { snackbar.showSnackbar("Choose Pickup/drop, Local or Outstation exactly.") }
                                tollPaise == null || otherPaise == null || tollPaise < 0 || otherPaise < 0 -> scope.launch { snackbar.showSnackbar("Enter valid non-negative expenses with up to 2 decimal places.") }
                                else -> scope.launch {
                                    runCatching {
                                        val now = System.currentTimeMillis()
                                        val trip = TripEntity(
                                            id = form.id ?: UUID.randomUUID().toString(),
                                            tripDate = valid.date.toString(),
                                            clientName = form.client.trim(),
                                            packageType = form.packageType,
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
                                            createdAtEpochMillis = form.createdAtEpochMillis ?: now,
                                            updatedAtEpochMillis = now
                                        )
                                        if (form.id == null) repository.save(trip) else repository.updateTrip(trip)
                                    }.onSuccess {
                                        FirestoreSyncWorker.enqueue(context, ownerUid)
                                        snackbar.showSnackbar(if (form.id == null) "Trip saved on this device." else "Trip updated.")
                                        editingTrip = null
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
                AppTab.Trips -> TripsScreen(trips, clients, bills, onPreviewBill = { trip, client ->
                    val packageType = when (trip.packageType) {
                        "Pickup/drop" -> TripPackage.PICKUP_DROP
                        "Local" -> TripPackage.LOCAL
                        else -> TripPackage.OUTSTATION
                    }
                    when (val result = BillingCalculator.calculate(client, packageType, trip.distanceKm, trip.durationMinutes)) {
                        is BillingResult.MissingRate -> scope.launch { snackbar.showSnackbar("Cannot bill: configure ${result.rateName} for this client first.") }
                        is BillingResult.UnsupportedTrip -> scope.launch { snackbar.showSnackbar("Cannot bill: ${result.reason}") }
                        is BillingResult.Success -> scope.launch {
                            runCatching {
                                val snapshot = InvoiceSnapshotFactory.create(trip, result.bill)
                                repository.saveBill(snapshot)
                                FirestoreSyncWorker.enqueue(context, ownerUid)
                                val pdf = InvoicePdfExporter.createPdf(context, snapshot)
                                context.startActivity(Intent.createChooser(InvoicePdfExporter.shareIntent(context, pdf), "Share TripTrack invoice"))
                            }.onSuccess {
                                snackbar.showSnackbar("Invoice snapshot saved. Share the generated PDF.")
                            }.onFailure { error ->
                                snackbar.showSnackbar("Invoice could not be generated: ${error.message ?: "unknown error"}")
                            }
                        }
                    }
                }, onShareSavedBill = { bill ->
                    scope.launch {
                        runCatching {
                            val pdf = InvoicePdfExporter.createPdf(context, bill)
                            context.startActivity(Intent.createChooser(InvoicePdfExporter.shareIntent(context, pdf), "Share TripTrack invoice"))
                        }.onFailure { error -> snackbar.showSnackbar("Could not export invoice: ${error.message ?: "unknown error"}") }
                    }
                }, onEditTrip = { trip ->
                    editingTrip = trip
                    selectedTab = AppTab.TrackTrip
                }, onDeleteTrip = { trip ->
                    scope.launch {
                        runCatching { repository.deleteTrip(trip.id) }
                            .onSuccess {
                                FirestoreSyncWorker.enqueue(context, ownerUid)
                                snackbar.showSnackbar("Trip deleted. Existing invoice snapshots are retained.")
                            }
                            .onFailure { snackbar.showSnackbar("Could not delete trip.") }
                    }
                })
                AppTab.Clients -> ClientManagementScreen(
                    clients = clients,
                    onSave = { client -> scope.launch {
                        runCatching { repository.saveClient(client) }
                            .onSuccess {
                                FirestoreSyncWorker.enqueue(context, ownerUid)
                                snackbar.showSnackbar("Client and rate card saved.")
                            }
                            .onFailure { snackbar.showSnackbar("Could not save client: ${it.message ?: "unknown error"}") }
                    } },
                    onDelete = { id -> scope.launch {
                        runCatching { repository.deleteClient(id) }
                            .onSuccess {
                                FirestoreSyncWorker.enqueue(context, ownerUid)
                                snackbar.showSnackbar("Client deleted.")
                            }
                            .onFailure { snackbar.showSnackbar("Could not delete client.") }
                    } }
                )
                AppTab.Profile -> ProfileScreen(
                    ownerUid = ownerUid,
                    onOwnerChanged = { newOwnerUid -> editingTrip = null; onOwnerChanged(newOwnerUid) },
                    onImportLocalData = {
                        scope.launch {
                            runCatching { repository.claimLocalDataForThisAccount() }
                                .onSuccess { count ->
                                    FirestoreSyncWorker.enqueue(context, ownerUid)
                                    snackbar.showSnackbar("Imported $count local records. Cloud upload queued.")
                                }
                                .onFailure { snackbar.showSnackbar(it.message ?: "Could not import local data.") }
                        }
                    },
                    onSyncNow = {
                        FirestoreSyncWorker.enqueue(context, ownerUid)
                        scope.launch { snackbar.showSnackbar("Cloud upload queued for this account.") }
                    }
                )
            }
        }
    }
}

private data class TripForm(
    val date: String, val client: String, val packageType: String, val guest: String,
    val vehicle: String, val startTime: String, val endTime: String, val startKm: String,
    val endKm: String, val toll: String, val other: String,
    val id: String? = null, val createdAtEpochMillis: Long? = null
)

private fun String.toPaiseOrNull(): Long? = runCatching {
    val value = trim().toBigDecimal()
    if (value < BigDecimal.ZERO || value.scale() > 2) null else value.movePointRight(2).longValueExact()
}.getOrNull()

private fun String.optionalPaise(): Long? {
    if (isBlank()) return null
    return toPaiseOrNull() ?: throw IllegalArgumentException("Rates must be non-negative amounts with up to 2 decimal places.")
}

private fun money(paise: Long?): String = paise?.let { "₹%.2f".format(it / 100.0) } ?: "Not set"

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
    Text("Bill preview uses client-configured rates. Finalized invoices and dashboard billing totals are not implemented yet.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.headlineSmall)
    } }
}

@Composable
private fun ManualTripScreen(initialTrip: TripEntity?, onSave: (TripForm) -> Unit) {
    var date by remember(initialTrip?.id) { mutableStateOf(initialTrip?.tripDate ?: "") }
    var client by remember(initialTrip?.id) { mutableStateOf(initialTrip?.clientName ?: "") }
    var packageType by remember(initialTrip?.id) { mutableStateOf(initialTrip?.packageType ?: "Local") }
    var guest by remember(initialTrip?.id) { mutableStateOf(initialTrip?.guestName ?: "") }
    var vehicle by remember(initialTrip?.id) { mutableStateOf(initialTrip?.vehicleNumber ?: "") }
    var startTime by remember(initialTrip?.id) { mutableStateOf(initialTrip?.startTime ?: "") }
    var endTime by remember(initialTrip?.id) { mutableStateOf(initialTrip?.endTime ?: "") }
    var startKm by remember(initialTrip?.id) { mutableStateOf(initialTrip?.startOdometerKm?.toString() ?: "") }
    var endKm by remember(initialTrip?.id) { mutableStateOf(initialTrip?.endOdometerKm?.toString() ?: "") }
    var toll by remember(initialTrip?.id) { mutableStateOf(initialTrip?.let { "%.2f".format(it.tollPaise / 100.0) } ?: "0") }
    var other by remember(initialTrip?.id) { mutableStateOf(initialTrip?.let { "%.2f".format(it.otherExpensePaise / 100.0) } ?: "0") }

    Text("Enter trip details", style = MaterialTheme.typography.headlineSmall)
    Text("Manual entry only. No GPS tracking is used.")
    FormField("Trip date (YYYY-MM-DD)", date) { date = it }
    FormField("Client name (must match client profile)", client) { client = it }
    FormField("Package: Pickup/drop, Local, Outstation", packageType) { packageType = it }
    FormField("Guest name (optional)", guest) { guest = it }
    FormField("Vehicle registration", vehicle) { vehicle = it }
    FormField("Start time (HH:MM)", startTime) { startTime = it }
    FormField("End time (HH:MM)", endTime) { endTime = it }
    FormField("Start odometer (KM)", startKm) { startKm = it }
    FormField("End odometer (KM)", endKm) { endKm = it }
    FormField("Toll (₹)", toll) { toll = it }
    FormField("Other expenses (₹)", other) { other = it }
    Button(onClick = { onSave(TripForm(date, client, packageType, guest, vehicle, startTime, endTime, startKm, endKm, toll, other, initialTrip?.id, initialTrip?.createdAtEpochMillis)) }, modifier = Modifier.fillMaxWidth()) { Text(if (initialTrip == null) "Save trip" else "Update trip") }
}

@Composable
private fun TripsScreen(
    trips: List<TripEntity>,
    clients: List<ClientEntity>,
    bills: List<BillEntity>,
    onPreviewBill: (TripEntity, ClientEntity) -> Unit,
    onShareSavedBill: (BillEntity) -> Unit,
    onEditTrip: (TripEntity) -> Unit,
    onDeleteTrip: (TripEntity) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<TripEntity?>(null) }
    Text("Trip history", style = MaterialTheme.typography.headlineSmall)
    if (trips.isEmpty()) Text("No trips saved yet. Use Track Trip to add your first trip.")
    trips.forEach { trip ->
        val client = clients.firstOrNull { it.name.trim().equals(trip.clientName.trim(), ignoreCase = true) }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("${trip.clientName} · ${trip.tripDate}", style = MaterialTheme.typography.titleMedium)
            Text("${trip.packageType} · ${"%.1f".format(trip.distanceKm)} KM")
            Text("${trip.startTime}–${trip.endTime} · ${trip.durationMinutes / 60}h ${trip.durationMinutes % 60}m")
            Text("Toll: ₹${"%.2f".format(trip.tollPaise / 100.0)} · Other: ₹${"%.2f".format(trip.otherExpensePaise / 100.0)}")
            Text("Sync status: ${trip.syncStatus}", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onEditTrip(trip) }, modifier = Modifier.weight(1f)) { Text("Edit trip") }
                OutlinedButton(onClick = { pendingDelete = trip }, modifier = Modifier.weight(1f)) { Text("Delete") }
            }
            if (client == null) {
                Text("Create a matching client profile to calculate this trip's bill.", style = MaterialTheme.typography.bodySmall)
            } else {
                OutlinedButton(onClick = { onPreviewBill(trip, client) }, modifier = Modifier.fillMaxWidth()) { Text("Create invoice PDF") }
            }
        } }
    }
    Text("Saved invoice snapshots", style = MaterialTheme.typography.titleLarge)
    if (bills.isEmpty()) Text("No invoices generated yet.")
    bills.forEach { bill ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(bill.clientNameSnapshot, style = MaterialTheme.typography.titleMedium)
                Text("${bill.tripDateSnapshot} · ${bill.packageTypeSnapshot} · ${"%.1f".format(bill.distanceKmSnapshot)} KM")
                Text("Total: ₹${"%.2f".format(bill.totalPaise / 100.0)}")
                Text("Invoice snapshot is fixed; later rate edits will not change it.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { onShareSavedBill(bill) }) { Text("Share PDF again") }
            }
        }
    }
    pendingDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this trip?") },
            text = { Text("The trip record will be removed. Previously generated invoice snapshots will remain unchanged.") },
            confirmButton = { TextButton(onClick = { onDeleteTrip(trip); pendingDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ClientManagementScreen(
    clients: List<ClientEntity>,
    onSave: (ClientEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var pickupBase by remember { mutableStateOf("") }
    var pickupHour by remember { mutableStateOf("") }
    var pickupKm by remember { mutableStateOf("") }
    var localBase by remember { mutableStateOf("") }
    var localHour by remember { mutableStateOf("") }
    var localKm by remember { mutableStateOf("") }
    var outstationDaily by remember { mutableStateOf("") }
    var outstationKm by remember { mutableStateOf("12") }
    var formError by remember { mutableStateOf<String?>(null) }

    fun loadClient(client: ClientEntity) {
        editingId = client.id
        name = client.name
        contact = client.contact
        pickupBase = client.pickupBasePaise?.let { "%.2f".format(it / 100.0) } ?: ""
        pickupHour = client.pickupExtraHourPaise?.let { "%.2f".format(it / 100.0) } ?: ""
        pickupKm = client.pickupExtraKmPaise?.let { "%.2f".format(it / 100.0) } ?: ""
        localBase = client.localBasePaise?.let { "%.2f".format(it / 100.0) } ?: ""
        localHour = client.localExtraHourPaise?.let { "%.2f".format(it / 100.0) } ?: ""
        localKm = client.localExtraKmPaise?.let { "%.2f".format(it / 100.0) } ?: ""
        outstationDaily = client.outstationDailyPaise?.let { "%.2f".format(it / 100.0) } ?: ""
        outstationKm = client.outstationExtraKmPaise?.let { "%.2f".format(it / 100.0) } ?: ""
        formError = null
    }

    Text("Client profiles & rate cards", style = MaterialTheme.typography.headlineSmall)
    Text("Leave an unknown rate blank. Billing will stop if a required rate is missing.")
    if (clients.isEmpty()) Text("No clients yet.")
    clients.forEach { client ->
        Card(Modifier.fillMaxWidth().clickable { loadClient(client) }) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(client.name, style = MaterialTheme.typography.titleMedium)
                if (client.contact.isNotBlank()) Text(client.contact)
                Text("Pickup/drop base: ${money(client.pickupBasePaise)} · Local base: ${money(client.localBasePaise)}")
                Text("Outstation/day: ${money(client.outstationDailyPaise)} · Outstation extra KM: ${money(client.outstationExtraKmPaise)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { loadClient(client) }) { Text("Edit rates") }
                    OutlinedButton(onClick = { onDelete(client.id); if (editingId == client.id) { editingId = null; name = "" } }) { Text("Delete") }
                }
            }
        }
    }
    Text(if (editingId == null) "Add client" else "Edit client", style = MaterialTheme.typography.titleLarge)
    FormField("Client/company name *", name) { name = it; formError = null }
    FormField("Contact details (optional)", contact) { contact = it }
    Text("Pickup/drop · allowance 4 hours or 40 KM", style = MaterialTheme.typography.titleMedium)
    FormField("Base rate (₹)", pickupBase) { pickupBase = it }
    FormField("Extra hour rate (₹/hour)", pickupHour) { pickupHour = it }
    FormField("Extra distance rate (₹/KM)", pickupKm) { pickupKm = it }
    Text("Local · allowance 8 hours or 80 KM", style = MaterialTheme.typography.titleMedium)
    FormField("Base rate (₹)", localBase) { localBase = it }
    FormField("Extra hour rate (₹/hour)", localHour) { localHour = it }
    FormField("Extra distance rate (₹/KM)", localKm) { localKm = it }
    Text("Outstation · 300 KM/day; known extra distance rate ₹12/KM", style = MaterialTheme.typography.titleMedium)
    FormField("Base daily rate (₹)", outstationDaily) { outstationDaily = it }
    FormField("Extra distance rate (₹/KM)", outstationKm) { outstationKm = it }
    if (formError != null) Text(formError!!, color = MaterialTheme.colorScheme.error)
    Button(onClick = {
        try {
            if (name.isBlank()) throw IllegalArgumentException("Client name is required.")
            val fields = listOf(pickupBase, pickupHour, pickupKm, localBase, localHour, localKm, outstationDaily, outstationKm)
            val parsed = fields.map { it.optionalPaise() }
            if (parsed.any { it != null && it < 0L }) throw IllegalArgumentException("Rates cannot be negative.")
            onSave(ClientEntity(
                id = editingId ?: UUID.randomUUID().toString(),
                name = name.trim(),
                contact = contact.trim(),
                pickupBasePaise = parsed[0],
                pickupExtraHourPaise = parsed[1],
                pickupExtraKmPaise = parsed[2],
                localBasePaise = parsed[3],
                localExtraHourPaise = parsed[4],
                localExtraKmPaise = parsed[5],
                outstationDailyPaise = parsed[6],
                outstationExtraKmPaise = parsed[7],
                updatedAtEpochMillis = System.currentTimeMillis()
            ))
            editingId = null
            name = ""; contact = ""; pickupBase = ""; pickupHour = ""; pickupKm = ""
            localBase = ""; localHour = ""; localKm = ""; outstationDaily = ""; outstationKm = "12"
            formError = null
        } catch (error: IllegalArgumentException) {
            formError = error.message ?: "Please check rate values."
        }
    }, modifier = Modifier.fillMaxWidth()) { Text(if (editingId == null) "Save client" else "Save changes") }
    if (editingId != null) OutlinedButton(onClick = {
        editingId = null; name = ""; contact = ""; pickupBase = ""; pickupHour = ""; pickupKm = ""
        localBase = ""; localHour = ""; localKm = ""; outstationDaily = ""; outstationKm = "12"; formError = null
    }, modifier = Modifier.fillMaxWidth()) { Text("Cancel edit") }
}

@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
private fun ProfileScreen(ownerUid: String, onOwnerChanged: (String) -> Unit, onImportLocalData: () -> Unit, onSyncNow: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(ownerUid) { context.getSharedPreferences("triptrack_driver_profile_$ownerUid", android.content.Context.MODE_PRIVATE) }
    var driverName by remember(ownerUid) { mutableStateOf(preferences.getString("driverName", "") ?: "") }
    var driverPhone by remember(ownerUid) { mutableStateOf(preferences.getString("driverPhone", "") ?: "") }
    var vehicleRegistration by remember(ownerUid) { mutableStateOf(preferences.getString("vehicleRegistration", "") ?: "") }
    var vehicleModel by remember(ownerUid) { mutableStateOf(preferences.getString("vehicleModel", "") ?: "") }
    var profileSaved by remember { mutableStateOf(false) }
    val firebaseConfigured = remember { FirebaseApp.getApps(context).isNotEmpty() }
    val auth = remember(firebaseConfigured) {
        if (firebaseConfigured) runCatching { FirebaseAuth.getInstance() }.getOrNull() else null
    }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    val currentUser = remember(auth, refresh) { auth?.currentUser }

    Text("Driver profile & account", style = MaterialTheme.typography.headlineSmall)
    Text("Driver and vehicle details are stored locally on this device.")
    FormField("Driver name", driverName) { driverName = it; profileSaved = false }
    FormField("Phone number", driverPhone) { driverPhone = it; profileSaved = false }
    FormField("Vehicle registration", vehicleRegistration) { vehicleRegistration = it; profileSaved = false }
    FormField("Vehicle make/model (optional)", vehicleModel) { vehicleModel = it; profileSaved = false }
    Button(onClick = {
        preferences.edit()
            .putString("driverName", driverName.trim())
            .putString("driverPhone", driverPhone.trim())
            .putString("vehicleRegistration", vehicleRegistration.trim())
            .putString("vehicleModel", vehicleModel.trim())
            .apply()
        profileSaved = true
    }, modifier = Modifier.fillMaxWidth()) { Text("Save driver & vehicle profile") }
    if (profileSaved) Text("Profile saved on this device.")
    if (auth == null) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Firebase setup required", style = MaterialTheme.typography.titleMedium)
                Text("Add app/google-services.json for package com.virajgiri.triptrack, enable Email/Password authentication in Firebase Console, then rebuild.")
                Text("Cloud sync is unavailable until Firebase project configuration is added.")
            }
        }
        return
    }
    if (currentUser != null) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Signed in", style = MaterialTheme.typography.titleMedium)
                Text(currentUser.email ?: currentUser.uid)
                Text("Account-scoped local records are separated by UID.")
                Text("Sync restores missing cloud records, uploads pending changes and propagates trip/client deletions. Existing pending local edits are preserved.")
                Button(onClick = onSyncNow, modifier = Modifier.fillMaxWidth()) { Text("Sync pending records") }
                Button(onClick = onImportLocalData, modifier = Modifier.fillMaxWidth()) { Text("Import local device records to this account") }
                Button(onClick = {
                    auth.signOut()
                    onOwnerChanged("local")
                    refresh++
                    message = "Signed out. This device now shows only local records."
                }) { Text("Sign out") }
            }
        }
    } else {
        Text(if (createAccount) "Create account" else "Sign in", style = MaterialTheme.typography.titleMedium)
        FormField("Email", email) { email = it }
        FormField("Password", password) { password = it }
        Button(onClick = {
            if (email.isBlank() || password.isBlank()) {
                message = "Enter an email and password."
            } else {
                val task = if (createAccount) auth.createUserWithEmailAndPassword(email.trim(), password)
                    else auth.signInWithEmailAndPassword(email.trim(), password)
                task.addOnCompleteListener { result ->
                    if (result.isSuccessful) {
                        val uid = result.result?.user?.uid
                        if (uid != null) onOwnerChanged(uid)
                        refresh++
                        message = if (createAccount) "Account created and signed in." else "Signed in."
                    } else {
                        message = result.exception?.localizedMessage ?: "Authentication failed."
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (createAccount) "Create account" else "Sign in")
        }
        OutlinedButton(onClick = { createAccount = !createAccount; message = null }, modifier = Modifier.fillMaxWidth()) {
            Text(if (createAccount) "I already have an account" else "Create a new account")
        }
    }
    if (message != null) Text(message!!, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(description) } }
}
