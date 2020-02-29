package com.niwatty.intellij.plugin.align

import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek

object LineSpek : Spek({

    group(".trim()") {
        val data = listOf(
                // space around token.
                // @formatter:off
                Pair(listOf(
                        Token(TokenType.Other, " space "),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Other, " space ")
                ), listOf(
                        Token(TokenType.Other, " space"),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Other, "space ")
                )),
                // no space around token.
                Pair(listOf(
                        Token(TokenType.Other, " space"),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Other, "space ")
                ), listOf(
                        Token(TokenType.Other, " space"),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Other, "space ")
                )),
                // every tokens.
                Pair(listOf(
                        Token(TokenType.Other, " space "),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Other, " space "),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Other, " space ")
                ), listOf(
                        Token(TokenType.Other, " space"),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Other, "space"),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Other, "space ")
                )),
                // continuous tokens.
                Pair(listOf(
                        Token(TokenType.Other, " space "),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Other, " space ")
                ), listOf(
                        Token(TokenType.Other, " space"),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Assign, "="),
                        Token(TokenType.Other, "space ")
                ))
                // @formatter:on
        )

        data.forEach {
            val input = Line(it.first)
            val expected = Line(it.second)
            input.trim(listOf(TokenType.Assign))

            test("can trim $input") {
                assertThat(input).isEqualTo(expected)
            }
        }
    }

    group(".intersect()") {
        val data = listOf(
                // @formatter:off
                Triple(listOf(
                        Token(TokenType.Other, ""),
                        Token(TokenType.Assign, ""),
                        Token(TokenType.Colon, "")
                ), listOf(
                        TokenType.Other
                ),
                        setOf(TokenType.Other)
                ),
                Triple(listOf(
                        Token(TokenType.Other, ""),
                        Token(TokenType.Other, "")
                ), listOf(
                        TokenType.Other
                ),
                        setOf(TokenType.Other)
                ),
                Triple(listOf(
                        Token(TokenType.Other, ""),
                        Token(TokenType.Assign, "")
                ), listOf(
                        TokenType.Colon
                ),
                        setOf()
                )
                // @formatter:on
        )

        data.forEach {
            val input = Line(it.first)
            val result = input.intersect(it.second)
            val expected = it.third

            test("intersect ${it.second} and $input") {
                assertThat(result).isEqualTo(expected)
            }
        }
    }

    group(".indexOf()") {
        data class Data(val tokens: List<Token>, val findTokenTypes: List<TokenType>, val startIndex: Int, val expected: Int)

        val data = listOf(
                // @formatter:off
                Data(
                        listOf(
                                Token(TokenType.Other, ""),
                                Token(TokenType.Assign, ""),
                                Token(TokenType.Colon, "")
                        ),
                        listOf(TokenType.Other),
                        0,
                        0
                ),
                Data(
                        listOf(
                                Token(TokenType.Other, ""),
                                Token(TokenType.Assign, ""),
                                Token(TokenType.Colon, "")
                        ),
                        listOf(TokenType.Colon),
                        0,
                        2
                ),
                Data(
                        listOf(
                                Token(TokenType.Other, ""),
                                Token(TokenType.Other, ""),
                                Token(TokenType.Other, "")
                        ),
                        listOf(TokenType.Other),
                        1,
                        1
                ),
                Data(
                        listOf(
                                Token(TokenType.Other, ""),
                                Token(TokenType.Assign, ""),
                                Token(TokenType.Colon, "")
                        ),
                        listOf(TokenType.Colon, TokenType.Assign),
                        0,
                        1
                ),
                // not found.
                Data(
                        listOf(
                                Token(TokenType.Other, ""),
                                Token(TokenType.Assign, ""),
                                Token(TokenType.Colon, "")
                        ),
                        listOf(TokenType.Other),
                        1,
                        -1
                )
                // @formatter:on
        )

        data.forEach {
            val input = Line(it.tokens)
            val result = input.indexOf(it.findTokenTypes, it.startIndex)
            val expected = it.expected

            test("indexOf(${it.findTokenTypes}, ${it.startIndex})") {
                assertThat(result).isEqualTo(expected)
            }
        }
    }

})
