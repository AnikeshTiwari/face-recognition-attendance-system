package com.example.attendanceapp

data class StudentProfile(
    val name: String,
    val email: String,
    val phone: String,
    val parentPhone: String,
    val address: String,
    val emergencyContact: String
)

data class TeacherDashboard(
    val name: String,
    val phone: String,
    val email: String,
    val password: String,
    val emergencyContact: String
)