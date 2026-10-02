package com.virgo.cubetimer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 会话（csTimer 的 "session"）：一组成绩的容器。
 * 对应 csTimer IndexedDB 中 `sessions` 存储的会话元信息。
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 会话名，如 "会话 1"。 */
    val name: String,
    /** 展示排序（越小越靠前）。 */
    val orderIndex: Int,
    /** 打乱类型，本期仅 "333"。 */
    val scrambleType: String = "333",
    /** 创建时间（Unix 秒）。 */
    val createdAt: Long,
)