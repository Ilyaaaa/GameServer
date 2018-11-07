package data.objects

import data.enums.MapObjectsType

data class Stick(val id: Long, var name: String, var x: Float, var y: Float): MapObject(id, name, x, y, MapObjectsType.STICK)