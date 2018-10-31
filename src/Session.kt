import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import data.DBConnector

class Session(private val server: Server, private val socket: Socket) : Runnable {
    private var stop = false
    private val writer = PrintWriter(socket.getOutputStream(), true)
    private val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    var player: Player? = null
    private val visiblePlayers = ArrayList<Session>()

    override fun run() {
        try {
            DBConnector.connect()

            Log.logInfo("Session started ${socket.inetAddress}")

            while (!stop && socket.isConnected && !socket.isClosed && !socket.isOutputShutdown) {
                val request = reader.readLine()
//                println(request)

                if (request == null) {
                    stop()
                    break
                }

                lateinit var objectFromClient: JSONObject
                try {
                    objectFromClient = JSONObject(request)
                }catch (ex: JSONException) {
                    Log.logError("${socket.inetAddress}\nRequest:$request\nError: $ex")

//                    val html = """
//                        <html>
//                            <head>
//                                <meta http-equiv="refresh"content="1;URL=https://www.pornhub.com/gayporn"/>
//                            </head>
//                        </html>""".trimIndent()
//
//                    writer.println(
//                            """
//                                HTTP/1.1 200 OK
//                                Server: YourFatAss
//                                Content-Type: text/html
//                                Content-Length: ${html.length}
//                                Connection: close
//                            """.trimIndent() + "\n\n$html"
//                    )

                    stop()
                    break
                }

                val value = objectFromClient["id"].toString().toInt()
                when (value) {
                    //authorisation
                    MessagesId.AUTHORISE_ID.id -> {
                        val name = objectFromClient["name"].toString()

                        transaction {
                            val players = DBConnector.Players.select { DBConnector.Players.nickname eq name }

                            if (players.count() > 0) {
                                players.forEach {
                                    val x = it[DBConnector.Players.x]
                                    val y = it[DBConnector.Players.y]

                                    player = Player(
                                            it[DBConnector.Players.id],
                                            it[DBConnector.Players.nickname],
                                            it[DBConnector.Players.email],
                                            server.map.getBlockId(x, y),
                                            x,
                                            y,
                                            it[DBConnector.Players.position],
                                            it[DBConnector.Players.hp]
                                    )
                                }
                            } else {
                                val id = DBConnector.Players.insert {
                                    it[DBConnector.Players.nickname] = name
                                    it[DBConnector.Players.email] = "$name@email.com"
                                    it[DBConnector.Players.password] = "12345678"
                                }.generatedKey

                                DBConnector.Players.select { DBConnector.Players.id eq id!!.toLong() }.forEach {
                                    val x = it[DBConnector.Players.x]
                                    val y = it[DBConnector.Players.y]

                                    player = Player(
                                            it[DBConnector.Players.id],
                                            it[DBConnector.Players.nickname],
                                            it[DBConnector.Players.email],
                                            server.map.getBlockId(x, y),
                                            x,
                                            y,
                                            it[DBConnector.Players.position],
                                            it[DBConnector.Players.hp]
                                    )
                                }
                            }

                            sendPlayerData(MessagesId.AUTHORISE_ID.id, player!!)

                            for (client in server.clients) {
                                if (client.player!!.id != player!!.id && isBlockVisible(client.player!!.blockId)) {
                                    sendPlayerData(MessagesId.PLAYER_DATA_ID.id, client.player!!)
                                    client.sendPlayerData(MessagesId.PLAYER_DATA_ID.id, player!!)
                                }
                            }

                            sendTime()

                            server.clients.add(this@Session)
                        }
                    }

                    MessagesId.PLAYER_DATA_ID.id -> {
                        player!!.position = objectFromClient["position"].toString()
                        player!!.x = objectFromClient["x"].toString().toFloat()
                        player!!.y = objectFromClient["y"].toString().toFloat()

                        for (client in server.clients) {
                            if (client.player!!.id != player!!.id) {
                                if (isBlockVisible(client.player!!.blockId)) {
                                    client.sendPlayerData(MessagesId.PLAYER_DATA_ID.id, player!!)

                                    if (!visiblePlayers.contains(client)) {
                                        visiblePlayers.add(client)
                                        client.visiblePlayers.add(this)
                                        sendPlayerData(MessagesId.PLAYER_DATA_ID.id, client.player!!)
                                    }
                                } else {
                                    if (visiblePlayers.contains(client)) {
                                        sendPlayerId(MessagesId.PLAYER_INVISIBLE_ID.id, client.player!!.id)
                                        client.sendPlayerId(MessagesId.PLAYER_INVISIBLE_ID.id, player!!.id)
                                        visiblePlayers.remove(client)
                                        client.visiblePlayers.remove(this)
                                    }
                                }
                            }
                        }
                    }

                    MessagesId.GET_MAP_ID.id -> {
                        val side = objectFromClient["side"].toString()
                        val x = objectFromClient["x"].toString().toFloat()
                        val y = objectFromClient["y"].toString().toFloat()

                        player!!.blockId = server.map.getBlockId(x, y)

                        val lower = server.map.getBlocks(x, y, side, 0)
                        val average = server.map.getBlocks(x, y, side, 1)
                        val top = server.map.getBlocks(x, y, side, 2)

                        if (lower != null) sendMapPart(lower.first, "lower")
                        if (average != null) {
                            sendMapPart(average.first, "average")
                            sendMapObjects(average.second)
                        }
                        if (top != null) sendMapPart(top.first, "top")
                    }

                    MessagesId.PLAYER_DISCONNECT_ID.id -> {
                        stop()
                    }

                    MessagesId.CHAT_ID.id -> {
                        val message = objectFromClient["message"].toString()

                        visiblePlayers.forEach {
                            it.sendChatMessage(player!!.id, message)
                        }
                    }
                }
            }
        } catch (ex: IOException) {
            Log.logError(ex.toString())
            stop()
        }
    }

