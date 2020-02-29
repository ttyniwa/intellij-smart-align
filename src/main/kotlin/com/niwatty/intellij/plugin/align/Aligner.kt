package com.niwatty.intellij.plugin.align

import java.lang.IllegalArgumentException

class ResultLines(lineSize: Int) {
    private val lines = Array(lineSize) { "" }

    operator fun get(index: Int) = lines[index]

    operator fun set(index: Int, value: String) {
        lines[index] = value
    }

    fun toList() = lines.toList()

    fun findFurthestLength() = lines.map { it.length }.max()!!
}

object Aligner {
    fun align(text: String, anchor: Int): String {
        val alignTargetTokens = listOf(TokenType.Comma, TokenType.Colon, TokenType.Assign, TokenType.OneLineComment)
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
                StringTokenLexer("'"),
                StringTokenLexer("\""),
                OneLineCommentTokenLexer("//"),
                MultiLineCommentTokenLexer("/*", "*/")
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

    private fun align(lineRange: LineRange, alignTargetTokens: List<TokenType>): ResultLines {

        //
        // Remove whitespace around [alignTargetTokens]
        lineRange.lines.forEach { line -> line.trim(alignTargetTokens) }

        //
        // Align
        val resultLines = ResultLines(lineRange.size)
        val alignedTokenIndexes = IntArray(lineRange.size) { -1 }
        val isCodeAlignCompleted = BooleanArray(lineRange.size) { false }

        // Loop for each token to be aligned.
        do {
            var didProcess = false

            //
            // Joins the string before the token to be aligned to [resultLines].
            val alignTokenIndexes = IntArray(lineRange.size) { 0 }
            (0 until lineRange.size).forEach { i ->
                if (isCodeAlignCompleted[i]) return@forEach

                val line = lineRange.lines[i]
                alignTokenIndexes[i] = line.indexOf(alignTargetTokens, alignedTokenIndexes[i] + 1)

                if (alignTokenIndexes[i] >= 0) { // token found.
                    resultLines[i] += line.getRawTextBetween(alignedTokenIndexes[i] + 1, alignTokenIndexes[i])
                    if (line.tokens[alignTokenIndexes[i]].type == TokenType.OneLineComment) {
                        isCodeAlignCompleted[i] = true
                    }
                    alignedTokenIndexes[i] = alignTokenIndexes[i] - 1
                } else if (alignTokenIndexes[i] == -1) { // token not found.
                    resultLines[i] += line.getRawTextBetween(alignedTokenIndexes[i] + 1, line.tokens.size)
                    isCodeAlignCompleted[i] = true
                    alignedTokenIndexes[i] = line.tokens.size - 1
                }
            }

            //
            // find furthest line length
            val furthestLength = resultLines.findFurthestLength()

            //
            // align token
            (0 until lineRange.size).forEach { i ->
                if (isCodeAlignCompleted[i]) return@forEach

                val line = lineRange.lines[i]
                val paddingNum = furthestLength - resultLines[i].length

                val alignTargetToken = line.tokens[alignTokenIndexes[i]]
                if (alignTargetToken.type == TokenType.Assign || alignTargetToken.type == TokenType.Arrow) {
                    resultLines[i] += " ".repeat(paddingNum) + " " + alignTargetToken.text

                    if (alignTokenIndexes[i] == line.tokens.size - 1) {
                        isCodeAlignCompleted[i] = true
                    } else {
                        resultLines[i] += " "
                    }
                } else if (alignTargetToken.type == TokenType.Comma || alignTargetToken.type == TokenType.Colon) {

                    if (alignTokenIndexes[i] == line.tokens.size - 1) {
                        resultLines[i] += alignTargetToken.text
                        isCodeAlignCompleted[i] = true
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

        //
        // align one line comment.
        val furthestLength = resultLines.findFurthestLength()
        lineRange.lines.forEachIndexed { i, line ->
            if (alignedTokenIndexes[i] + 1 >= line.tokens.size) return@forEachIndexed

            val paddingNum = furthestLength - resultLines[i].length
            val comment = line.tokens[alignedTokenIndexes[i] + 1].text
            if (alignedTokenIndexes[i] + 1 != 0) {
                resultLines[i] += " "
            }
            resultLines[i] += " ".repeat(paddingNum) + comment
        }

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
