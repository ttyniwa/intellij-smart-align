package com.github.ttyniwa.intellij.plugin.align.aligner

interface TokenLexer {
    fun tokenize(text: String, startIndex: Int): Token?
}

class SimpleTokenLexer(private val tokenPhrase: String, private val tokenType: TokenType) : TokenLexer {
    override fun tokenize(text: String, startIndex: Int): Token? {
        if (!text.startsWith(tokenPhrase, startIndex)) return null

        return Token(tokenType, tokenPhrase)
    }
}

open class RegexTokenLexer(pattern: String, private val tokenType: TokenType) : TokenLexer {
    private val regex: Regex = "^$pattern".toRegex()

    override fun tokenize(text: String, startIndex: Int): Token? {
        val result = regex.find(text, startIndex) ?: return null

        return Token(tokenType, result.groupValues[0])
    }
}

class EndOfLineCommentTokenLexer(private val startPhrase: String) : TokenLexer {
    override fun tokenize(text: String, startIndex: Int): Token? {
        if (!text.startsWith(startPhrase, startIndex)) return null

        return Token(TokenType.EndOfLineComment, text.substring(startIndex))
    }
}

class BlockCommentTokenLexer(private val startPhrase: String, private val endPhrase: String) : TokenLexer {
    override fun tokenize(text: String, startIndex: Int): Token? {
        if (!text.startsWith(startPhrase, startIndex)) return null

        val endIndex = text.indexOf(endPhrase, startIndex + startPhrase.length)
        return if (endIndex == -1) {
            Token(TokenType.BlockComment, text.substring(startIndex))
        } else {
            Token(TokenType.BlockComment, text.substring(startIndex, endIndex + endPhrase.length))
        }
    }
}

class StringTokenLexer(private val enclosurePhrase: String) : TokenLexer {
    override fun tokenize(text: String, startIndex: Int): Token? {
        if (!text.startsWith(enclosurePhrase, startIndex)) return null

        var pos = startIndex + enclosurePhrase.length
        while (pos < text.length) {
            val char = text[pos]
            if (char == '\\') {
                pos += 2
            } else if (text.startsWith(enclosurePhrase, pos)) {
                return Token(TokenType.StringLiteral, text.substring(startIndex, pos + enclosurePhrase.length))
            }
            pos++
        }
        return Token(TokenType.StringLiteral, text.substring(startIndex))
    }
}
