package com.github.meudayhegde

import java.util.Queue
import java.util.LinkedList
import kotlin.collections.ArrayList

class ThreadHandler {
    private val threadList = ArrayList<InfiniteThread>()
    private var maxThreadCount = MAX_THREAD_COUNT

    private constructor(vararg threadNames: String, threadCunt: Int = CPU_CORE_COUNT - 1) {
        threadNames.forEach {
            val thread = InfiniteThread(true)
            thread.name = it
            threadList.add(thread)
        }
        if (threadCunt > threadNames.size) for (i in threadNames.size until threadCunt) add(
            InfiniteThread()
        )
    }

    private constructor() : this(PRIMARY, ESP_MESSAGE)

    private fun add(thread: InfiniteThread, position: Int, name: String?) {
        thread.name = name ?: thread.name
        threadList.add(position, thread)
    }

    private fun add(thread: InfiniteThread) {
        threadList.add(thread)
    }

    private fun getThreadByPosition(position: Int): InfiniteThread {
        return threadList[position]
    }

    private fun getThreadByName(name: String): InfiniteThread? {
        threadList.forEach {
            if (it.name == name) return it
        }
        return null
    }

    private fun runOnThread(pos: Int, task: Runnable) {
        getThreadByPosition(pos).enqueueTask(task)
    }

    private fun runOnThread(pos: Int, task: (() -> Unit)) {
        getThreadByPosition(pos).enqueueTask(task)
    }

    private fun runOnThread(name: String, task: Runnable) {
        getThreadByName(name)?.enqueueTask(task)
    }

    private fun runOnThread(name: String, task: (() -> Unit)) {
        getThreadByName(name)?.enqueueTask(task)
    }


    private fun runOnFreeThread(task: Runnable, vararg except: String): Int {
        threadList.forEach {
            if (it.isFree and !(except.contains(it.name))) {
                it.enqueueTask(task)
                return threadList.indexOf(it)
            }
        }
        return if (threadList.size < maxThreadCount) {
            add(InfiniteThread(task))
            -1 % threadList.size
        } else {
            val thread = threadList.sortedBy { it.getTaskCount() }[0]
            thread.enqueueTask(task)
            threadList.indexOf(thread)
        }
    }

    private fun runOnFreeThread(task: (() -> Unit), vararg except: String): Int {
        threadList.forEach {
            if (it.isFree and !(except.contains(it.name))) {
                it.enqueueTask(task)
                return threadList.indexOf(it)
            }
        }
        return if (threadList.size < maxThreadCount) {
            threadList.add(InfiniteThread(task))
            -1 % threadList.size
        } else {
            val thread = threadList.sortedBy { it.getTaskCount() }[0]
            thread.enqueueTask(task)
            threadList.indexOf(thread)
        }
    }

    private fun runOnPrimaryThread(run: Runnable) {
        runOnThread(0, run)
    }

    private fun getThreadCount(): Int {
        return threadList.size
    }

    private fun setMaxThreadCount(count: Int) {
        if (count < maxThreadCount)
            for (i in threadList.size - 1 until count) {
                threadList[i].finish()
                threadList.removeAt(i)
            }
        maxThreadCount = count
    }

    private fun finishAll() {
        threadList.forEach { it.finish() }
    }

    class InfiniteThread : Thread {
        private var flag = true
        private var isPaused = false
        var isFree = true
        private val taskQueue: Queue<(() -> Unit)> = LinkedList()

        constructor() : super() {
            start()
        }

        constructor(task: Runnable) : super() {
            start()
            enqueueTask {
                task.run()
            }
        }

        constructor(task: (() -> Unit)) : super() {
            start()
            enqueueTask(task)
        }

        constructor(daemon: Boolean) : super() {
            isDaemon = daemon
            start()
        }

        override fun run() {
            while (flag) {
                if (taskQueue.size > 0) {
                    if (!isPaused) {
                        isFree = false
                        taskQueue.poll()?.invoke()
                    }
                } else {
                    isFree = true
                    sleep(10)
                }
            }
        }

        @Synchronized
        fun enqueueTask(task: (() -> Unit)) {
            taskQueue.add(task)
        }

        @Synchronized
        fun enqueueTask(task: Runnable) {
            taskQueue.add {
                task.run()
            }
        }

        fun getTaskCount(): Int {
            return taskQueue.size
        }

        fun finish() {
            flag = false
        }

        fun pauseThread() {
            isPaused = true
        }

        fun resumeThread() {
            isPaused = false
        }
    }

    companion object {
        private val CPU_CORE_COUNT = Runtime.getRuntime().availableProcessors()
        val MAX_THREAD_COUNT = (CPU_CORE_COUNT - 1) * 2

        const val PRIMARY = "PRIMARY"
        const val ESP_MESSAGE = "ESP_MESSAGE"

        private val instance = ThreadHandler()

        fun getThreadByPosition(position: Int): InfiniteThread {
            return instance.getThreadByPosition(position)
        }

        fun getThreadByName(name: String): InfiniteThread? {
            return instance.getThreadByName(name)
        }

        fun runOnThread(pos: Int, task: Runnable) {
            return instance.runOnThread(pos, task)
        }

        fun runOnThread(pos: Int, task: (() -> Unit)) {
            return instance.runOnThread(pos, task)
        }

        fun runOnThread(name: String, task: Runnable) {
            return instance.runOnThread(name, task)
        }

        fun runOnThread(name: String, task: (() -> Unit)) {
            return instance.runOnThread(name, task)
        }

        fun runOnFreeThread(vararg except: String = arrayOf(ESP_MESSAGE), task: Runnable): Int {
            return instance.runOnFreeThread(task, *except)
        }

        fun runOnFreeThread(vararg except: String = arrayOf(ESP_MESSAGE), task: (() -> Unit)): Int {
            return instance.runOnFreeThread(task, *except)
        }

        fun runOnPrimaryThread(run: Runnable) {
            return instance.runOnPrimaryThread(run)
        }

        fun getThreadCount(): Int {
            return instance.getThreadCount()
        }

        fun setMaxThreadCount(count: Int) {
            return instance.setMaxThreadCount(count)
        }

        fun finishAll() {
            return instance.finishAll()
        }
    }
}

