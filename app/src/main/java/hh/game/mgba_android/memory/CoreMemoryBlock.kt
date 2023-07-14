package hh.game.mgba_android.memory

data class CoreMemoryBlock(
    var id: Long,
    var internalName: String,
    var shortName: String,
    var longName: String,
    var start: Int,
    var end: Int,
    var size: Int,
    var flags: Int,
    var maxSegment: Short,
    var segmentStart: Int,
    var valuearray:ArrayList<Pair<Int,Int>>
)