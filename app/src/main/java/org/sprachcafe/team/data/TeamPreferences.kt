package org.sprachcafe.team.data

import android.content.Context
import android.content.SharedPreferences

class TeamPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sprachcafe_team_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MEMBER_NAME = "key_member_name"
        private const val KEY_MEMBER_EMAIL = "key_member_email"
        private const val KEY_MEMBER_CODE = "key_member_code"
        private const val KEY_MEMBER_COLOR = "key_member_color"

        private const val KEY_ACTIVE_SHIFT_ID = "key_active_shift_id"
        private const val KEY_ACTIVE_SHIFT_START = "key_active_shift_start"
        private const val KEY_ACTIVE_SHIFT_END = "key_active_shift_end"
        private const val KEY_IS_CASH_ACTIVE = "key_is_cash_active"
        private const val KEY_ACTIVE_SESSION_ID = "key_active_session_id"
        private const val KEY_OPENING_FLOAT_CENTS = "key_opening_float_cents"

        @Volatile
        private var instance: TeamPreferences? = null

        fun getInstance(context: Context): TeamPreferences {
            return instance ?: synchronized(this) {
                instance ?: TeamPreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    var memberName: String?
        get() = prefs.getString(KEY_MEMBER_NAME, null)
        set(value) = prefs.edit().putString(KEY_MEMBER_NAME, value).apply()

    var memberEmail: String?
        get() = prefs.getString(KEY_MEMBER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_MEMBER_EMAIL, value).apply()

    var memberCode: String?
        get() = prefs.getString(KEY_MEMBER_CODE, null)
        set(value) = prefs.edit().putString(KEY_MEMBER_CODE, value).apply()

    var memberColor: String
        get() = prefs.getString(KEY_MEMBER_COLOR, "#8B1E2D") ?: "#8B1E2D"
        set(value) = prefs.edit().putString(KEY_MEMBER_COLOR, value).apply()

    var activeShiftId: Int?
        get() {
            val id = prefs.getInt(KEY_ACTIVE_SHIFT_ID, -1)
            return if (id == -1) null else id
        }
        set(value) = prefs.edit().putInt(KEY_ACTIVE_SHIFT_ID, value ?: -1).apply()

    var activeShiftStartTime: String?
        get() = prefs.getString(KEY_ACTIVE_SHIFT_START, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_SHIFT_START, value).apply()

    var activeShiftEndTime: String?
        get() = prefs.getString(KEY_ACTIVE_SHIFT_END, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_SHIFT_END, value).apply()

    var isCashActive: Boolean
        get() = prefs.getBoolean(KEY_IS_CASH_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_CASH_ACTIVE, value).apply()

    var activeSessionId: Long?
        get() {
            val id = prefs.getLong(KEY_ACTIVE_SESSION_ID, -1L)
            return if (id == -1L) null else id
        }
        set(value) = prefs.edit().putLong(KEY_ACTIVE_SESSION_ID, value ?: -1L).apply()

    var openingFloatCents: Int
        get() = prefs.getInt(KEY_OPENING_FLOAT_CENTS, 5000)
        set(value) = prefs.edit().putInt(KEY_OPENING_FLOAT_CENTS, value).apply()

    fun clearShift() {
        prefs.edit()
            .remove(KEY_ACTIVE_SHIFT_ID)
            .remove(KEY_ACTIVE_SHIFT_START)
            .remove(KEY_ACTIVE_SHIFT_END)
            .remove(KEY_IS_CASH_ACTIVE)
            .remove(KEY_ACTIVE_SESSION_ID)
            .apply()
    }
}
