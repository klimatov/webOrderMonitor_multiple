package utils

import cache.InMemoryCache
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Logging {
    private val tag = this::class.java.simpleName

    companion object {
        const val RESET = "\u001B[0m"
        const val BLACK = "\u001B[30m"
        const val RED = "\u001B[31m"
        const val GREEN = "\u001B[32m"
        const val YELLOW = "\u001B[33m"
        const val BLUE = "\u001B[34m"
        const val PURPLE = "\u001B[35m"
        const val CYAN = "\u001B[36m"
        const val WHITE = "\u001B[37m"

        fun i(tag: String, message: String) {
            toConsole(type = "I", tag = tag, message = message)
        }

        fun e(tag: String, message: String) {
            toConsole(type = "E", tag = tag, message = message)
            InMemoryCache.addError(dateTime = timeStamp(), sourceTag = tag, errorText = message)
        }

        fun d(tag: String, message: String) {
            toConsole(type = "D", tag = tag, message = message)
        }

        private fun toConsole(type: String, tag: String, message: String) {
            println(
                "${
                    when (type) {
                        "E" -> RED
                        "D" -> YELLOW
                        else -> RESET
                    }
                }${timeStamp()} $type [$tag] $message$RESET"
            )
        }

        private fun timeStamp(): String {
            return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SS"))
        }
    }
}