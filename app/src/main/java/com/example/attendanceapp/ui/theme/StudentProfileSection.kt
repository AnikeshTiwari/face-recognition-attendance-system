package com.example.attendanceapp.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceapp.data.StudentProfile


@Composable
fun StudentProfileSection(profile: StudentProfile) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Student Profile", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Name: ${profile.name}")
        Text("Email: ${profile.email}")
        Text("Phone: ${profile.phone}")
        Text("Parent's Phone: ${profile.parentPhone}")
        Text("Address: ${profile.address}")
        Text("Emergency Contact: ${profile.emergencyContact}")
    }
}