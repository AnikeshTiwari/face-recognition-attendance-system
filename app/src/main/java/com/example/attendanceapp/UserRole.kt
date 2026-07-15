package com.example.attendanceapp

sealed class UserRole {
    data class Student(val studentId: String, val fullName: String) : UserRole()
    data class Teacher(val fullName: String) : UserRole()
    data class Admin(val fullName: String) : UserRole()
}