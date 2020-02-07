package io.keinix.peertopeerblesample.util.struct

import java.util.*

/**
 * Execute operations in series. If an operation B is executed while operation A
 * is in progress. Operations B will not be executed until operation A is marked
 * as complete.
 */
class OperationQueue {

    private var currentOperation: (() -> Unit)? = null
    private val queue = ArrayDeque<() -> Unit>()

    @Synchronized
    fun execute(operation: () -> Unit) {
        queue.add(operation)
        if (currentOperation == null) executeNext()
    }

    @Synchronized
    fun operationComplete() {
        currentOperation = null
        if (queue.isNotEmpty()) executeNext()
    }

    @Synchronized
    fun clear() {
        currentOperation = null
        queue.clear()
    }

    private fun executeNext() {
        currentOperation = queue.poll()
        currentOperation?.invoke()
    }
}