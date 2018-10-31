import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

object Log {
    private const val DIR_NAME = "log"
    private const val LOG_NAME = "$DIR_NAME/log.log"

    private val log = Logger.getLogger(this::class.simpleName)

    init {
        val dir = File(DIR_NAME)
        if (!dir.exists())
            dir.mkdir()

        val file = File(LOG_NAME)
        if (!file.exists())
            file.createNewFile()

        val fh = FileHandler(LOG_NAME)
        log.addHandler(fh)
        val formatter = SimpleFormatter()
        fh.formatter = formatter
    }

    fun logInfo(message: String) {
        log.info(message)
    }

    fun logError(message: String) {
        log.severe(message)
    }
}