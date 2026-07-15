package com.example.attendanceapp

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.attendanceapp.data.AppDatabase
import com.example.attendanceapp.data.AttendanceEntity
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.Locale
import com.example.attendanceapp.data.StudentProfile
import com.example.attendanceapp.ui.theme.StudentProfileSection



enum class StudentView {
    DASHBOARD, PROFILE, RESULTS, CHANGE_PASSWORD
}

@Composable
fun StudentDashboard(studentId: String, fullName: String, onLogout: () -> Unit, navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentView by remember { mutableStateOf(StudentView.DASHBOARD) }

    val attendanceHistory = remember { mutableStateListOf<String>() }
    val todayAttendance = remember { mutableStateListOf<String>() }
    var newPassword by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var studentProfile by remember { mutableStateOf<StudentProfile?>(null) }
    var exportedFilePath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val today = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.LocalDate.now().toString()
            } else {
                val cal = Calendar.getInstance()
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
            }

            val period = getCurrentPeriod()
            if (period != 0) {
                val response = markAttendanceIfNotExists(studentId, today, period, "Present")
                val message = JSONObject(response).optString("message", "Attendance marked")
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }

            try {
                val attendanceResponse = getAttendance(studentId)
                updateAttendance(attendanceResponse, attendanceHistory)
            } catch (e: Exception) {
                val db = AppDatabase.getInstance(context)
                val cached = db.attendanceDao().getAll()
                attendanceHistory.clear()
                attendanceHistory.addAll(cached.map { record: AttendanceEntity ->
                    "${record.date} | Period ${record.period} | ${record.status.replaceFirstChar { it.uppercase(Locale.ROOT) }}"
                })
            }

            val todayResponse = getTodayAttendance(studentId)
            updateTodayAttendance(todayResponse, todayAttendance)

            studentProfile = fetchStudentProfile(studentId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentView) {
            StudentView.DASHBOARD -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.statusBars.asPaddingValues())
                        .padding(24.dp)
                ) {
                    Text("Student Dashboard", style = MaterialTheme.typography.headlineMedium)
                    Text("Welcome, $fullName", style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Today's Attendance:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    todayAttendance.forEach { record -> Text(text = record) }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Attendance History:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    attendanceHistory.forEach { record -> Text(text = record) }
                }
            }

            StudentView.PROFILE -> {
                studentProfile?.let {
                    StudentProfileSection(profile = it)
                } ?: Text("Loading profile...")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { currentView = StudentView.DASHBOARD }, modifier = Modifier.padding(24.dp)) {
                    Text("Back to Dashboard")
                }
            }

            StudentView.RESULTS -> {
                val parsedResults = try {
                    val json = JSONObject(resultText)
                    if (json.getString("status") == "success") {
                        val resultsArray = json.getJSONArray("results")
                        List(resultsArray.length()) { i ->
                            val obj = resultsArray.getJSONObject(i)
                            "${obj.getString("subject")}: ${obj.getInt("marks")}"
                        }
                    } else {
                        listOf("Error: ${json.getString("message")}")
                    }
                } catch (e: Exception) {
                    listOf("Failed to parse results")
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Results:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    parsedResults.forEach { line -> Text(line, style = MaterialTheme.typography.bodyMedium) }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { currentView = StudentView.DASHBOARD }, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to Dashboard")
                    }
                }
            }

            StudentView.CHANGE_PASSWORD -> {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (newPassword.length < 6) {
                            Toast.makeText(context, "Password too short", Toast.LENGTH_SHORT).show()
                        } else {
                            coroutineScope.launch {
                                val response = updatePassword(studentId, newPassword)
                                val message = JSONObject(response).optString("message", "Password updated")
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                newPassword = ""
                                currentView = StudentView.DASHBOARD
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Change Password")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { currentView = StudentView.DASHBOARD }) {
                        Text("Cancel")
                    }
                }
            }
        }

        RoverButton(
            actions = listOf(
                "View Profile" to { currentView = StudentView.PROFILE },
                "View My Results" to {
                    coroutineScope.launch {
                        val response = getResults(studentId)
                        resultText = response
                        currentView = StudentView.RESULTS
                    }
                },
                "Export Attendance" to {
                    val file = exportAttendanceToPdf(context, attendanceHistory)
                    exportedFilePath = file?.absolutePath
                    Toast.makeText(context, "PDF exported", Toast.LENGTH_SHORT).show()
                },
                "Share Exported PDF" to {
                    exportedFilePath?.let { path ->
                        val file = File(path)
                        val uri = Uri.fromFile(file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
                    }
                },
                "Refresh Attendance" to {
                    coroutineScope.launch {
                        val attendanceResponse = getAttendance(studentId)
                        updateAttendance(attendanceResponse, attendanceHistory)
                        val todayResponse = getTodayAttendance(studentId)
                        updateTodayAttendance(todayResponse, todayAttendance)
                        Toast.makeText(context, "Attendance refreshed", Toast.LENGTH_SHORT).show()
                    }
                },
                "Change Password" to { currentView = StudentView.CHANGE_PASSWORD },
                "Logout" to onLogout
            )
        )
    }
}