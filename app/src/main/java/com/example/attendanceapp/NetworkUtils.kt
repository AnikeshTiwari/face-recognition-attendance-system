package com.example.attendanceapp

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.time.LocalTime
import android.os.Build
import java.util.Calendar
import com.example.attendanceapp.data.TeacherProfileModel
import com.example.attendanceapp.data.StudentProfile



// 🔹 Login
suspend fun sendLoginRequest(username: String, password: String): Pair<String, String> = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.LOGIN_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        val postData = "username=${URLEncoder.encode(username, "UTF-8")}&password=${URLEncoder.encode(password, "UTF-8")}"
        conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))

        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)

        val status = json.optString("status", "error")
        val studentId = json.optString("student_id", "")
        val fullName = json.optString("full_name", "")
        val role = json.optString("role", "")
        val message = if (status == "success") "$studentId|$fullName|$role" else json.optString("message", "Login failed")

        Pair(status, message)
    } catch (e: Exception) {
        Pair("error", "Network error: ${e.localizedMessage}")
    }
}


// 🔹 System Health
suspend fun checkSystemHealth(): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.HEALTH_CHECK_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val response = connection.inputStream
            .bufferedReader()
            .use { it.readText() }

        val json = JSONObject(response)

        val status = json.optString("status", "error")
        val message = json.optString("message", "Unknown system state")

        if (status == "ok") {
            "System Health: $message ✅"
        } else {
            "System Health Issue: $message ❌"
        }

    } catch (e: Exception) {
        Log.e("SystemHealth", "Health check failed", e)
        "System Health: Failed to check"
    }
}

// 🔹 Audit Logs
suspend fun fetchAuditLogs(): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.LOGS_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        "{\"status\":\"error\",\"message\":\"${e.localizedMessage}\"}"
    }
}

fun updateAuditLogs(
    response: String,
    list: SnapshotStateList<AuditLog>
) {
    list.clear()
    try {
        val json = JSONObject(response)
        if (json.optString("status") == "success") {
            val logs = json.getJSONArray("logs")
            for (i in 0 until logs.length()) {
                val item = logs.getJSONObject(i)
                list.add(
                    AuditLog(
                        timestamp = item.optString("timestamp"),
                        action = item.optString("action"),
                        user = item.optString("user")
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.e("AuditLogs", "Parse error", e)
    }
}

data class AuditLog(
    val timestamp: String,
    val action: String,
    val user: String
)


// 🔹 Analytics
suspend fun fetchAnalytics(): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.ANALYTICS_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        "{\"status\":\"error\",\"message\":\"${e.localizedMessage}\"}"
    }
}

fun updateAnalytics(response: String, list: SnapshotStateList<String>) {
    list.clear()
    try {
        val json = JSONObject(response)
        if (json.getString("status") == "success") {
            val summary = json.getJSONArray("summary")
            if (summary.length() == 0) {
                list.add("No analytics available")
            } else {
                for (i in 0 until summary.length()) {
                    list.add("• ${summary.getString(i)}")
                }
            }
        } else {
            list.add("Error: ${json.getString("message")}")
        }
    } catch (e: Exception) {
        list.add("Failed to parse analytics: ${e.localizedMessage}")
    }
}

// 🔹 Password Update
suspend fun updatePassword(username: String, newPassword: String): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.PASSWORD_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val postData = "username=${URLEncoder.encode(username, "UTF-8")}&new_password=${URLEncoder.encode(newPassword, "UTF-8")}"
        conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        "{\"status\":\"error\",\"message\":\"${e.localizedMessage}\"}"
    }
}

// 🔹 Attendance Parsing
fun updateAttendance(response: String, attendanceHistory: SnapshotStateList<String>) {
    attendanceHistory.clear()
    try {
        val json = JSONObject(response)
        if (json.getString("status") == "success") {
            val records = json.getJSONArray("attendance")
            for (i in 0 until records.length()) {
                val obj = records.getJSONObject(i)
                val line = "${obj.getString("date")} | Period ${obj.getInt("period")} | ${
                    obj.getString("status").replaceFirstChar { it.uppercase(Locale.ROOT) }
                }"
                attendanceHistory.add(line)
            }
        } else {
            attendanceHistory.add("Error: ${json.getString("message")}")
        }
    } catch (e: Exception) {
        attendanceHistory.add("Failed to parse attendance")
    }
}

fun updateTodayAttendance(response: String, todayAttendance: SnapshotStateList<String>) {
    todayAttendance.clear()
    try {
        val json = JSONObject(response)
        if (json.getString("status") == "success") {
            val records = json.getJSONArray("attendance")
            for (i in 0 until records.length()) {
                val obj = records.getJSONObject(i)
                val line = "Period ${obj.getInt("period")}: ${
                    obj.getString("status").replaceFirstChar { it.uppercase(Locale.ROOT) }
                }"
                todayAttendance.add(line)
            }
        } else {
            todayAttendance.add("Error: ${json.getString("message")}")
        }
    } catch (e: Exception) {
        todayAttendance.add("Failed to parse today's attendance")
    }
}

