package com.virgo.cubetimer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 会话与成绩的数据库访问接口。 */
@Dao
interface VirgoDao {

    // ---- 会话 ----

    @Query("SELECT * FROM sessions ORDER BY orderIndex ASC, id ASC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY orderIndex ASC, id ASC")
    suspend fun sessions(): List<SessionEntity>

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun sessionCount(): Int

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("UPDATE sessions SET name = :name WHERE id = :id")
    suspend fun renameSession(id: Long, name: String)

    @Query("UPDATE sessions SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun reorderSession(id: Long, orderIndex: Int)

    // ---- 成绩 ----

    @Query("SELECT * FROM solves WHERE sessionId = :sessionId ORDER BY seq ASC, id ASC")
    fun observeSolves(sessionId: Long): Flow<List<SolveEntity>>

    @Query("SELECT * FROM solves WHERE sessionId = :sessionId ORDER BY seq ASC, id ASC")
    suspend fun solvesOf(sessionId: Long): List<SolveEntity>

    @Query("SELECT COALESCE(MAX(seq), -1) FROM solves WHERE sessionId = :sessionId")
    suspend fun maxSeq(sessionId: Long): Int

    @Insert
    suspend fun insertSolve(solve: SolveEntity): Long

    @Update
    suspend fun updateSolve(solve: SolveEntity)

    @Delete
    suspend fun deleteSolve(solve: SolveEntity)

    @Query("DELETE FROM solves WHERE sessionId = :sessionId")
    suspend fun deleteSolvesOf(sessionId: Long)

    /** 删除后重排剩余成绩的 seq，保持连续。 */
    @Query("UPDATE solves SET seq = :seq WHERE id = :id")
    suspend fun updateSolveSeq(id: Long, seq: Int)
}