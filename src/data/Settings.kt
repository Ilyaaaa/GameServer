package data

import Calendar
import Log
import Server
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*

class Settings(val server: Server) {
    private val serverProps = Properties()
    private val worldProps = Properties()

    init {
        existsCheck()

        val serverConfReader = FileReader(CONF_NAME)
        serverProps.load(serverConfReader)
        serverConfReader.close()

        val worldConfReader = FileReader(WORLD_CONF_NAME)
        worldProps.load(worldConfReader)
        worldConfReader.close()

        port = serverProps.getProperty("port").toInt()
    }

    fun setPort(port: Int){
        Companion.port = port
        serverProps.setProperty("port", port.toString())
        save()
    }

    private fun setServerDefProps(){
        serverProps.setProperty("port", port.toString())
    }

    private fun setTimeDefProps(){
        worldProps.setProperty("minute", "0")
        worldProps.setProperty("hour", "0")
        worldProps.setProperty("day", "1")
        worldProps.setProperty("month", "1")
        worldProps.setProperty("year", "0")
    }

    private fun setTimeProps(calendar: Calendar){
        worldProps.setProperty("minute", calendar.minute.toString())
        worldProps.setProperty("hour", calendar.hour.toString())
        worldProps.setProperty("day", calendar.day.toString())
        worldProps.setProperty("month", calendar.month.toString())
        worldProps.setProperty("year", calendar.year.toString())
    }

    fun initTimeProps(calendar: Calendar) {
        calendar.minute = worldProps.getProperty("minute").toShort()
        calendar.hour = worldProps.getProperty("hour").toShort()
        calendar.day = worldProps.getProperty("day").toShort()
        calendar.month = worldProps.getProperty("month").toShort()
        calendar.year = worldProps.getProperty("year").toShort()
    }

    fun saveWorldProps(calendar: Calendar){
        setTimeProps(calendar)

        save()
    }

    private fun save() {
        try {
            val serverConfWriter = FileWriter(CONF_NAME)
            serverProps.store(serverConfWriter, null)
            serverConfWriter.close()

            val worldConfWriter = FileWriter(WORLD_CONF_NAME)
            worldProps.store(worldConfWriter, null)
            worldConfWriter.close()

            Log.logInfo("Settings saved")
        } catch (ex: IOException) {
            Log.logError(ex.toString())
        }
    }

    private fun existsCheck() {
        var save = false
        val dir = File(DIR_NAME)
        if (!dir.exists())
            dir.mkdir()

        val serverConf = File(CONF_NAME)
        if (!serverConf.exists()) {
            serverConf.createNewFile()

            setServerDefProps()
            save = true
        }

        val worldConf = File(WORLD_CONF_NAME)
        if (!worldConf.exists()) {
            worldConf.createNewFile()

            setTimeDefProps()
            save = true
        }

        if (save) save()
    }

    companion object {
        private const val DIR_NAME = "settings"
        private const val CONF_NAME = "$DIR_NAME/server.ini"
        private const val WORLD_CONF_NAME = "$DIR_NAME/world.ini"

        var port: Int = 4000
    }
}
