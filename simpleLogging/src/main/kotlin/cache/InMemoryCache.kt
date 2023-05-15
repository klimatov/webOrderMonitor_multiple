package cache

import java.util.LinkedList

data class Errors(
    val dateTime: String,
    val sourceTag: String,
    val errorText: String,
)

object InMemoryCache {
    private var LastErrors = LinkedList<Errors>()

    fun addError(dateTime: String, sourceTag: String, errorText: String) {
        LastErrors.addFirst(Errors(dateTime = dateTime, sourceTag = sourceTag, errorText = errorText))
        if (LastErrors.size > 10) LastErrors.removeLast()
    }

    fun getErrors(): String {
        val result = LastErrors.joinToString(separator = "") {
            "${it.dateTime} [${it.sourceTag}] ${it.errorText}\n"
        }
        return result
    }

}