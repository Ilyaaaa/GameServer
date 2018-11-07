package data

import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONArray
import org.json.JSONObject
import Server
import enums.ServerErrors
import java.io.BufferedReader
import java.io.File
import kotlin.collections.ArrayList
import Log
import data.enums.MapObjectsType
import org.jetbrains.exposed.sql.selectAll

class Map(private val server: Server){
    private val map = ArrayList<ArrayList<Char>>()
    private val lowerMap = ArrayList<ArrayList<Char>>()
    private val upperMap = ArrayList<ArrayList<Char>>()
    private val mapFile = File("levelMap.xy")
    private val lowerMapFile = File("lowerLevelMap.xy")
    private val upperMapFile = File("upperLevelMap.xy")


    private val trees = ArrayList<MapObject>()
    private val bushes = ArrayList<MapObject>()
    private val bonfires = ArrayList<MapObject>()
    private val sticks = ArrayList<MapObject>()

    init {
        if (!mapFile.exists() || !lowerMapFile.exists() || !upperMapFile.exists()) {
            val command = if (System.getProperty("os.name").toLowerCase().contains("windows")) "MapGenerator.exe 128 128"
            else "mono MapGenerator.exe 128 128"

            val proc = Runtime.getRuntime().exec(command)
            proc.waitFor()
            proc.destroy()
        }

        loadMapFile(mapFile.bufferedReader(), map)
        loadMapFile(lowerMapFile.bufferedReader(), lowerMap)
        loadMapFile(upperMapFile.bufferedReader(), upperMap)

        if (getMapObjects()) Log.logInfo("Map objects loaded")
        else {
            server.stop()
            Log.logError(ServerErrors.MAP_OBJECTS_LOAD_ERROR.errorMessage)
        }
    }

    private fun loadMapFile(inputStream: BufferedReader, list: ArrayList<ArrayList<Char>>) {
        for (line in inputStream.lines())
            if (line != null) {
                var lineVar = line
                if (lineVar.length%2 != 0)
                    lineVar = lineVar.substring(1)

                val lineChars = ArrayList<Char>()
                for (char in lineVar)
                    lineChars.add(char)

                list.add(lineChars)
            }
    }

    private fun getMapObjects(): Boolean {
        var success = false

        DBConnector.connect()
        transaction {
            DBConnector.MapObjects.selectAll().forEach {
                val obj = MapObject(
                        it[DBConnector.MapObjects.id],
                        it[DBConnector.MapObjects.name],
                        it[DBConnector.MapObjects.x],
                        it[DBConnector.MapObjects.y],
                        it[DBConnector.MapObjects.type],
                        it[DBConnector.MapObjects.params]
                )

                when (obj.type) {
                    MapObjectsType.TREE.type -> { trees.add(obj) }

                    MapObjectsType.BUSH.type -> { bushes.add(obj) }

                    MapObjectsType.BONFIRE.type -> { bonfires.add(obj) }

                    MapObjectsType.STICK.type -> {sticks.add(obj)}
                }
            }

            success = true
        }

        return success
    }

    private fun getMapWidth(): Int{
        return map[0].size
    }

    private fun getMapHeight(): Int{
        return map.size
    }

    fun getBlockId(x: Float, y: Float): Int {
        var blockX = x
        var blockY = y

        if ((blockX >= getMapWidth()) || (blockX < 0)) blockX -= Math.floor((blockX / getMapWidth()).toDouble()).toFloat() * getMapWidth()
        if ((blockY >= getMapHeight()) || (blockY < 0)) blockY -= Math.floor((blockY / getMapHeight()).toDouble()).toFloat() * getMapHeight()

        return (blockX / blockSize).toInt() + (blockY / blockSize).toInt() * (getMapWidth()/blockSize)
    }

    fun getLeftUpBlockId(x: Float, y: Float): Int{
        return  getBlockId(x - blockSize, y - blockSize)
    }

