package ss.proximityservice.data

interface AppStorage {
    fun getString(key: String, defValue: String?): String?
    fun getStringSet(key: String, defValues: Set<String>): Set<String>
    fun getInt(key: String, defValue: Int): Int
    fun getLong(key: String, defValue: Long): Long
    fun getFloat(key: String, defValue: Float): Float
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun <T> put(key: String, value: T)
    fun remove(key: String)
    fun clear()
}
