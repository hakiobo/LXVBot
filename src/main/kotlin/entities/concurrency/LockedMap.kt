package entities.concurrency

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LockedMap<T, V> {
    private val mutex = Mutex(false)
    private val data = HashMap<T, LockedData<V>>()

    suspend fun getOrDefault(key: T, default: suspend () -> V): LockedData<V>? {
        var curData: LockedData<V>?
        mutex.withLock {
            curData = data[key]
            if (curData == null) {
                val insides = default()
                if (insides != null) {
                    curData = LockedData(insides)
                    data[key] = curData!!
                }
            }
        }
        return curData
    }
}

