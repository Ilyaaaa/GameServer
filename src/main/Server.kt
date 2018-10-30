package main

import data.Map
import data.Settings
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

class Server : Runnable {
    val map = Map(this)
    private val locker = ReentrantLock()
    private lateinit var serverSocket: ServerSocket
    lateinit var calendar: Calendar
    val clients = CopyOnWriteArrayList<Session>()
    var isRunning = false

    override fun run() {
        try {
            serverSocket = ServerSocket(Settings.port)
            Log.logInfo("Server started. Port: ${Settings.port}")

            val executorService = Executors.newCachedThreadPool()
            calendar = Calendar(this)
            isRunning = true

            while (!serverSocket.isClosed) {
                try {
                    println("Clients: " + clients.size)
                    val socket = serverSocket.accept()
                    Log.logInfo("New client connected ${socket.inetAddress}")
                    val session = Session(this, socket)
                    executorService.submit(session)
                } catch (ex: IOException) {
                    if (!serverSocket.isClosed) {
                        try {
                            serverSocket.close()
                        } catch (ex: IOException) {
                            Log.logError(ex.toString())
                        }
                    }
                }
            }
        } catch (ex: IOException) {
            Log.logError("Can't connect to " + Settings.port + " port\n$ex")
        }
    }

    fun stop() {
        calendar.stop()
        for (client in clients)
            client.stop()

        try {
            serverSocket.close()

            isRunning = false
            Log.logInfo("Server stopped")
        } catch (ex: IOException) {
            Log.logError(ex.toString())
        }
    }


}
