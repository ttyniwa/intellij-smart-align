package com.niwatty.intellij.plugin.align

interface TokenLexer {
    fun tokenize(text: String, startIndex: Int): Token?
}

class SimpleTokenLexer(private val tokenPhrase: String, private val tokenType: TokenType) : TokenLexer {
    override fun tokenize(text: String, startIndex: Int): Token? {
        if (!text.startsWith(tokenPhrase, startIndex)) return null

        return Token(tokenType, tokenPhrase)
    }
}

class OneLineCommentTokenLexer(private val startPhrase: String, private val tokenType: TokenType) : TokenLexer {
    override fun tokenize(text: String, startIndex: Int): Token? {
        if (!text.startsWith(startPhrase, startIndex)) return null

        return Token(tokenType, text.substring(startIndex))
    }
}

class MultiLineCommentTokenLexer(private val startPhrase: String, private val endPhrase: String, private val tokenType: TokenType) : TokenLexer {
    override fun tokenize(text: String, startIndex: Int): Token? {
        if (!text.startsWith(startPhrase, startIndex)) return null

        val endIndex = text.indexOf(endPhrase, startIndex + startPhrase.length)
        return if (endIndex == -1) {
            Token(tokenType, text.substring(startIndex))
        } else {
            Token(tokenType, text.substring(startIndex, endIndex))
        }
    }
}

class StringTokenLexer(private val enclosurePhrase: String, private val tokenType: TokenType) : TokenLexer {
    override fun tokenize(text: String, startIndex: Int): Token? {
        if (!text.startsWith(enclosurePhrase, startIndex)) return null

        var pos = startIndex + enclosurePhrase.length
        while (pos < text.length) {
            val char = text[pos]
            if (char == '\\') {
                pos += 2
            } else if (text.startsWith(enclosurePhrase, pos)) {
                return Token(tokenType, text.substring(startIndex, pos + enclosurePhrase.length))
            }
            pos++
        }
        return Token(tokenType, text.substring(startIndex))
    }
}

