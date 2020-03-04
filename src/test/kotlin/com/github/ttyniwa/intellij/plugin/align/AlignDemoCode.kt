package com.github.ttyniwa.intellij.plugin.align

class AlignDemoCode {
    fun alignAroundCursor() {
        var aaa = 1
        var b = 2
        b += 2

        listOf(
                listOf("hello", "world"),
                listOf("say hello to", "world"),
                listOf("good bye,", "word")
        )

        listOf(
                "hello", "world", // comment
                "hello world", "yes",   // comment
                "good bye"
        )
    }

    fun selectAndAlign() {
        val notAligned = 1
        var aaa = 1
        var b = 2

        aaa += 123
        b += 2
        val notAligned2 = 1
    }
}