package com.github.ttyniwa.intellij.plugin.align.aligner

object Aligner {
    //
    // settings
    private val tokenLexers: List<TokenLexer> = listOf(
        // @formatter:off
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
        SimpleTokenLexer("[", TokenType.Bracket),
        SimpleTokenLexer("]", TokenType.Bracket),
        SimpleTokenLexer("{", TokenType.Bracket),
        SimpleTokenLexer("}", TokenType.Bracket),
        SimpleTokenLexer("(", TokenType.Bracket),
        SimpleTokenLexer(")", TokenType.Bracket),
        SimpleTokenLexer("?", TokenType.Comma),
        StringTokenLexer("'"),
        StringTokenLexer("\""),
        EndOfLineCommentTokenLexer("//"),
        BlockCommentTokenLexer("/*", "*/")
        // @formatter:on
    )
    private val alignInfos = AlignInfos(
        listOf(
            AlignInfo(TokenType.Assign, 1, 1, true),
            AlignInfo(TokenType.Colon, 0, 1, false),
            AlignInfo(TokenType.Comma, 0, 1, false),
            AlignInfo(TokenType.EndOfLineComment, 1, 0, false)
        )
    )

    /**
     * Align surrounding [anchor] lines.
     */
    fun align(text: String, anchor: Int): String {
        val lineSeparator = findLineSeparator(text)
        val rawLines = text.split(lineSeparator)

        val lineRange = detectLinesToAlign(rawLines, anchor)

        return align(rawLines, lineRange, lineSeparator)
    }

    /**
     * Align [rowRange] lines.
     */
    fun align(text: String, rowRange: IntRange): String {
        val lineSeparator = findLineSeparator(text)
        val rawLines = text.split(lineSeparator)

        val lexer = Lexer(tokenLexers)
        val lines = rowRange.map { Line(lexer.tokenize(rawLines[it])) }
        val lineRange = LineRange(rowRange.first, lines.toMutableList())

        return align(rawLines, lineRange, lineSeparator)
    }

    private fun align(rawLines: List<String>, lineRange: LineRange, lineSeparator: String): String {
        val formattedLines = align(lineRange)

        return listOf(
            rawLines.subList(0, lineRange.start),
            formattedLines.toList(),
            rawLines.subList(lineRange.end + 1, rawLines.size)
        )
            .flatten()
            .joinToString(lineSeparator)
    }

