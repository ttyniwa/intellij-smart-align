package com.niwatty.intellij.plugin.align

import java.lang.IllegalArgumentException

enum class TokenType {
    Assign,
    Operator,
    Arrow,
    Comparator,
    Colon,
    Comma,
    StringLiteral,
    OneLineComment,
    MultiLineComment,
    Other,
    ;
}

data class Token(val type: TokenType, var text: String)

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

class Line(var tokens: List<Token>) {

    /**
     * Trim spaces around the specified [tokenTypes]
     */
    fun trim(tokenTypes: List<TokenType>) {
        (1 until tokens.size).forEach { i ->
            if (tokenTypes.contains(tokens[i].type)) {
                tokens[i - 1].text = tokens[i - 1].text.trimEnd()
                if (i + 1 < tokens.size) {
                    tokens[i + 1].text = tokens[i + 1].text.trimStart()
                }
            }
        }
    }

    /**
     * Returns the set of TokenTypes that intersect of [tokenTypes] and  TokenTypes in this Line
     */
    fun intersect(tokenTypes: List<TokenType>): Set<TokenType> {
        return tokens.map { it.type }.intersect(tokenTypes)
    }

    /**
     * Returns the index within this Line of the first occurrence of the specified TokenType, starting from the specified [startIndex].
     *
     * @return An index of the first occurrence of any [tokenTypes] or -1 if none is found.
     */
    fun indexOf(tokenTypes: List<TokenType>, startIndex: Int): Int {
        (startIndex until tokens.size).forEach { i ->
            if (tokenTypes.contains(tokens[i].type)) {
                return i
            }
        }
        return -1
    }

    /**
     * get raw text of tokens in the specified range.
     * @param startIndex (inclusive)
     * @param endIndex (exclusive)
     */
    fun getRawTextBetween(startIndex: Int, endIndex: Int): String {
        return (startIndex until endIndex)
                .joinToString("") { i -> tokens[i].text }
    }
}

class LineRange(var start: Int, var lines: MutableList<Line>) {

    val end: Int
        get() = start + lines.size - 1

    val size: Int
        get() = lines.size

    fun addHead(line: Line) {
        lines.add(0, line)
        start--
    }

    fun addTail(line: Line) {
        lines.add(line)
    }
}

object Aligner {
    fun align(text: String, anchor: Int): String {
        val alignTargetTokens = listOf(TokenType.Comma, TokenType.Colon, TokenType.Arrow, TokenType.Assign)
        val tokenLexers: List<TokenLexer> = listOf(
                SimpleTokenLexer("+=", TokenType.Assign),
                SimpleTokenLexer("-=", TokenType.Assign),
                SimpleTokenLexer("*=", TokenType.Assign),
                SimpleTokenLexer("/=", TokenType.Assign),
                SimpleTokenLexer("=", TokenType.Assign),
                SimpleTokenLexer("->", TokenType.Arrow),
                SimpleTokenLexer("=>", TokenType.Comparator),
                SimpleTokenLexer("=<", TokenType.Comparator),
                SimpleTokenLexer(">=", TokenType.Comparator),
                SimpleTokenLexer("<=", TokenType.Comparator),
                SimpleTokenLexer("::", TokenType.Operator),
                SimpleTokenLexer(":", TokenType.Colon),
                SimpleTokenLexer(",", TokenType.Comma),
                StringTokenLexer("'", TokenType.StringLiteral),
                StringTokenLexer("\"", TokenType.StringLiteral),
                OneLineCommentTokenLexer("//", TokenType.OneLineComment),
                MultiLineCommentTokenLexer("/*", "*/", TokenType.MultiLineComment)
        )

        val lineSeparator = findLineSeparator(text)
        val rawLines = text.split(lineSeparator)

        val alignLines = detectLinesToAlign(rawLines, anchor, alignTargetTokens, tokenLexers)
        val formattedLines = align(alignLines, alignTargetTokens)

        return listOf(
                rawLines.subList(0, alignLines.start),
                formattedLines.toList(),
                rawLines.subList(alignLines.end + 1, rawLines.size)
        )
                .flatten()
                .joinToString(lineSeparator)
    }