    fun getUpBlockId(x: Float, y: Float): Int{
        return getBlockId(x, y - blockSize)
    }

    fun getRightUpBlockId(x: Float, y: Float): Int{
        return getBlockId(x + blockSize, y - blockSize)
    }

    fun getLeftBlockId(x: Float, y: Float): Int{
        return getBlockId(x - blockSize, y)
    }

    fun getRightBlockId(x: Float, y: Float): Int{
        return getBlockId(x + blockSize, y)
    }

    fun getLeftDownBlockId(x: Float, y: Float): Int{
        return getBlockId(x - blockSize, y + blockSize)
    }

    fun getDownBlockId(x: Float, y: Float): Int{
        return getBlockId(x, y + blockSize)
    }

    fun getRightDownBlockId(x: Float, y: Float): Int{
        return getBlockId(x + blockSize, y + blockSize)
    }

    @Synchronized fun getBlocks(x: Float, y: Float, side: String, level: Int): Pair<JSONArray, JSONArray>? {
        val width = getMapWidth()
        val blockIds = ArrayList<Int>()

        when (side) {
            "all" -> {
                blockIds.addAll(intArrayOf(
                        getLeftUpBlockId(x, y),
                        getUpBlockId(x, y),
                        getRightUpBlockId(x, y),
                        getLeftBlockId(x, y),
                        getBlockId(x, y),
                        getRightBlockId(x, y),
                        getLeftDownBlockId(x, y),
                        getDownBlockId(x, y),
                        getRightDownBlockId(x, y)
                ).toCollection(ArrayList()))
            }

            "R" -> {
                blockIds.addAll(intArrayOf(
                        getRightUpBlockId(x, y),
                        getRightBlockId(x, y),
                        getRightDownBlockId(x, y)
                ).toCollection(ArrayList()))
            }

            "L" -> {
                blockIds.addAll(intArrayOf(
                        getLeftUpBlockId(x, y),
                        getLeftBlockId(x, y),
                        getLeftDownBlockId(x, y)
                ).toCollection(ArrayList()))
            }

            "U" -> {
                blockIds.addAll(intArrayOf(
                        getLeftUpBlockId(x, y),
                        getUpBlockId(x, y),
                        getRightUpBlockId(x, y)
                ).toCollection(ArrayList()))
            }

            "D" -> {
                blockIds.addAll(intArrayOf(
                        getLeftDownBlockId(x, y),
                        getDownBlockId(x, y),
                        getRightDownBlockId(x, y)
                ).toCollection(ArrayList()))
            }

            "LU" -> {
                blockIds.addAll(intArrayOf(
                        getLeftDownBlockId(x, y),
                        getLeftBlockId(x, y),
                        getLeftUpBlockId(x, y),
                        getUpBlockId(x, y),
                        getRightUpBlockId(x, y)
                ).toCollection(ArrayList()))
            }

            "RU" -> {
                blockIds.addAll(intArrayOf(
                        getRightDownBlockId(x, y),
                        getRightBlockId(x, y),
                        getRightUpBlockId(x, y),
                        getUpBlockId(x, y),
                        getLeftUpBlockId(x, y)
                ).toCollection(ArrayList()))
            }

            "LD" -> {
                blockIds.addAll(intArrayOf(
                        getLeftUpBlockId(x, y),
                        getLeftBlockId(x, y),
                        getLeftDownBlockId(x, y),
                        getDownBlockId(x, y),
                        getRightDownBlockId(x, y)
                ).toCollection(ArrayList()))
            }

            "RD" -> {
                blockIds.addAll(intArrayOf(
                        getRightUpBlockId(x, y),
                        getRightBlockId(x, y),
                        getRightDownBlockId(x, y),
                        getDownBlockId(x, y),
                        getLeftDownBlockId(x, y)
                ).toCollection(ArrayList()))}
        }

        lateinit var levelMap: ArrayList<ArrayList<Char>>

        when (level) {
            0 -> levelMap = lowerMap

            1 -> levelMap = map

            2 -> levelMap = upperMap

            else -> return null
        }

        val objects = JSONArray()
        val blocks = JSONArray()
        for (id in blockIds) {
            if (id >= 0 && id < (getMapWidth()/blockSize) * (getMapHeight()/blockSize)) {
                var blockX = id * blockSize
                var blockY = 0

                while (width - blockSize < blockX) {
                    blockX -= width
                    blockY += blockSize
                }

                val block = JSONArray()
                for (i in 0 until blockSize) {
                    val line = JSONArray()
                    for (j in 0 until blockSize)
                        line.put(levelMap[i + blockY][j + blockX])

                    block.put(line)
                }
                blocks.put(JSONObject()
                        .put("id", id)
                        .put("block", block)
                )

                if (level != 1) continue
                val blockObjects = JSONArray()
                trees.forEach {
                    if (Math.floor(it.x.toDouble()).toInt() in blockX .. blockX + blockSize && Math.floor(it.y.toDouble()).toInt() in blockY .. blockY + blockSize)
                        blockObjects.put(JSONObject(it))
                }

                bushes.forEach {
                    if (Math.floor(it.x.toDouble()).toInt() in blockX .. blockX + blockSize && Math.floor(it.y.toDouble()).toInt() in blockY .. blockY + blockSize)
                        blockObjects.put(JSONObject(it))
                }

                bonfires.forEach {
                    if (Math.floor(it.x.toDouble()).toInt() in blockX .. blockX + blockSize && Math.floor(it.y.toDouble()).toInt() in blockY .. blockY + blockSize)
                        blockObjects.put(JSONObject(it))
                }

                sticks.forEach {
                    if (Math.floor(it.x.toDouble()).toInt() in blockX .. blockX + blockSize && Math.floor(it.y.toDouble()).toInt() in blockY .. blockY + blockSize)
                        blockObjects.put(JSONObject(it))
                }

                objects.put(
                        JSONObject()
                                .put("blockId", id)
                                .put("blockObjects", blockObjects)
                )
            }
        }

        return Pair(blocks, objects)
    }

//    @Synchronized private fun setCellValue(cell: Cell): Int {
//        when (cell.value) {
//            '"' -> {
//                map[cell.y][cell.x] = '!'
//                map[cell.y][cell.x] = cell.value
//            }
//
//            else -> {
//                map[cell.y][cell.x] = cell.value
//            }
//        }
//
//        return getBlockIdByCoords(cell.x, cell.y)
//    }
//
//    private fun updateBlocks(blockIds: ArrayList<Int>) {
//        for(blockId in blockIds) {
//            val blocks = getBlocks(blockId, "id")
//            val blockObject = JSONObject()
//            blockObject.put("id", "MP")
//            blockObject.put("idBlock", blockId)
//            for (block in blocks) {
//                val blockObj = JSONObject(block.toString())
//                val blockChars = StringBuilder()
//                val chunk = JSONArray(blockObj["chunk"].toString())
//                for (line in chunk) {
//                    for (char in JSONArray(line.toString())) {
//                        blockChars.append(char)
//                    }
//                }
//                blockObject.put("map", blockChars.toString())
//            }
//            for (clients in server.clients) {
//                if (clients.isBlockVisible(blockId)) {
//                    clients.sendMessage(blockObject.toString())
//                }
//            }
//        }
//    }
//
//    fun setCellValues(cells: ArrayList<Cell>){
//        val blockIds = ArrayList<Int>()
//        for (cell in cells) {
//            val blockId = setCellValue(cell)
//            if (!blockIds.contains(blockId)) blockIds.add(blockId)
//        }
//        updateBlocks(blockIds)
//    }




    companion object {
        data class MapObject(
                val id: Long,
                val name: String,
                var x: Float,
                var y: Float,
                val type: String,
                var params: String
        )

        const val blockSize = 8
    }
}