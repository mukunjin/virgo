package com.virgo.cubetimer.data.repo

import android.content.Context
import android.content.SharedPreferences

/**
 * 设置仓库。键名沿用 csTimer 的属性名，便于对照与后续迁移。
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 打乱类型，本期固定 333。 */
    var scrambleType: String
        get() = prefs.getString(KEY_SCR_TYPE, "333") ?: "333"
        set(value) = prefs.edit().putString(KEY_SCR_TYPE, value).apply()

    /** 是否开启 15 秒观察。 */
    var useInspection: Boolean
        get() = prefs.getBoolean(KEY_USE_INS, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_INS, value).apply()

    /** 打乱文本是否自动换行。 */
    var scrambleWrap: Boolean
        get() = prefs.getBoolean(KEY_SCR_WRAP, true)
        set(value) = prefs.edit().putBoolean(KEY_SCR_WRAP, value).apply()

    /** 打乱文本水平对齐：0 左、1 居中、2 右。 */
    var scrambleAlign: Int
        get() = prefs.getInt(KEY_SCR_ALIGN, 1)
        set(value) = prefs.edit().putInt(KEY_SCR_ALIGN, value).apply()

    /** 计时显示精度（小数位）。 */
    var displayPrecision: Int
        get() = prefs.getInt(KEY_DIS_PREC, 2)
        set(value) = prefs.edit().putInt(KEY_DIS_PREC, value).apply()

    /** 是否精确到毫秒（小数点后三位），默认开启。 */
    var useMilli: Boolean
        get() = prefs.getBoolean(KEY_USE_MILLI, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_MILLI, value).apply()

    /** 统计修剪规则，默认 "p5"（去掉最好/最差各 5%）。 */
    var trim: String
        get() = prefs.getString(KEY_TRIM, "p5") ?: "p5"
        set(value) = prefs.edit().putString(KEY_TRIM, value).apply()

    /** 修剪偏高侧规则，默认 "a"。 */
    var trimRight: String
        get() = prefs.getString(KEY_TRIMR, "a") ?: "a"
        set(value) = prefs.edit().putString(KEY_TRIMR, value).apply()

    /** 当前选中的会话 id（0 表示尚未确定）。 */
    var currentSessionId: Long
        get() = prefs.getLong(KEY_CUR_SESSION, 0L)
        set(value) = prefs.edit().putLong(KEY_CUR_SESSION, value).apply()

    private companion object {
        const val PREFS_NAME = "virgo_settings"

        const val KEY_SCR_TYPE = "scrType"
        const val KEY_USE_INS = "useIns"
        const val KEY_SCR_WRAP = "scrWrap"
        const val KEY_SCR_ALIGN = "scrAlign"
        const val KEY_DIS_PREC = "disPrec"
        const val KEY_USE_MILLI = "useMilli"
        const val KEY_TRIM = "trim"
        const val KEY_TRIMR = "trimr"
        const val KEY_CUR_SESSION = "curSession"
    }
}