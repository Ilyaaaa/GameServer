import data.Settings
import enums.ServerErrors
import java.util.*

private val server = Server()
val settings = Settings(server)

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)

    var text: String
    var command: String
    var commandArgs: String

    println("Input 'run' to run server, 'help' to open manual")
    while (true) {
        text = input.nextLine()
        val n = text.indexOf(" ")
        if (n == -1) {
            command = text
            commandArgs = ""
        } else {
            command = text.substring(0, n)
            commandArgs = text.substring(n)

            while (commandArgs.startsWith(" "))
                commandArgs = commandArgs.substring(1)
            while (commandArgs.endsWith(" "))
                commandArgs = commandArgs.substring(0, commandArgs.length - 1)
        }

        if (commandArgs == "") {
            when (command) {
                "run" -> {
                    if (!server.isRunning)
                        runServer()
                    else
                        println(ServerErrors.ALREADY_RUNNING_ERROR.errorMessage)
                }
                "stop" -> {
                    if (server.isRunning)
                        server.stop()
                    else
                        println(ServerErrors.NOT_RUNNING_ERROR.errorMessage)
                }

                "restart" -> {
                    if (server.isRunning) {
                        server.stop()
                        runServer()
                    } else
                        println(ServerErrors.NOT_RUNNING_ERROR.errorMessage)
                }
                "close" -> System.exit(0)

                "help" -> println(getManual())

                else -> println(ServerErrors.INV_SYNTAX.errorMessage)
            }
        } else {
            when (command) {
//                "send" -> send(commandArgs)

                "set" -> set(commandArgs)

                else -> println(ServerErrors.INV_COMMAND.errorMessage)
            }
        }
    }
}

//private fun send(args: String) {
//    var args = args
//    if (server.isRunning) {
//        val n = args.indexOf(" ")
//        if (n != -1) {
//            val command = args.substring(0, n)
//            args = args.substring(n + 1)
//
//            when (command) {
//                "all" -> for (client in server.clients) {
//                    client.sendMessage(args)
//                }
//
//                else -> outErrorMessage(ServerErrors.INV_SYNTAX.errorMessage)
//            }
//        } else
//            outErrorMessage(ServerErrors.INV_SYNTAX.errorMessage)
//    } else
//        outErrorMessage(ServerErrors.NOT_RUNNING_ERROR.errorMessage)
//}

private fun set(args: String) {
    var args = args
    val n = args.indexOf(" ")
    if (n != -1) {
        val command = args.substring(0, n)
        args = args.substring(n + 1)

        when (command) {
            "port" -> try {
                settings.setPort(Integer.parseInt(args))
            } catch (ex: NumberFormatException) {
                println(ServerErrors.INV_ARGS.errorMessage)
            }

            else -> println(ServerErrors.INV_SYNTAX.errorMessage)
        }
    } else
        println(ServerErrors.INV_SYNTAX.errorMessage)
}

private fun runServer() {
    val thread = Thread(server)
    thread.start()
}

private fun getManual(): String {
    return """
    run - Run server
    stop - Stop server
    restart - Restart server
    send [to] [message] - Send message
    set [option] [value] <value2 ...>
    close - Exit
    """.trimIndent()
}