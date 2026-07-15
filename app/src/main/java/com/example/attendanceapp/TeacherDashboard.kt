package com.example.attendanceapp.ui.theme

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.attendanceapp.data.TeacherProfileModel
import com.example.attendanceapp.data.StudentProfile
import com.example.attendanceapp.RoverButton
import com.example.attendanceapp.fetchTeacherProfile
import com.example.attendanceapp.uploadResult
import com.example.attendanceapp.fetchStudentProfile
import com.example.attendanceapp.addStudent
import com.example.attendanceapp.updatePassword
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class TeacherView {
    DASHBOARD, PROFILE, STUDENT_INFO, ADD_STUDENT, CHANGE_PASSWORD
}

@Composable
fun TeacherDashboard(fullName: String, onLogout: () -> Unit, navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentView by remember { mutableStateOf(TeacherView.DASHBOARD) }

    var newPassword by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var marks by remember { mutableStateOf("") }

    var studentQuery by remember { mutableStateOf("") }
    var studentProfile by remember { mutableStateOf<StudentProfile?>(null) }
    var teacherProfile by remember { mutableStateOf<TeacherProfileModel?>(null) }
    var studentSearched by remember { mutableStateOf(false) }

    var studentName by remember { mutableStateOf("") }
    var studentEmail by remember { mutableStateOf("") }
    var studentPhone by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var studentAddress by remember { mutableStateOf("") }
    var studentEmergency by remember { mutableStateOf("") }
    var studentPassword by remember { mutableStateOf("") }


    LaunchedEffect(Unit) {
        coroutineScope.launch {
            teacherProfile = fetchTeacherProfile(fullName)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentView) {
            TeacherView.DASHBOARD -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.statusBars.asPaddingValues())
                        .padding(24.dp)
                ) {
                    Text("Teacher Dashboard", style = MaterialTheme.typography.headlineMedium)
                    Text("Welcome, $fullName", style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Upload Result:", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = studentId, onValueChange = { studentId = it }, label = { Text("Student ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = marks, onValueChange = { marks = it }, label = { Text("Marks") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (studentId.isBlank() || subject.isBlank() || marks.isBlank()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        } else {
                            coroutineScope.launch {
                                try {
                                    val response = uploadResult(studentId, subject, marks, fullName)
                                    val message = try {
                                        JSONObject(response).optString("message", "Upload successful")
                                    } catch (e: Exception) {
                                        "Upload failed: Invalid response format"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Upload Result")
                    }
                }
            }

            TeacherView.PROFILE -> {
                teacherProfile?.let {
                    TeacherInfoCard(it)
                } ?: Text("Loading teacher profile...")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { currentView = TeacherView.DASHBOARD }, modifier = Modifier.padding(24.dp)) {
                    Text("Back to Dashboard")
                }
            }

            TeacherView.STUDENT_INFO -> {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = studentQuery,
                        onValueChange = { studentQuery = it },
                        label = { Text("Enter Student ID or Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        coroutineScope.launch {
                            studentSearched = true
                            studentProfile = fetchStudentProfile(studentQuery)
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Search")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    when {
                        studentProfile != null -> StudentInfoCard(studentProfile!!)
                        studentSearched -> Text("No student data found.")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { currentView = TeacherView.DASHBOARD }, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to Dashboard")
                    }
                }
            }

            TeacherView.ADD_STUDENT -> {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Add New Student", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = studentName, onValueChange = { studentName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = studentEmail, onValueChange = { studentEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = studentPhone, onValueChange = { studentPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = parentPhone, onValueChange = { parentPhone = it }, label = { Text("Parent Phone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = studentAddress, onValueChange = { studentAddress = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = studentEmergency, onValueChange = { studentEmergency = it }, label = { Text("Emergency Contact") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = studentPassword,
                        onValueChange = { studentPassword = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (
                            studentName.isBlank() || studentEmail.isBlank() || studentPhone.isBlank() ||
                            parentPhone.isBlank() || studentAddress.isBlank() ||
                            studentEmergency.isBlank() || studentPassword.isBlank()
                        )
                        {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        } else {
                            coroutineScope.launch {
                                val response = addStudent(
                                    studentName, studentEmail, studentPhone,
                                    parentPhone, studentAddress, studentEmergency, studentPassword
                                )
                                val message = try {
                                    JSONObject(response).optString("message", "Student added")
                                } catch (e: Exception) {
                                    "Failed to parse response"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                studentName = ""
                                studentEmail = ""
                                studentPhone = ""
                                parentPhone = ""
                                studentAddress = ""
                                studentEmergency = ""
                                studentPassword = ""

                                currentView = TeacherView.DASHBOARD
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Add Student")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { currentView = TeacherView.DASHBOARD }) {
                        Text("Cancel")
                    }
                }
            }

            TeacherView.CHANGE_PASSWORD -> {
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
                                updatePassword(fullName, newPassword)
                                Toast.makeText(context, "Password update requested", Toast.LENGTH_SHORT).show()
                                newPassword = ""
                                currentView = TeacherView.DASHBOARD
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Change Password")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { currentView = TeacherView.DASHBOARD }) {
                        Text("Cancel")
                    }
                }
            }
        }

        RoverButton(
            actions = listOf(
                "View Profile" to { currentView = TeacherView.PROFILE },
                "View Student Info" to { currentView = TeacherView.STUDENT_INFO },
                "Add Student" to { currentView = TeacherView.ADD_STUDENT },
                "Change Password" to { currentView = TeacherView.CHANGE_PASSWORD },
                "Logout" to onLogout
            )
        )
    }
}

@Composable
fun TeacherInfoCard(profile: TeacherProfileModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Teacher Profile", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Name: ${profile.name}", style = MaterialTheme.typography.bodyMedium)
        Text("Phone: ${profile.phone}", style = MaterialTheme.typography.bodyMedium)
        Text("Email: ${profile.email}", style = MaterialTheme.typography.bodyMedium)
        Text("Emergency Contact: ${profile.emergencyContact}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun StudentInfoCard(profile: StudentProfile) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Student Info", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Name: ${profile.name}", style = MaterialTheme.typography.bodyMedium)
        Text("Email: ${profile.email}", style = MaterialTheme.typography.bodyMedium)
        Text("Phone: ${profile.phone}", style = MaterialTheme.typography.bodyMedium)
        Text("Parent's Phone: ${profile.parentPhone}", style = MaterialTheme.typography.bodyMedium)
        Text("Address: ${profile.address}", style = MaterialTheme.typography.bodyMedium)
        Text("Emergency Contact: ${profile.emergencyContact}", style = MaterialTheme.typography.bodyMedium)
    }
}