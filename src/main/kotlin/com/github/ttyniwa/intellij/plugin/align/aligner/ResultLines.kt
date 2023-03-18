package com.github.ttyniwa.intellij.plugin.align.aligner

class ResultLines(lineSize: Int) {
    private val lines = Array(lineSize) { "" }

    operator fun get(index: Int) = lines[index]

    operator fun set(index: Int, value: String) {
        lines[index] = value
    }

    fun toList() = lines.toList()

    fun findFurthestLength() = lines.map { it.length }.max()
}
