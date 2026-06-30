package com.example.digitaluniversity.courses

data class Course(
    val courseId: String = "",
    val courseCode: String = "",
    val courseName: String = "",
    val credits: Int = 0,
    val description: String = "",
    val lecturerName: String = "",
    val lectureId: String = "",

    val schedules: List<CourseSchedule> = emptyList()
)

fun Course.firstScheduleDisplay(): String {
    val s = schedules.firstOrNull() ?: return "No schedule"
    return "${s.day} ${s.startTime} - ${s.endTime}"
}

fun Course.firstRoom(): String = schedules.firstOrNull()?.room ?: "No room"