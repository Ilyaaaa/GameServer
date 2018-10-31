import java.util.*

class Calendar (val server: Server) {
    var minute: Short = 0
    var hour: Short = 0
    var day: Short = 1
    var month: Short = 1
    var year: Short = 0
    private val timer = Timer()

    init {
        settings.initTimeProps(this)

        timer.schedule(object: TimerTask(){
            override fun run() {
                minute++

                if (minute >= 60){
                    minute = 0
                    hour++

                    if (hour >= 24){
                        hour = 0
                        day++
                        settings.saveWorldProps(this@Calendar)

                        if (day > 30) {
                            day = 1
                            month++

                            if (month > 12) {
                                month = 1
                                year++
                            }
                        }
                    }
                }

                if (minute%15 == 0)
                    server.clients.forEach {
                        it.sendTime()
                    }
            }
        }, 0, PERIOD)

        Log.logInfo("Calendar service started")
    }

    fun stop() {
        timer.cancel()
        timer.purge()
        Log.logInfo("Calendar service stopped")
    }

    companion object {
        const val PERIOD = 1000L
    }
}