    private fun isBlockVisible (blockId: Int): Boolean {
        return (
                blockId == server.map.getLeftUpBlockId(player!!.x, player!!.y) ||
                blockId == server.map.getUpBlockId(player!!.x, player!!.y) ||
                blockId == server.map.getRightUpBlockId(player!!.x, player!!.y) ||
                blockId == server.map.getRightBlockId(player!!.x, player!!.y) ||
                blockId == server.map.getBlockId(player!!.x, player!!.y) ||
                blockId == server.map.getLeftBlockId(player!!.x, player!!.y) ||
                blockId == server.map.getRightDownBlockId(player!!.x, player!!.y) ||
                blockId == server.map.getDownBlockId(player!!.x, player!!.y) ||
                blockId == server.map.getLeftDownBlockId(player!!.x, player!!.y)
                )
    }

    fun sendPlayerData(messageId: Int, player: Player) {
        writer.println(
                JSONObject()
                        .put("id", messageId)
                        .put("playerId", player.id)
                        .put("name", player.nickname)
                        .put("position", player.position)
                        .put("x", player.x)
                        .put("y", player.y)
                        .toString()
        )
    }

    fun sendPlayerId(id: Int, playerId: Long) {
        writer.println(
                JSONObject()
                        .put("id", id)
                        .put("playerId", playerId)
                        .toString()
        )
    }

    fun sendChatMessage(playerId: Long, message: String) {
        writer.println(
                JSONObject()
                        .put("id", MessagesId.CHAT_ID.id)
                        .put("playerId", playerId)
                        .put("message", message)
                        .toString()
        )
    }

    fun sendTime() {
        writer.println(
                JSONObject()
                        .put("id", MessagesId.TIME_ID.id)
                        .put("year", server.calendar.year)
                        .put("month", server.calendar.month)
                        .put("day", server.calendar.day)
                        .put("hour", server.calendar.hour)
                        .put("minute", Math.floor(server.calendar.minute.toDouble() / 15) * 15)
                        .put("period", Calendar.PERIOD)
                        .toString()
        )
    }

    private fun sendMapPart(blocks: JSONArray , type: String) {
        if (blocks.length() > 0) {
            writer.println(
                    JSONObject()
                            .put("id", MessagesId.GET_MAP_ID.id)
                            .put("type", type)
                            .put("blocks", blocks)
                            .toString()
            )
        }
    }

    private fun sendMapObjects(objects: JSONArray) {
        if (objects.length() > 0) {
            writer.println(
                    JSONObject()
                            .put("id", MessagesId.MAP_OBJECT_ID.id)
                            .put("objects", objects)
            )
        }
    }

    fun stop() {
        stop = true

        try {
            server.clients.remove(this)

            writer.close()
            reader.close()
            socket.close()

            for (client in server.clients) {
                client.sendPlayerId(MessagesId.PLAYER_DISCONNECT_ID.id, player!!.id)
            }

            Log.logInfo("Client disconnected ${socket.inetAddress}")
        } catch (ex: IOException) {
            Log.logError(ex.toString())
            Log.logError("Can't disconnect client\n$ex")
        }

        println("Clients: " + server.clients.size)
    }

    companion object {
        data class Player(
                val id: Long,
                var nickname: String,
                var email: String,
                var blockId: Int,
                var x:Float,
                var y: Float,
                var position: String,
                var hp: Int
        )

        enum class MessagesId(val id: Int) {
            AUTHORISE_ID(0),
            PLAYER_DATA_ID(1),
            PLAYER_INVISIBLE_ID(2),
            GET_MAP_ID(3),
            PLAYER_DISCONNECT_ID(4),
            CHAT_ID(5),
            TIME_ID(6),
            MAP_OBJECT_ID(7)
        }
    }
}
