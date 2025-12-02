package com.example.alarmpuzzle

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarmpuzzle.ui.theme.AlarmPuzzleTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmScheduler = AlarmScheduler(this)
        setContent {
            AlarmPuzzleTheme {
                AlarmScreen(alarmScheduler)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(scheduler: AlarmScheduler) {
    val context = LocalContext.current
    var alarms by remember { mutableStateOf(AlarmRepository.loadAlarms(context)) }

    // --- Permission Handling for Notifications ---
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (!isGranted) {
                    Toast.makeText(context, "Notifications permission is required for alarms to work.", Toast.LENGTH_LONG).show()
                }
            }
        )
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Alarm", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val newAlarm = Alarm(
                        id = (alarms.maxOfOrNull { it.id } ?: 0) + 1,
                        hour = 7, minute = 0, isEnabled = true
                    )
                    val updatedAlarms = (alarms + newAlarm).sortedBy { it.hour * 60 + it.minute }
                    alarms = updatedAlarms
                    AlarmRepository.saveAlarms(context, updatedAlarms)
                    scheduler.schedule(newAlarm)
                },
                shape = CircleShape,
                containerColor = Color(0xFF333333)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Alarm", tint = Color.White)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.Black) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* No other tabs */ },
                    icon = { Icon(Icons.Filled.Alarm, contentDescription = "Alarm") },
                    label = { Text("Alarm") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF2E2E2E)
                    )
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onUpdate = { updatedAlarm ->
                        alarms = alarms.map { if (it.id == updatedAlarm.id) updatedAlarm else it }
                        AlarmRepository.saveAlarms(context, alarms)
                        if (updatedAlarm.isEnabled) scheduler.schedule(updatedAlarm) else scheduler.cancel(updatedAlarm)
                    },
                    onDelete = {
                        scheduler.cancel(alarm)
                        alarms = alarms.filterNot { it.id == alarm.id }
                        AlarmRepository.saveAlarms(context, alarms)
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun AlarmItem(alarm: Alarm, onUpdate: (Alarm) -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    // --- Ringtone Picker Launcher ---
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            onUpdate(alarm.copy(ringtoneUri = uri?.toString()))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light,
                        color = if (alarm.isEnabled) Color.White else Color.Gray,
                        modifier = Modifier.clickable { showTimePicker(context, alarm, onUpdate) }
                    )
                    Text(
                        text = alarm.getRecurringDaysText(),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { isEnabled -> onUpdate(alarm.copy(isEnabled = isEnabled)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = Color.DarkGray,
                        uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF2C2C2E)
                    )
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        value = alarm.label,
                        onValueChange = { onUpdate(alarm.copy(label = it)) },
                        label = { Text("Add label") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Gray, unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = Color.Gray, unfocusedLabelColor = Color.Gray
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    DaySelector(
                        selectedDays = alarm.recurringDays,
                        onDaySelected = { day ->
                            val newDays = alarm.recurringDays.toMutableSet().apply {
                                if (contains(day)) remove(day) else add(day)
                            }
                            onUpdate(alarm.copy(recurringDays = newDays))
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    // --- New Ringtone Setting Row ---
                    SettingRow(
                        icon = Icons.Default.MusicNote,
                        title = "Ringtone",
                        subtitle = getRingtoneTitle(context, alarm.ringtoneUri),
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                val currentUri = alarm.ringtoneUri?.let { Uri.parse(it) }
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                            }
                            ringtonePickerLauncher.launch(intent)
                        }
                    )
                    PuzzleSelector(
                        selectedPuzzle = alarm.puzzleType,
                        onPuzzleSelected = { onUpdate(alarm.copy(puzzleType = it)) }
                    )
                    SettingRowWithSwitch(
                        icon = Icons.Default.Vibration,
                        title = "Vibrate",
                        checked = alarm.vibrate,
                        onCheckedChange = { onUpdate(alarm.copy(vibrate = it)) }
                    )
                    SettingRow(
                        icon = Icons.Default.Delete,
                        title = "Delete",
                        onClick = onDelete,
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

// --- Other UI components (DaySelector, PuzzleSelector, etc.) remain the same ---

@Composable
fun DaySelector(selectedDays: Set<DayOfWeek>, onDaySelected: (DayOfWeek) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.values().forEach { day ->
            val isSelected = selectedDays.contains(day)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color = if (isSelected) Color.White else Color.Transparent)
                    .clickable { onDaySelected(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.name.first().toString(),
                    color = if (isSelected) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PuzzleSelector(selectedPuzzle: PuzzleType, onPuzzleSelected: (PuzzleType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SettingRow(
            icon = Icons.Default.Extension,
            title = "Puzzle",
            subtitle = selectedPuzzle.displayName,
            onClick = { expanded = true },
            trailingContent = { Icon(Icons.Default.ArrowDropDown, "Select puzzle", tint = Color.White) }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF2C2C2E))
        ) {
            PuzzleType.values().forEach { puzzleType ->
                DropdownMenuItem(
                    text = { Text(puzzleType.displayName, color = Color.White) },
                    onClick = {
                        onPuzzleSelected(puzzleType)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SettingRow(icon: ImageVector, title: String, subtitle: String? = null, tint: Color = Color.White, onClick: (() -> Unit)? = null, trailingContent: @Composable (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }.padding(vertical = 12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = tint)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = tint, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        trailingContent?.invoke()
    }
}

@Composable
fun SettingRowWithSwitch(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Icon(imageVector = icon, contentDescription = title, tint = Color.White)
        Spacer(Modifier.width(16.dp))
        Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = Color.DarkGray,
                uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF2C2C2E)
            )
        )
    }
}


fun showTimePicker(context: Context, alarm: Alarm, onUpdate: (Alarm) -> Unit) {
    TimePickerDialog(
        context,
        { _, hourOfDay, minute -> onUpdate(alarm.copy(hour = hourOfDay, minute = minute)) },
        alarm.hour, alarm.minute, true
    ).show()
}

// --- Helper to display ringtone name ---
@Composable
fun getRingtoneTitle(context: Context, uriString: String?): String {
    if (uriString == null) {
        return "Default"
    }
    return try {
        val uri = Uri.parse(uriString)
        RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
}

