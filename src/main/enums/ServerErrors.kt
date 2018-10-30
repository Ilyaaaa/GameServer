package main.enums

enum class ServerErrors(val errorMessage: String) {
    INV_COMMAND("Invalid command"),
    INV_SYNTAX("Invalid command syntax"),
    INV_ARGS("Invalid arguments"),
    NOT_RUNNING_ERROR("Server not running"),
    ALREADY_RUNNING_ERROR("Server already running"),
    MAP_LOAD_ERROR("Map not exist"),
    MAP_OBJECTS_LOAD_ERROR("Error while loading map objects")
}