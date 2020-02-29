package com.niwatty.intellij.plugin.align

data class Line(var tokens: List<Token>) {

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

        // remove token if empty
        tokens = tokens.filter { it.text.isNotEmpty() }
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

