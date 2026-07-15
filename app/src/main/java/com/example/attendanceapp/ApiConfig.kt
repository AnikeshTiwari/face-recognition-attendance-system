package com.example.attendanceapp

/**
 * ngrok http 8080 for starting the ngrok
 */

object ApiConfig {
    const val BASE_URL = "https://91931f1d54fb.ngrok-free.app/attendance_system/"

    // 🔐 Authentication
    const val LOGIN_URL = "${BASE_URL}login.php"
    const val PASSWORD_URL = "${BASE_URL}update_password.php"

    // 👤 Profiles
    const val PROFILE_URL = "${BASE_URL}get_student_profile.php"
    const val TEACHER_PROFILE_URL = "${BASE_URL}get_teacher_profile.php"

    // 📊 Results & Analytics
    const val UPLOAD_RESULT_URL = "${BASE_URL}upload_result.php"
    const val RESULTS_URL = "${BASE_URL}get_results.php"
    const val ANALYTICS_URL = "${BASE_URL}get_analytics.php"

    // 📅 Attendance
    const val ATTENDANCE_URL = "${BASE_URL}get_attendance.php"
    const val TODAY_ATTENDANCE_URL = "${BASE_URL}get_today_attendance.php"
    const val MARK_ATTENDANCE_URL = "${BASE_URL}mark_attendance.php"

    // 🛠️ Admin & System
    const val LOGS_URL = "${BASE_URL}get_logs.php"
    const val HEALTH_CHECK_URL = "${BASE_URL}check_health.php"

    const val ADD_TEACHER_URL = "${BASE_URL}add_teacher.php"
    const val ADD_STUDENT_URL = "${BASE_URL}add_student.php"
}