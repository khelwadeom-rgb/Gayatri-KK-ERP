package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.model.AuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insertAuditLog(log: AuditLog)

    @Query("SELECT * FROM audit_logs ORDER BY id DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>
}
