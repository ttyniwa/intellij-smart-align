package com.niwatty.intellij.plugin.align

import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek

object TokenLexerSpek : Spek({
    group("SimpleTokenLexer") {
        val lexer = SimpleTokenLexer("=", TokenType.Assign)
        listOf(
                // @formatter:off
                Triple("="      , 0, Token(TokenType.Assign, "=")),
                Triple("0=after", 1, Token(TokenType.Assign, "=")),
                Triple("0=after", 0, null)
                // @formatter:on
        ).forEach {
            val input = it.first
            val startIndex = it.second
            val expected = it.third

            test("tokenize($input, $startIndex)") {
                val result = lexer.tokenize(input, startIndex)
                assertThat(result).isEqualTo(expected)
            }
        }
    }

    group("StringLiteralTokenLexer") {
        listOf("'", "\"", "`").forEach { q ->
            val lexer = StringTokenLexer(q)
            listOf(
                    // @formatter:off
                    Triple("""${q}hoge fuga${q}"""         , 0 , Token(TokenType.StringLiteral, """${q}hoge fuga${q}""")),
                    Triple("""${q}hoge\${q}fuga${q}"""     , 0 , Token(TokenType.StringLiteral, """${q}hoge\${q}fuga${q}""")),
                    Triple("""${q}hoge${q}fuga${q}"""      , 0 , Token(TokenType.StringLiteral, """${q}hoge${q}""")),
                    Triple(""" ${q}hoge\${q}fuga${q}"""    , 0 , null),
                    Triple("""hoge\${q}fuga${q}"""         , 0 , null),
                    Triple("""${q}hoge\${q}fuga${q}"""     , 1 , null),
                    Triple("""${q}hoge\${q}fuga${q}after""", 12, null)
                    // @formatter:on
            ).forEach {
                val input = it.first
                val startIndex = it.second
                val expected = it.third

                test("$q string literal tokenize($input, $startIndex)") {
                    val result = lexer.tokenize(input, startIndex)
                    assertThat(result).isEqualTo(expected)
                }
            }
        }
    }

    group("OneLineCommentTokenLexer") {
        val lexer = OneLineCommentTokenLexer("//")
        listOf(
                // @formatter:off
                Triple("""// 1"""       , 0, Token(TokenType.OneLineComment, """// 1""")),
                Triple("""0 // 1"""     , 2, Token(TokenType.OneLineComment, """// 1""")),
                Triple("""0 // 1 // a""", 2, Token(TokenType.OneLineComment, """// 1 // a""")),
                Triple("""0 // 1"""     , 0, null),
                Triple("""0 // 1"""     , 3, null)
                // @formatter:on
        ).forEach {
            val input = it.first
            val startIndex = it.second
            val expected = it.third

            test("tokenize($input, $startIndex)") {
                val result = lexer.tokenize(input, startIndex)
                assertThat(result).isEqualTo(expected)
            }
        }
    }

    group("MultiLineCommentTokenLexer") {
        val lexer = MultiLineCommentTokenLexer("/*", "*/")
        listOf(
                // @formatter:off
                Triple("/* 1 */"      , 0, Token(TokenType.MultiLineComment, "/* 1 */")),
                Triple("0 /* 1 */"    , 2, Token(TokenType.MultiLineComment, "/* 1 */")),
                Triple("/* 1 /* a */" , 0, Token(TokenType.MultiLineComment, "/* 1 /* a */")),
                Triple("/* 1"         , 0, Token(TokenType.MultiLineComment, "/* 1")),
                Triple("/* 1 */ after", 0, Token(TokenType.MultiLineComment, "/* 1 */")),
                Triple("0 /* 1 */"    , 0, null),
                Triple("0 /* 1 */"    , 3, null)
                // @formatter:on
        ).forEach {
            val input = it.first
            val startIndex = it.second
            val expected = it.third

            test("tokenize($input, $startIndex)") {
                val result = lexer.tokenize(input, startIndex)
                assertThat(result).isEqualTo(expected)
            }
        }
    }
})
