package ss.proximityservice.data

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class SharedPreferencesAppStorage(context: Context, name: String? = null) : AppStorage {
    private val preferences: SharedPreferences = if (name == null) {
        PreferenceManager.getDefaultSharedPreferences(context)
    } else {
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    override fun getString(key: String, defValue: String?): String? {
        return preferences.getString(key, defValue)
    }

    override fun getStringSet(key: String, defValues: Set<String>): Set<String> {
        return preferences.getStringSet(key, defValues)
    }

    override fun getInt(key: String, defValue: Int): Int {
        return preferences.getInt(key, defValue)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return preferences.getLong(key, defValue)
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return preferences.getFloat(key, defValue)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return preferences.getBoolean(key, defValue)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> put(key: String, value: T) {
        when (value) {
            is String -> preferences.edit().putString(key, value).apply()
            is Set<*> -> {
                if (value.all { it is String }) {
                    preferences.edit().putStringSet(key, value as Set<String>).apply()
                } else {
                    throw IllegalArgumentException("The supplied Set must contain Strings.")
                }
            }
            is Int -> preferences.edit().putInt(key, value).apply()
            is Long -> preferences.edit().putLong(key, value).apply()
            is Float -> preferences.edit().putFloat(key, value).apply()
            is Boolean -> preferences.edit().putBoolean(key, value).apply()
            else -> throw IllegalArgumentException("The supplied value type is not supported.")
        }
    }
}
