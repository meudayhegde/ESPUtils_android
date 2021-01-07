package com.irware

import android.app.Activity
import java.util.*
import kotlin.collections.ArrayList


/**
 *
 * @author hegdeuday
 */
class ThreadHandler(val activity: Activity, vararg threadNames: String, threadCunt: Int = CPU_CORE_COUNT - 1) {
    private val threadList = ArrayList<InfiniteThread>()
    private var maxThreadCount = MAX_THREAD_COUNT

    init {
        threadNames.forEach {
            val thread = InfiniteThread()
            thread.name = it
            threadList.add(thread)
        }
        if(threadCunt > threadNames.size) for(i in threadNames.size until threadCunt) add(InfiniteThread())
    }

    constructor(activity: Activity): this(activity, PRIMARY, ESP_MESSAGE)

    private fun add(thread: InfiniteThread, position: Int, name: String?) {
        thread.name = name?:thread.name
        threadList.add(position, thread)
    }

    private fun add(thread: InfiniteThread) {
        threadList.add(thread)
    }

    fun getThreadByPosition(position: Int): InfiniteThread {
        return threadList[position]
    }

    fun getThreadByName(name: String): InfiniteThread? {
        threadList.forEach {
            if(it.name == name) return it
        }
        return null
    }

    fun runOnThread(pos: Int, task: Runnable) {
        getThreadByPosition(pos).enqueueTask(task)
    }

    fun runOnThread(pos: Int, task: (() -> Unit)) {
        getThreadByPosition(pos).enqueueTask(task)
    }

    fun runOnThread(name: String, task: Runnable) {
        getThreadByName(name)?.enqueueTask(task)
    }

    fun runOnThread(name: String, task: (() -> Unit)) {
        getThreadByName(name)?.enqueueTask(task)
    }


    fun runOnFreeThread(task: Runnable): Int {
        threadList.forEach {
            if(it.isFree){
                it.enqueueTask(task)
                return threadList.indexOf(it)
            }
        }
        return if(threadList.size < maxThreadCount) {
            add(InfiniteThread(task))
            -1 % threadList.size
        }else {
            val thread = threadList.sortedBy { it.getTaskCount() }[0]
            thread.enqueueTask(task)
            threadList.indexOf(thread)
        }
    }

    fun runOnFreeThread(task: (() -> Unit)): Int {
        threadList.forEach {
            if(it.isFree){
                it.enqueueTask(task)
                return threadList.indexOf(it)
            }
        }
        return if(threadList.size < maxThreadCount) {
            threadList.add(InfiniteThread(task))
            -1 % threadList.size
        }else {
            val thread = threadList.sortedBy { it.getTaskCount() }[0]
            thread.enqueueTask(task)
            threadList.indexOf(thread)
        }
    }

    fun runOnPrimaryThread(run: Runnable) {
        runOnThread(0, run)
    }

    fun runOnUIThread(task: (() -> Unit)){
        activity.runOnUiThread(task)
    }

    fun getThreadCount() : Int{
        return threadList.size
    }

    fun setMaxThreadCount(count: Int){
        if(count < maxThreadCount)
            for(i in threadList.size -1 until count) {
                threadList[i].finish()
                threadList.removeAt(i)
            }
        maxThreadCount = count
    }

    fun runOnUiThread(task: Runnable){
        activity.runOnUiThread(task)
    }

    fun runOnUiThread(task: (() -> Unit)){
        activity.runOnUiThread(task)
    }

    fun finishAll() {
        threadList.forEach { it.finish() }
    }

    class InfiniteThread : Thread {
        private var flag = true
        private var isPaused = false
        var isFree = true
        private val taskQueue = ArrayList<(() -> Unit)>()

        constructor() : super() {
            start()
        }

        constructor(task: Runnable) : super() {
            start()
            enqueueTask{
                task.run()
            }
        }

        constructor(task: (() -> Unit)) : super() {
            start()
            enqueueTask(task)
        }

        override fun run() {
            while (flag) {
                if (taskQueue.size > 0) {
                    if(!isPaused){
                        isFree = false
                        taskQueue[0].invoke()
                        taskQueue.removeAt(0)
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
            taskQueue.add{
                task.run()
            }
        }

        fun getTaskCount(): Int{
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
        val MAX_THREAD_COUNT = ( CPU_CORE_COUNT- 1) * 2;

        const val PRIMARY = "PRIMARY"
        const val ESP_MESSAGE = "ESP"
    }
}

