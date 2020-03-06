package com.github.ttyniwa.intellij.plugin.align

@Suppress("unused")
class AlignDemoCode {
    fun alignAroundCursor() {
        var aaa = 1
        var b = 2
        b += 2
        aaa += 1

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
        var notAligned = 1
        var aaa  = 1
        var b    = 2
        aaa     += 123
        b       += 2
        var notAligned2 = 1

        notAligned += 1
        notAligned2 += 1
    }
}