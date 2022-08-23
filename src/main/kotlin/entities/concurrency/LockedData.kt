package entities.concurrency

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LockedData<V>(private var data: V) {
    private val mutex = Mutex(false)

    suspend fun adjustData(action: suspend (V) -> V) {
        mutex.withLock {
            data = action(data)
        }
    }

    fun view() : V {
        return data
    }
}