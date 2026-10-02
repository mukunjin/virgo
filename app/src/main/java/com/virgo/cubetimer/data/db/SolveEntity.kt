package com.virgo.cubetimer.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单条成绩。字段语义对齐 csTimer 的成绩记录
 * `[[penalty, ...phaseEnds], scramble, comment, startTimestamp, extension?]`。
 */
@Entity(
    tableName = "solves",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SolveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 所属会话。 */
    val sessionId: Long,
    /** 会话内序号（从 0 递增，用于稳定排序）。 */
    val seq: Int,
    /**
     * 罚时：`0` 正常、`2000` 表示 +2、`-1` 表示 DNF。
     * 与 csTimer 的取值完全一致。
     */
    val penalty: Int,
    /** 最终用时（毫秒，不含 +2 加成；展示时按 penalty 处理）。 */
    val totalMs: Long,
    /** 分段用时（毫秒，逗号分隔）；单阶段时为空串。 */
    val splitsCsv: String = "",
    /** 本成绩对应的打乱。 */
    val scramble: String,
    /** 备注。 */
    val comment: String = "",
    /** 开始时间（Unix 秒）。 */
    val startEpochSec: Long,
)