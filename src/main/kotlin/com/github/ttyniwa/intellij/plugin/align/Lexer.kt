package com.github.ttyniwa.intellij.plugin.align

/**
 * Lexical analyzer.
 */
class Lexer(private val tokenLexers: List<TokenLexer>) {

    /**
     * Divide given text into tokens and return the list.
     */
    fun tokenize(text: String): MutableList<Token> {
        val tokens: MutableList<Token> = mutableListOf()
        var lastTokenStartPos = 0
        var isTokenTypeOfLastCharOther = false

        var pos = 0
        while (pos < text.length) {
            var nextSeek = 1

            val foundToken = tokenLexers
                    .mapNotNull { lexer -> lexer.tokenize(text, pos) }
                    .maxBy { token -> token.text.length }
            if (foundToken != null) {
                if (isTokenTypeOfLastCharOther) {
                    tokens.add(Token(TokenType.Other, text.substring(lastTokenStartPos, pos)))
                    isTokenTypeOfLastCharOther = false
                }

                tokens.add(foundToken)
                nextSeek = foundToken.text.length
            } else {
                if (!isTokenTypeOfLastCharOther) {
                    lastTokenStartPos = pos
                    isTokenTypeOfLastCharOther = true
                }
            }

            pos += nextSeek
        }

        if (isTokenTypeOfLastCharOther) {
            tokens.add(Token(TokenType.Other, text.substring(lastTokenStartPos, pos)))
        }

        return tokens
    }
}

