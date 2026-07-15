package com.example.attendanceapp

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import com.example.attendanceapp.data.StudentProfile
import com.example.attendanceapp.ApiConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AdminView {
    DASHBOARD, PROFILE, ADD_TEACHER, CHANGE_PASSWORD
}

@Composable
fun AdminDashboard(fullName: String, onLogout: () -> Unit, navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentView by remember { mutableStateOf(AdminView.DASHBOARD) }

    var systemStatus by remember { mutableStateOf("Checking...") }
    val auditLogs = remember { mutableStateListOf<AuditLog>() }
    val analyticsSummary = remember { mutableStateListOf<String>() }

    var newPassword by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var teacherPhone by remember { mutableStateOf("") }
    var teacherEmail by remember { mutableStateOf("") }
    var teacherPassword by remember { mutableStateOf("") }
    var teacherEmergency by remember { mutableStateOf("") }
    var teacherDepartment by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            systemStatus = checkSystemHealth()
            updateAuditLogs(fetchAuditLogs(), auditLogs)
            updateAnalytics(fetchAnalytics(), analyticsSummary)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentView) {
            AdminView.DASHBOARD -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.statusBars.asPaddingValues())
                        .padding(24.dp)
                ) {
                    Text("Admin Dashboard", style = MaterialTheme.typography.headlineMedium)
                    Text("Welcome, $fullName", style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("System Health: $systemStatus", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Audit Logs", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Spacer(modifier = Modifier.height(8.dp))

                    Column {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Timestamp", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            Text("Action", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium)
                            Text("User", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        }

                        Divider()

                        // Table Rows
                        auditLogs.forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.timestamp, modifier = Modifier.weight(1f))
                                Text(log.action, modifier = Modifier.weight(1.5f))
                                Text(log.user, modifier = Modifier.weight(1f))
                            }
                            Divider()
                        }
                    }

                }
            }

            AdminView.PROFILE -> {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text("Admin Profile", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Name: $fullName")
                    Text("Role: Admin")
                    Text("Access Level: Full")
                    Text("Contact: admin@campus.com")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { currentView = AdminView.DASHBOARD }, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to Dashboard")
                    }
                }
            }

            AdminView.ADD_TEACHER -> {

                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text("Add New Teacher", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { teacherName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = teacherPhone,
                        onValueChange = { teacherPhone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = teacherEmail,
                        onValueChange = { teacherEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = teacherPassword,
                        onValueChange = { teacherPassword = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
//                    OutlinedTextField(
//                        value = teacherEmergency,
//                        onValueChange = { teacherEmergency = it },
//                        label = { Text("Emergency Contact") },
//                        modifier = Modifier.fillMaxWidth()
//                    )
                    OutlinedTextField(
                        value = teacherDepartment,
                        onValueChange = { teacherDepartment = it },
                        label = { Text("Department") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (
                            teacherName.isBlank() || teacherPhone.isBlank() || teacherEmail.isBlank() ||
                            teacherPassword.isBlank() || teacherDepartment.isBlank()
                        )
                        {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        } else {
                            coroutineScope.launch {
                                val response = addTeacherWithDepartment(
                                    teacherName,
                                    teacherPhone,
                                    teacherEmail,
                                    teacherPassword,
                                    teacherDepartment
                                )

                                val message = try {
                                    JSONObject(response).optString("message", "Teacher added")
                                } catch (e: Exception) {
                                    "Failed to parse response"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                teacherName = ""; teacherPhone = ""; teacherEmail = ""
                                teacherPassword = ""; teacherEmergency = ""; teacherDepartment = ""
                                currentView = AdminView.DASHBOARD
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Submit")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { currentView = AdminView.DASHBOARD }) {
                        Text("Cancel")
                    }
                }
            }
            AdminView.CHANGE_PASSWORD -> {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text("Change Password", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (newPassword.length < 6) {
                            Toast.makeText(context, "Password too short", Toast.LENGTH_SHORT).show()
                        } else {
                            coroutineScope.launch {
                                val response = updatePassword(fullName, newPassword)
                                val message = JSONObject(response).optString("message", "Password updated")
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                newPassword = ""
                                currentView = AdminView.DASHBOARD
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Update Password")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { currentView = AdminView.DASHBOARD }) {
                        Text("Cancel")
                    }
                }
            }
        }

        RoverButton(
            actions = listOf(
                "View Profile" to { currentView = AdminView.PROFILE },
                "Export Attendance & Results" to {
                    exportDataToPdf(context, auditLogs, analyticsSummary)
                },
                "Add Teacher" to { currentView = AdminView.ADD_TEACHER },
                "Change Password" to { currentView = AdminView.CHANGE_PASSWORD },
                "Logout" to onLogout
            )
        )
    }
}

fun exportDataToPdf(
    context: android.content.Context,
    auditLogs: List<AuditLog>,
    analytics: List<String>
) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        var y = 40f
        canvas.drawText("=== Audit Logs ===", 40f, y, paint)
        y += 20f

        auditLogs.forEach {
            canvas.drawText("${it.timestamp} | ${it.action} | ${it.user}", 40f, y, paint)
            y += 16f
        }

        y += 20f
        canvas.drawText("=== Analytics Summary ===", 40f, y, paint)
        y += 16f

        analytics.forEach {
            canvas.drawText(it, 40f, y, paint)
            y += 16f
        }

        pdfDocument.finishPage(page)

        val fileName = "attendance_export_${System.currentTimeMillis()}.pdf"

        // 🔹 MediaStore (CORRECT WAY)
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        )

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { output ->
                pdfDocument.writeTo(output)
            }

            Toast.makeText(
                context,
                "PDF saved to Downloads 📄",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_LONG).show()
        }

        pdfDocument.close()

    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

suspend fun addTeacherWithDepartment(
    name: String,
    phone: String,
    email: String,
    password: String,
    department: String
): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.ADD_TEACHER_URL)

        val postData = listOf(
            "name" to name,
            "phone" to phone,
            "email" to email,
            "password" to password,
            "department" to department
        ).joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty(
            "Content-Type",
            "application/x-www-form-urlencoded"
        )

        connection.outputStream.use { output ->
            output.write(postData.toByteArray(Charsets.UTF_8))
        }

        connection.inputStream.bufferedReader().use { it.readText() }

    } catch (e: Exception) {
        Log.e("AddTeacher", "Exception occurred", e)
        """{ "message": "Network error" }"""
    }
}
