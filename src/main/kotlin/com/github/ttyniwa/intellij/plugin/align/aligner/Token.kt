package com.github.ttyniwa.intellij.plugin.align.aligner

enum class TokenType {
    Assign,
    Operator,
    Arrow,
    Comparator,
    Colon,
    Comma,
    StringLiteral,
    EndOfLineComment,
    BlockComment,
    Bracket,
    Other,
    ;
}

data class Token(val type: TokenType, var text: String)
