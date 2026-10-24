package com.keyboardyou.data

import android.content.Context
import com.keyboardyou.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Сохраняет последнее падение, чтобы его можно было скопировать из приложения. */
object CrashLog {
    private fun file(context: Context) = File(context.filesDir, "last_crash.txt")

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
                val time = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.ROOT).format(Date())
                file(app).writeText(
                    "Keyboard You ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                        "Android ${android.os.Build.VERSION.RELEASE} / ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                        "$time, поток ${thread.name}\n\n$trace"
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    @Volatile
    private var watchdog: Thread? = null

    /** Сторож зависаний: если главный поток молчит > 3 с, записываем, где он застрял. */
    fun startWatchdog(context: Context) {
        if (watchdog != null) return
        val app = context.applicationContext
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        watchdog = Thread {
            var reported = false
            while (!Thread.currentThread().isInterrupted) {
                val tick = java.util.concurrent.atomic.AtomicBoolean(false)
                val ping = Runnable { tick.set(true) }
                main.post(ping)
                try {
                    Thread.sleep(3000)
                } catch (_: InterruptedException) {
                    return@Thread
                }
                if (!tick.get() && !reported) {
                    reported = true
                    val stack = android.os.Looper.getMainLooper().thread.stackTrace.joinToString("\n") { "    at $it" }
                    runCatching {
                        file(app).writeText(
                            "Keyboard You ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                                "Android ${android.os.Build.VERSION.RELEASE} / ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                                "ЗАВИСАНИЕ главного потока > 3 с\n\n$stack"
                        )
                    }
                } else if (tick.get()) {
                    reported = false
                }
                main.removeCallbacks(ping)
            }
        }.apply {
            name = "kb-watchdog"
            isDaemon = true
            start()
        }
    }

    fun stopWatchdog() {
        watchdog?.interrupt()
        watchdog = null
    }

    fun read(context: Context): String? = file(context).takeIf { it.exists() }?.readText()

    fun clear(context: Context) {
        file(context).delete()
    }
}
