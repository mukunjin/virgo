package com.virgo.cubetimer.data.repo

import com.virgo.cubetimer.data.db.SessionEntity
import com.virgo.cubetimer.data.db.SolveEntity
import com.virgo.cubetimer.data.db.VirgoDao
import kotlinx.coroutines.flow.Flow

/**
 * 会话与成绩仓库。对上层屏蔽数据库细节。
 *
 * 罚时约定与 csTimer 一致：`0` 正常、`2000` 表示 +2、`-1` 表示 DNF。
 */
class SessionRepository(private val dao: VirgoDao) {

    fun observeSessions(): Flow<List<SessionEntity>> = dao.observeSessions()

    fun observeSolves(sessionId: Long): Flow<List<SolveEntity>> = dao.observeSolves(sessionId)

    /** 确保至少存在一个分组（首次启动时创建默认分组）。 */
    suspend fun ensureDefaultSession(): Long {
        val existing = dao.sessions()
        if (existing.isNotEmpty()) {
            // 兼容旧版本数据：把历史的「会话 N」改名为「分组 N」
            existing.filter { it.name.startsWith("会话 ") }
                .forEach { dao.renameSession(it.id, it.name.replaceFirst("会话 ", "分组 ")) }
            return existing.first().id
        }
        return dao.insertSession(
            SessionEntity(
                name = "分组 1",
                orderIndex = 0,
                createdAt = System.currentTimeMillis() / 1000,
            )
        )
    }

    /**
     * 生成不与现有分组重名的名字：取现有名字中最大编号 + 1。
     * 直接用「数量 + 1」会在删除后产生重名（如删除「分组 1」后再新建又得到「分组 2」）。
     */
    private suspend fun nextGroupName(): String {
        val maxIndex = dao.sessions()
            .mapNotNull { it.name.substringAfterLast(' ').toIntOrNull() }
            .maxOrNull() ?: 0
        return "分组 ${maxIndex + 1}"
    }

    /** 新建分组（名字自动编号，保证不重名）。 */
    suspend fun addSession(): Long {
        val order = dao.sessionCount()
        return dao.insertSession(
            SessionEntity(
                name = nextGroupName(),
                orderIndex = order,
                createdAt = System.currentTimeMillis() / 1000,
            )
        )
    }

    suspend fun renameSession(id: Long, name: String) = dao.renameSession(id, name)

    suspend fun deleteSession(session: SessionEntity) = dao.deleteSession(session)

    suspend fun solvesOf(sessionId: Long): List<SolveEntity> = dao.solvesOf(sessionId)

    /** 追加一条成绩，自动分配 seq。 */
    suspend fun appendSolve(
        sessionId: Long,
        penalty: Int,
        totalMs: Long,
        scramble: String,
        splitsCsv: String = "",
        comment: String = "",
        startEpochSec: Long = System.currentTimeMillis() / 1000,
    ): Long {
        val seq = dao.maxSeq(sessionId) + 1
        return dao.insertSolve(
            SolveEntity(
                sessionId = sessionId,
                seq = seq,
                penalty = penalty,
                totalMs = totalMs,
                splitsCsv = splitsCsv,
                scramble = scramble,
                comment = comment,
                startEpochSec = startEpochSec,
            )
        )
    }

    suspend fun updateSolve(solve: SolveEntity) = dao.updateSolve(solve)

    /** 设置罚时。`penalty` 取 `0` / `2000` / `-1`。 */
    suspend fun setPenalty(solve: SolveEntity, penalty: Int) =
        dao.updateSolve(solve.copy(penalty = penalty))

    /** 删除成绩并重排同会话剩余成绩的 seq。 */
    suspend fun deleteSolve(solve: SolveEntity) {
        dao.deleteSolve(solve)
        dao.solvesOf(solve.sessionId).forEachIndexed { index, s ->
            if (s.seq != index) {
                dao.updateSolveSeq(s.id, index)
            }
        }
    }

    /** 导出为 csTimer 兼容文本（每行一条时间）。 */
    suspend fun exportText(sessionId: Long): String {
        val solves = dao.solvesOf(sessionId)
        return solves.joinToString("\n") { formatTime(it) }
    }

    private fun formatTime(solve: SolveEntity): String {
        if (solve.penalty == -1) {
            return "DNF"
        }
        val ms = solve.totalMs + if (solve.penalty == 2000) 2000 else 0
        val suffix = if (solve.penalty == 2000) "+" else ""
        val sec = ms / 1000.0
        return String.format("%.2f%s", sec, suffix)
    }
}