// 🔹 Auto-Mark Attendance
suspend fun markAttendanceIfNotExists(studentId: String, date: String, period: Int, status: String): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.MARK_ATTENDANCE_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val postData = "student_id=${URLEncoder.encode(studentId, "UTF-8")}" +
                "&date=${URLEncoder.encode(date, "UTF-8")}" +
                "&period=${URLEncoder.encode(period.toString(), "UTF-8")}" +
                "&status=${URLEncoder.encode(status, "UTF-8")}"
        conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        "{\"status\":\"error\",\"message\":\"${e.localizedMessage}\"}"
    }
}

// 🔹 Determine Current Period Based on Time
fun getCurrentPeriod(): Int {
    val hour = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        java.time.LocalTime.now().hour
    } else {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }

    return when (hour) {
        in 9..10 -> 1
        in 11..12 -> 2
        in 13..14 -> 3
        else -> 0
    }
}
// 🔹 Teacher Profile
suspend fun fetchTeacherProfile(teacherId: String): TeacherProfileModel? = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.TEACHER_PROFILE_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val postData = "teacher_id=${URLEncoder.encode(teacherId, "UTF-8")}"
        conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))
        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        if (json.getString("status") == "success") {
            val obj = json.getJSONObject("profile")
            TeacherProfileModel(
                name = obj.getString("name"),
                phone = obj.getString("phone"),
                email = obj.getString("email"),
                emergencyContact = obj.getString("emergency_contact")
            )
        } else null
    } catch (e: Exception) {
        Log.e("TeacherProfileError", e.localizedMessage ?: "Unknown error")
        null
    }
}
// 🔹 Fetch Full Attendance
suspend fun getAttendance(studentId: String): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.ATTENDANCE_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val postData = "student_id=${URLEncoder.encode(studentId, "UTF-8")}"
        conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        "{\"status\":\"error\",\"message\":\"${e.localizedMessage}\"}"
    }
}

// 🔹 Fetch Today's Attendance
suspend fun getTodayAttendance(studentId: String): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.TODAY_ATTENDANCE_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val postData = "student_id=${URLEncoder.encode(studentId, "UTF-8")}"
        conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        "{\"status\":\"error\",\"message\":\"${e.localizedMessage}\"}"
    }
}

// 🔹 Fetch Student Profile
suspend fun fetchStudentProfile(studentId: String): StudentProfile? = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.PROFILE_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val postData = "student_id=${URLEncoder.encode(studentId, "UTF-8")}"
        conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))
        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        if (json.getString("status") == "success") {
            val obj = json.getJSONObject("profile")
            StudentProfile(
                name = obj.getString("name"),
                email = obj.getString("email"),
                phone = obj.getString("phone"),
                parentPhone = obj.getString("parent_phone"),
                address = obj.getString("address"),
                emergencyContact = obj.getString("emergency_contact")
            )
        } else null
    } catch (e: Exception) {
        Log.e("StudentProfileError", e.localizedMessage ?: "Unknown error")
        null
    }
}

// 🔹 Fetch Student Results
suspend fun getResults(studentId: String): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.RESULTS_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val postData = "student_id=${URLEncoder.encode(studentId, "UTF-8")}"
        conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        "{\"status\":\"error\",\"message\":\"${e.localizedMessage}\"}"
    }
}

suspend fun uploadResult(studentId: String, subject: String, marks: String, teacherId: String): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.UPLOAD_RESULT_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val postData = "student_id=${URLEncoder.encode(studentId, "UTF-8")}" +
                "&subject=${URLEncoder.encode(subject, "UTF-8")}" +
                "&marks=${URLEncoder.encode(marks, "UTF-8")}" +
                "&teacher_id=${URLEncoder.encode(teacherId, "UTF-8")}"
        conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        "{\"status\":\"error\",\"message\":\"${e.localizedMessage}\"}"
    }
}
suspend fun addTeacher(
    name: String, phone: String, email: String, password: String, emergencyContact: String
): String = withContext(Dispatchers.IO) {
    try {
        val postData = "name=${URLEncoder.encode(name, "UTF-8")}" +
                "&phone=${URLEncoder.encode(phone, "UTF-8")}" +
                "&email=${URLEncoder.encode(email, "UTF-8")}" +
                "&password=${URLEncoder.encode(password, "UTF-8")}" +
                "&emergency_contact=${URLEncoder.encode(emergencyContact, "UTF-8")}"
        val conn = URL(ApiConfig.ADD_TEACHER_URL).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        "{\"status\":\"error\",\"message\":\"${e.localizedMessage}\"}"
    }
}

suspend fun addStudent(
    name: String,
    email: String,
    phone: String,
    parentPhone: String,
    address: String,
    emergency: String,
    password: String
): String = withContext(Dispatchers.IO) {
    try {
        val url = URL(ApiConfig.ADD_STUDENT_URL)

        val postData = listOf(
            "name" to name,
            "email" to email,
            "phone" to phone,
            "parent_phone" to parentPhone,
            "address" to address,
            "emergency_contact" to emergency,
            "password" to password
        ).joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        conn.outputStream.use {
            it.write(postData.toByteArray(Charsets.UTF_8))
        }

        conn.inputStream.bufferedReader().readText()

    } catch (e: Exception) {
        """{"status":"error","message":"Network error"}"""
    }
}