    private fun align(lineRange: LineRange, alignTargetTokens: List<TokenType>): Array<String> {

        //
        // Remove whitespace around [alignTargetTokens]
        lineRange.lines.forEach { line -> line.trim(alignTargetTokens) }

        //
        // Align
        val resultLines = Array(lineRange.size) { "" }
        val alignedTokenIndexes = IntArray(lineRange.size) { -1 }
        val isCompleted = BooleanArray(lineRange.size) { false }

        // Loop for each token to be aligned.
        do {
            var didProcess = false

            //
            // Joins the string before the token to be aligned to [resultLines].
            val alignTokenIndexes = IntArray(lineRange.size) { 0 }
            (0 until lineRange.size).forEach { i ->
                if (isCompleted[i]) return@forEach

                val line = lineRange.lines[i]
                alignTokenIndexes[i] = line.indexOf(alignTargetTokens, alignedTokenIndexes[i] + 1)

                if (alignTokenIndexes[i] > 0) { // token found.
                    resultLines[i] += line.getRawTextBetween(alignedTokenIndexes[i] + 1, alignTokenIndexes[i])
                } else if (alignTokenIndexes[i] == -1) { // token not found.
                    resultLines[i] += line.getRawTextBetween(alignedTokenIndexes[i] + 1, line.tokens.size)
                    isCompleted[i] = true
                }
            }

            //
            // find furthest line length
            val furthestLength = resultLines.map { it.length }.max()!!

            //
            // align token
            (0 until lineRange.size).forEach { i ->
                if (isCompleted[i]) return@forEach

                val line = lineRange.lines[i]
                val paddingNum = furthestLength - resultLines[i].length

                val alignTargetToken = line.tokens[alignTokenIndexes[i]]
                if (alignTargetToken.type == TokenType.Assign || alignTargetToken.type == TokenType.Arrow) {
                    resultLines[i] += " ".repeat(paddingNum) + " " + alignTargetToken.text

                    if (alignTokenIndexes[i] == line.tokens.size - 1) {
                        isCompleted[i] = true
                    } else {
                        resultLines[i] += " "
                    }
                } else if (alignTargetToken.type == TokenType.Comma || alignTargetToken.type == TokenType.Colon) {

                    if (alignTokenIndexes[i] == line.tokens.size - 1) {
                        resultLines[i] += alignTargetToken.text
                        isCompleted[i] = true
                    } else {
                        resultLines[i] += " ".repeat(paddingNum) + alignTargetToken.text + " "
                    }
                } else {
                    throw IllegalArgumentException("Not supported token type to align. " + alignTargetToken.type)
                }

                alignedTokenIndexes[i] = alignTokenIndexes[i]
                didProcess = true
            }
        } while (didProcess)

        return resultLines
    }

    /**
     * Detect lines to align around the specified [anchor] line.
     */
    private fun detectLinesToAlign(rawLines: List<String>, anchor: Int, alignTargetTokens: List<TokenType>, tokenLexers: List<TokenLexer>): LineRange {
        val lexer = Lexer(tokenLexers)
        val anchorLine = Line(lexer.tokenize(rawLines[anchor]))
        val lines = LineRange(anchor, mutableListOf(anchorLine))
        var commonTokens = anchorLine.intersect(alignTargetTokens)

        // find start line to align.
        for (i in anchor - 1 downTo 0) {
            val line = Line(lexer.tokenize(rawLines[i]))

            val ct = commonTokens.intersect(line.intersect(alignTargetTokens))
            if (ct.isEmpty()) {
                break
            }

            commonTokens = ct
            lines.addHead(line)
        }

        // find end line to align.
        for (i in anchor + 1 until rawLines.size) {
            val line = Line(lexer.tokenize(rawLines[i]))

            val ct = commonTokens.intersect(line.intersect(alignTargetTokens))
            if (ct.isEmpty()) {
                break
            }

            commonTokens = ct
            lines.addTail(line)
        }

        return lines
    }

    private fun findLineSeparator(text: String): String {
        val lineSeparatorIndex = text.indexOf("\n")

        if (lineSeparatorIndex == -1) return ""
        if (lineSeparatorIndex == 0) return "\n"
        return if (text[lineSeparatorIndex - 1] == '\r') "\r\n" else "\n"
    }
}
