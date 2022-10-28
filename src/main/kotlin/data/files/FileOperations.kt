package data.files

import java.io.File
import utils.Logging

class FileOperations {
    private val tag = this::class.java.simpleName
    fun write(fileName: String, data: String) {
        val writer = File(fileName).bufferedWriter()
        try {
            writer.write(data)
            Logging.i(tag,"Write to $fileName")
        } finally {
            writer.close()
        }
    }

    fun read(fileName: String): String {
        val file = File(fileName)
        var text = ""
        if (isFileExist(file)) {
            val reader = file.bufferedReader()
            try {
                text = reader.readText()
                Logging.i(tag, "Read from $fileName")
            } finally {
                reader.close()
            }
        } else Logging.e(tag, "File $fileName is missing!")
        return text
    }

    private fun isFileExist(file: File): Boolean {
        return file.isFile
    }
}