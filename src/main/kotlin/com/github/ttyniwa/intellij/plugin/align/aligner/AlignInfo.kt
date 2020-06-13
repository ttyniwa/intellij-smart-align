package com.github.ttyniwa.intellij.plugin.align.aligner

data class AlignInfo(val tokenType: TokenType, val leftPadding: Int, val rightPadding: Int, val isAlignLast: Boolean)

class AlignInfos(private val alignInfos:List<AlignInfo>) {
    val tokenTypes = alignInfos.map { it.tokenType }
    val tokenTypesExceptComment = alignInfos.map { it.tokenType }.minus(TokenType.EndOfLineComment)

    fun get(tokenType: TokenType): AlignInfo? {
        return alignInfos.find { it.tokenType == tokenType }
    }
}
