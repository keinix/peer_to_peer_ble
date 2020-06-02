package io.keinix.peertopeerblesample.util.struct

import android.os.CountDownTimer
import android.os.Handler
import java.util.*

/**
 * Execute operations in series. If an operation B is executed while operation A
 * is in progress. Operations B will not be executed until operation A is marked
 * as complete.
 */
class OperationQueue {

    private var currentOperation: (() -> Unit)? = null
    private val queue = ArrayDeque<() -> Unit>()
    private val handler = Handler()

    @Synchronized
    fun execute(operation: () -> Unit) {
        queue.add(operation)
        if (currentOperation == null) executeNext()
    }

    @Synchronized
    fun operationComplete() {
        timeout.cancel()
        currentOperation = null
        if (queue.isNotEmpty()) executeNext()
    }

    @Synchronized
    fun clear() {
        timeout.cancel()
        currentOperation = null
        queue.clear()
    }

    private fun executeNext() {
        currentOperation = queue.poll()
        handler.post {
            timeout.start()
            currentOperation?.invoke()
        }
    }

    private val timeout = object : CountDownTimer(10000, 1000) {
        override fun onFinish() = operationComplete()

        override fun onTick(millisUntilFinished: Long) {
        }
    }
}