    private fun align(lineRange: LineRange): ResultLines {
        // option
        val isPaddingTokenRight = true

        //
        // 1. find the line that has no [alignTargetTokens] except EOL Comment, and mark it will not align.
        val isAlignIgnorableLine = lineRange.lines.map { line ->
            line.indexOf(alignInfos.tokenTypesExceptComment, 0) == -1
        }

        //
        // 2. Remove whitespace around [alignTargetTokens]
        lineRange.lines
            .filterIndexed { i, _ -> !isAlignIgnorableLine[i] }
            .forEach { line -> line.trim(alignInfos.tokenTypes) }

        //
        // 3. Align
        val resultLines = ResultLines(lineRange.size)
        val alignedTokenIndexes = IntArray(lineRange.size) { -1 }
        val isCodeAlignCompleted = BooleanArray(lineRange.size) { false }

        // Loop for each token to be aligned.
        do {
            var didProcess = false

            //
            // Joins the string before the token to be aligned to [resultLines].
            lineRange.lines.forEachIndexed { i, line ->
                if (isAlignIgnorableLine[i]) return@forEachIndexed
                if (isCodeAlignCompleted[i]) return@forEachIndexed

                val alignTokenIndex = line.indexOf(alignInfos.tokenTypes, alignedTokenIndexes[i] + 1)
                val alignToken = line.tokens.getOrNull(alignTokenIndex)

                if (alignToken != null) { // token found.
                    resultLines[i] += line.getRawTextBetween(alignedTokenIndexes[i] + 1, alignTokenIndex)
                    if (alignToken.type == TokenType.EndOfLineComment) {
                        isCodeAlignCompleted[i] = true
                    }
                    alignedTokenIndexes[i] = alignTokenIndex - 1
                } else { // token not found.
                    resultLines[i] += line.getRawTextBetween(alignedTokenIndexes[i] + 1, line.tokens.size)
                    isCodeAlignCompleted[i] = true
                    alignedTokenIndexes[i] = line.tokens.size - 1
                }
            }

            //
            // find furthest line length
            val furthestLength = (0 until lineRange.lines.size)
                .filter { !isAlignIgnorableLine[it] }
                .map { resultLines[it].length }
                .maxOrNull() ?: 0

            val longestOperatorLength = lineRange.lines
                .mapIndexed { i, line -> line.tokens.getOrNull(alignedTokenIndexes[i] + 1)?.text?.length }
                .filterIndexed { i, _ -> !isCodeAlignCompleted[i] && !isAlignIgnorableLine[i] }
                .filterNotNull()
                .maxOrNull()

            //
            // align token
            lineRange.lines.forEachIndexed { i, line ->
                if (isAlignIgnorableLine[i]) return@forEachIndexed
                if (isCodeAlignCompleted[i]) return@forEachIndexed

                val currentToken = line.tokens[alignedTokenIndexes[i] + 1]
                val nextToken = line.tokens.getOrNull(alignedTokenIndexes[i] + 2)

                val paddingNum = furthestLength - resultLines[i].length
                val paddingInfo = alignInfos.get(currentToken.type) ?: error("padding info not found.")
                val numOfPaddingTokenRight = if (isPaddingTokenRight) {
                    longestOperatorLength!! - currentToken.text.length
                } else {
                    0
                }

                val isLastTokenToAlign = nextToken == null || nextToken.type == TokenType.EndOfLineComment
                val leftPadding = if (!isLastTokenToAlign || paddingInfo.isAlignLast) {
                    " ".repeat(paddingNum + paddingInfo.leftPadding + numOfPaddingTokenRight)
                } else {
                    ""
                }
                if (currentToken.type in alignInfos.tokenTypesExceptComment) {
                    resultLines[i] += leftPadding + currentToken.text

                    if (isLastTokenToAlign) {
                        isCodeAlignCompleted[i] = true
                    } else {
                        resultLines[i] += " ".repeat(paddingInfo.rightPadding)
                    }
                } else {
                    throw IllegalArgumentException("Not supported token type to align. " + currentToken.type)
                }

                alignedTokenIndexes[i] = alignedTokenIndexes[i] + 1
                didProcess = true
            }
        } while (didProcess)

        //
        // 4. align EOL comment.
        val furthestLength = resultLines.findFurthestLength()
        val paddingInfo = alignInfos.get(TokenType.EndOfLineComment) ?: error("padding info not found.")
        lineRange.lines.forEachIndexed { i, line ->
            if (isAlignIgnorableLine[i]) return@forEachIndexed

            val currentTokenIndex = alignedTokenIndexes[i] + 1
            if (currentTokenIndex >= line.tokens.size) return@forEachIndexed

            // if no token to align found, don't align EOL Comment.
            val isExistAlignTarget = line.isExists(alignInfos.tokenTypesExceptComment)
            if (!isExistAlignTarget) {
                resultLines[i] += line.tokens[currentTokenIndex].text
                return@forEachIndexed
            }

            // align eol comment.
            val paddingNum = furthestLength - resultLines[i].length
            val comment = line.tokens[currentTokenIndex].text
            if (currentTokenIndex != 0) {
                resultLines[i] += " ".repeat(paddingInfo.leftPadding)
            }
            resultLines[i] += " ".repeat(paddingNum) + comment
        }

        //
        // 5. merge ignored lines.
        lineRange.lines.forEachIndexed { i, line ->
            if (isAlignIgnorableLine[i]) {
                resultLines[i] = line.getRawTextBetween(0, line.tokens.size)
            }
        }

        return resultLines
    }

    /**
     * Detect lines to align around the specified [anchor] line.
     */
    private fun detectLinesToAlign(rawLines: List<String>, anchor: Int): LineRange {
        // option
        val distinctBracketPattern = false

        val lexer = Lexer(tokenLexers)
        val anchorLine = Line(lexer.tokenize(rawLines[anchor]))
        val lineRange = LineRange(anchor, mutableListOf(anchorLine))
        var commonTokens = anchorLine.intersect(alignInfos.tokenTypesExceptComment)

        // find start line to align.
        for (i in anchor - 1 downTo 0) {
            val line = Line(lexer.tokenize(rawLines[i]))

            val ct = commonTokens.intersect(line.intersect(alignInfos.tokenTypes))
            if (ct.isEmpty()) {
                break
            }
            if (distinctBracketPattern) {
                if (!line.isSamePattern(anchorLine, TokenType.Bracket)) {
                    break
                }
            }

            commonTokens = ct
            lineRange.addHead(line)
        }

        // find end line to align.
        for (i in anchor + 1 until rawLines.size) {
            val line = Line(lexer.tokenize(rawLines[i]))

            val ct = commonTokens.intersect(line.intersect(alignInfos.tokenTypes))
            if (ct.isEmpty()) {
                break
            }
            if (distinctBracketPattern) {
                if (!line.isSamePattern(anchorLine, TokenType.Bracket)) {
                    break
                }
            }

            commonTokens = ct
            lineRange.addTail(line)
        }

        return lineRange
    }

    private fun findLineSeparator(text: String): String {
        val lineSeparatorIndex = text.indexOf("\n")

        if (lineSeparatorIndex == -1) return ""
        if (lineSeparatorIndex == 0) return "\n"
        return if (text[lineSeparatorIndex - 1] == '\r') "\r\n" else "\n"
    }
}
