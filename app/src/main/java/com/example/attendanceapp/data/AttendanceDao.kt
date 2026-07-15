package com.example.attendanceapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.attendanceapp.data.AttendanceEntity

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceEntity>)

    @Query("SELECT * FROM attendance ORDER BY date DESC")
    suspend fun getAll(): List<AttendanceEntity>

    @Query("DELETE FROM attendance")
    suspend fun clearAll()
}