package com.example.attendanceapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import com.example.attendanceapp.ui.theme.AttendanceAppTheme
import com.example.attendanceapp.UserRole
import com.example.attendanceapp.StudentDashboard
import com.example.attendanceapp.ui.theme.TeacherDashboard
import com.example.attendanceapp.AdminDashboard
import com.example.attendanceapp.LoginScreen
import kotlinx.coroutines.delay
import com.example.attendanceapp.ui.theme.AddStudentScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AttendanceAppTheme {
                var showSplash by remember { mutableStateOf(true) }
                var userRole by remember { mutableStateOf<UserRole?>(null) }
                val navController = rememberNavController()

                if (showSplash) {
                    SplashScreen(onFinish = { showSplash = false })
                } else {
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(
                                modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues()),
                                onLoginSuccess = { studentId, fullName, loginRole ->
                                    userRole = when (loginRole.lowercase()) {
                                        "student" -> UserRole.Student(studentId, fullName)
                                        "teacher" -> UserRole.Teacher(fullName)
                                        "admin" -> UserRole.Admin(fullName)
                                        else -> null
                                    }
                                    Toast.makeText(this@MainActivity, "Welcome $fullName", Toast.LENGTH_LONG).show()
                                    navController.navigate("dashboard")
                                }
                            )
                        }

                        composable("dashboard") {
                            when (val role = userRole) {
                                is UserRole.Student -> StudentDashboard(
                                    studentId = role.studentId,
                                    fullName = role.fullName,
                                    onLogout = {
                                        userRole = null
                                        navController.navigate("login")
                                    },
                                    navController = navController
                                )
                                is UserRole.Teacher -> TeacherDashboard(
                                    fullName = role.fullName,
                                    onLogout = {
                                        userRole = null
                                        navController.navigate("login")
                                    },
                                    navController = navController
                                )
                                is UserRole.Admin -> AdminDashboard(
                                    fullName = role.fullName,
                                    onLogout = {
                                        userRole = null
                                        navController.navigate("login")
                                    },
                                    navController = navController
                                )
                                else -> {}
                            }
                        }

                        composable("addStudent") {
                            AddStudentScreen(onBack = { navController.navigate("dashboard") })
                        }

                        // Add more screens like "viewProfile", "changePassword", etc.
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000)
        onFinish()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Welcome to Brindavan College of Engineering",
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }
    }
}