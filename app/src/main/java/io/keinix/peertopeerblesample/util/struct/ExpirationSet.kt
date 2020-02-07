package io.keinix.peertopeerblesample.util.struct

import java.util.*

/**
 * Allows only unique items to be added to the collection. Items are automatically
 * removed after expirationMillis
 *
 * @param expirationMillis time in milliseconds a value will be kept in the set
 * @param maxSize the maximum capacity of the set
 * If the max size is exceeded the oldest added item will be dropped
 */
class ExpirationSet<E: Any>(private val expirationMillis: Long,
                            private val maxSize: Int) {

    private val queue = ArrayDeque<Element>(maxSize)

    init {
        check(maxSize > 0) { "maxSize must be > 0" }
    }

    /**
     * @return True - if item was added False - item already exists as was not added
     */
    @Synchronized
    fun add(e: E): Boolean = when {
        queue.size < maxSize -> addUniqueElement(e)
        removeExpiredElements() -> addUniqueElement(e)
        else -> {
            queue.poll()
            addUniqueElement(e)
        }
    }

    @Synchronized
    fun remove(e: E): Boolean = queue
        .firstOrNull { it.value == e }
        ?.let { queue.remove(it) }
        ?: false

    @Synchronized
    fun clear() = queue.clear()

    private fun addUniqueElement(e: E): Boolean {
        val elementFromList = queue.firstOrNull { it.value == e }
        return when {
            elementFromList == null -> {
                queue.add(Element(e))
                true
            }
            elementFromList.isExpired -> {
                queue.remove(elementFromList)
                queue.add(Element(e))
                true
            }
            else -> false
        }
    }

    private fun removeExpiredElements(): Boolean = queue
        .filter { it.isExpired }
        .let { queue.removeAll(it) }

    private inner class Element(val value: E)  {
        private val createAt = System.currentTimeMillis()
        val isExpired get() = System.currentTimeMillis() - createAt >= expirationMillis
    }
}