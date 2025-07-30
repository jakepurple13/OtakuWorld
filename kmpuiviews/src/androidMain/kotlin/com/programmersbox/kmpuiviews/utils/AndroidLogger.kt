package com.programmersbox.kmpuiviews.utils

import android.util.Log

class AndroidLogger : Logger {
    override fun printLog(message: Any?) {
        println(message)
    }

    override fun d(tag: String, message: Any?) {
        Log.d(tag, message.toString())
    }

    override fun i(tag: String, message: Any?) {
        Log.i(tag, message.toString())
    }

    override fun w(tag: String, message: Any?) {
        Log.w(tag, message.toString())
    }

    override fun e(tag: String, message: Any?) {
        Log.e(tag, message.toString())
    }

    override fun v(tag: String, message: Any?) {
        Log.v(tag, message.toString())
    }

    override fun wtf(tag: String, message: Any?) {
        Log.wtf(tag, message.toString())
    }

    override fun log(priority: Int, tag: String, message: Any?) {
        Log.println(priority, tag, message.toString())
    }

    override fun log(priority: Int, tag: String, message: String?, tr: Throwable?) {
        Log.println(priority, tag, message.toString())
    }

    override fun d(tag: String, message: Any?, tr: Throwable?) {
        Log.d(tag, message.toString(), tr)
    }

    override fun i(tag: String, message: Any?, tr: Throwable?) {
        Log.i(tag, message.toString(), tr)
    }

    override fun w(tag: String, message: Any?, tr: Throwable?) {
        Log.w(tag, message.toString(), tr)
    }

    override fun e(tag: String, message: Any?, tr: Throwable?) {
        Log.e(tag, message.toString(), tr)
    }

    override fun v(tag: String, message: Any?, tr: Throwable?) {
        Log.v(tag, message.toString(), tr)
    }

    override fun wtf(tag: String, message: Any?, tr: Throwable?) {
        Log.wtf(tag, message.toString(), tr)
    }
}