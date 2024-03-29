package domain.orderProcessing

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

open class DateTimeProcess {

    private fun dateFormat(docDate: String): LocalDateTime {
        val docDateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
        return LocalDateTime.parse(docDate, docDateFormat)
    }

    fun replaceDateTime(docDate: String): String {
        return dateFormat(docDate).format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy"))
    }

    inner class DateDiff(startDate: LocalDateTime, endDate: LocalDateTime = LocalDateTime.now()) {
        private val period = startDate.until(endDate, ChronoUnit.MINUTES)
        val days = period / 1440
        val hours = period % 1440 / 60
        val minutes = period % 1440 % 60
    }
}