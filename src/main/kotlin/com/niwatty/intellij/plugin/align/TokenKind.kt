package com.niwatty.intellij.plugin.align

import java.lang.IllegalArgumentException

enum class TokenType {
    Assign,
    Operator,
    Arrow,
    Comparator,
    Colon,
    Comma,
    Word,
    StringLiteral,
    OneLineComment,
    MultiLineComment,
    ;
}

data class Token(val type: TokenType, var text: String)

object Lexer {
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

    fun tokenize(text: String): MutableList<Token> {
        var lastTokenStartPos = 0
        val tokens: MutableList<Token> = mutableListOf()
        var isTokenTypeOfLastCharOther = false

        var pos = 0
        while (pos < text.length) {
            var nextSeek = 1

            val foundToken = tokenLexers
                    .mapNotNull { lexer -> lexer.tokenize(text, pos) }
                    .maxBy { token -> token.text.length }
            if (foundToken != null) {
                if (isTokenTypeOfLastCharOther) {
                    tokens.add(Token(TokenType.Word, text.substring(lastTokenStartPos, pos)))
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
            tokens.add(Token(TokenType.Word, text.substring(lastTokenStartPos, pos)))
        }

        return tokens
    }
}

class Line(var tokens: List<Token>) {
    fun getAlignTokens(): List<Token> {
        return tokens.filter {
            listOf(TokenType.Assign, TokenType.Arrow, TokenType.Comma, TokenType.Colon).contains(it.type)
        }
    }

    fun indexOfToken(tokenTypes: List<TokenType>, startIndex: Int): Int {
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

class Lines(var start: Int, var end: Int, var lines: MutableList<Line>) {

    val size: Int
        get() = end - start + 1

    fun addHead(line: Line) {
        lines.add(0, line)
        start--
    }

    fun addTail(line: Line) {
        lines.add(line)
        end++
    }
}

object TokenAligner {
    fun align(text: String, anchor: Int): String {
        val lineSeparator = findLineSeparator(text)
        val rawLines = text.split(lineSeparator)

        val alignLines = detectAlignLines(rawLines, anchor)
        val formattedLines = align(alignLines)

        return listOf(
                rawLines.subList(0, alignLines.start),
                formattedLines.toList(),
                rawLines.subList(alignLines.end + 1, rawLines.size)
        )
                .flatten()
                .joinToString(lineSeparator)
    }

    private fun align(lines: Lines): Array<String> {
        val alignTargetTokens = listOf(TokenType.Comma, TokenType.Colon, TokenType.Arrow, TokenType.Assign)

        //
        // Remove whitespace surrounding operator
        lines.lines.forEach { line ->
            (1 until line.tokens.size).forEach { i ->
                if (alignTargetTokens.contains(line.tokens[i].type)) {
                    line.tokens[i - 1].text = line.tokens[i - 1].text.trimEnd()
                    if (i + 1 < line.tokens.size) {
                        line.tokens[i + 1].text = line.tokens[i + 1].text.trimStart()
                    }
                }
            }
        }

        //
        // Align

        // align済み文字列
        val resultLines = Array(lines.size) { "" }
        // align済みトークンの最終index
        val alignedTokenIndexes = IntArray(lines.size) { -1 }
        // 完了済みかどうか
        val isCompleted = BooleanArray(lines.size) { false }
        do {
            var didProcess = false

            //
            // align対象のトークンより前の文字列をresultに結合する

            // align対象のトークンのindex
            val alignTokenIndexes = IntArray(lines.size) { 0 }
            (0 until lines.size).forEach { i ->
                if (isCompleted[i]) return@forEach

                val line = lines.lines[i]
                alignTokenIndexes[i] = line.indexOfToken(alignTargetTokens, alignedTokenIndexes[i] + 1)

                if (alignTokenIndexes[i] > 0) {
                    resultLines[i] += line.getRawTextBetween(alignedTokenIndexes[i] + 1, alignTokenIndexes[i])
                } else if (alignTokenIndexes[i] == -1) {
                    // no align target token found.

                    resultLines[i] += line.getRawTextBetween(alignedTokenIndexes[i] + 1, line.tokens.size)
                    isCompleted[i] = true
                }
            }

            //
            // 最も長い行を探す
            val furthestLength = resultLines.map { it.length }.max()!!

            //
            // align sign
            (0 until lines.size).forEach { i ->
                if (isCompleted[i]) return@forEach

                val line = lines.lines[i]
                val paddingNum = furthestLength - resultLines[i].length

                val alignTargetToken = line.tokens[alignTokenIndexes[i]]
                if (alignTargetToken.type == TokenType.Assign || alignTargetToken.type == TokenType.Arrow) {
                    resultLines[i] += " ".repeat(paddingNum) + " " + alignTargetToken.text
                    // 末尾トークンなら処理終了
                    if (alignTokenIndexes[i] == line.tokens.size - 1) {
                        isCompleted[i] = true
                    } else {
                        resultLines[i] += " "
                    }
                } else if (alignTargetToken.type == TokenType.Comma || alignTargetToken.type == TokenType.Colon) {
                    resultLines[i] += " ".repeat(paddingNum) + alignTargetToken.text
                    // 末尾トークンなら処理終了
                    if (alignTokenIndexes[i] == line.tokens.size - 1) {
                        isCompleted[i] = true
                    } else {
                        resultLines[i] += " "
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

    private fun detectAlignLines(rawLines: List<String>, anchor: Int): Lines {
        val anchorLine = Line(Lexer.tokenize(rawLines[anchor]))
        val lines = Lines(anchor, anchor, mutableListOf(anchorLine))
        var commonTokens = anchorLine.getAlignTokens().map { it.type }.toMutableSet()

        for (i in anchor - 1 downTo 0) {
            //            if (lines[i].shouldIgnoreLine()) {
//                return i + 1
//            }
            val line = Line(Lexer.tokenize(rawLines[i]))
            // copy commonTokens
            val ct = commonTokens.toMutableSet()
            ct.retainAll(line.getAlignTokens().map { it.type })
            if (ct.isEmpty()) {
                break
            }

            commonTokens = ct
            lines.addHead(line)
        }

        for (i in anchor + 1 until rawLines.size) {
            val line = Line(Lexer.tokenize(rawLines[i]))
            // copy commonTokens
            val ct = commonTokens.toMutableSet()
            ct.retainAll(line.getAlignTokens().map { it.type })
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
