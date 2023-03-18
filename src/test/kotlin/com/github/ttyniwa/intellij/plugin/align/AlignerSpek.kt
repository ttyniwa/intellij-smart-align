package com.github.ttyniwa.intellij.plugin.align

import com.github.ttyniwa.intellij.plugin.align.aligner.Aligner
import io.kotest.core.spec.style.DescribeSpec
import org.assertj.core.api.Assertions.assertThat

object AlignerSpek : DescribeSpec({

    describe(".align(input)") {

        fun assertAlignEquals(input: String, lineNum: Int, expected: String) {
            val actual = Aligner.align(input, lineNum)
            assertThat(actual).isEqualTo(expected)
        }

        fun assertNotAlign(input: String, lineNum: Int) = assertAlignEquals(input, lineNum, input)

        describe("detect lines to align") {

            // TODO pass this test
//            it("align only same bracket structure lines.") {
//                val input = """
//                    var index = 0;
//                    j = 1;
//                    for (i=1; i<100; i++) {
//                """.trimIndent()
//                val expected = """
//                    var index = 0;
//                    j         = 1;
//                    for (i=1; i<100; i++) {
//                """.trimIndent()
//
//                assertAlignEquals(input, 0, expected)
//            }

            it("align only continuous lines") {
                val input = """
                    var noalign          = 0;
                    
                    var align          = 0;
                    j      = 1;
                    
                    var noalign          = 0;
                """.trimIndent()
                val expected = """
                    var noalign          = 0;
                    
                    var align = 0;
                    j         = 1;
                    
                    var noalign          = 0;
                """.trimIndent()

                assertAlignEquals(input, 3, expected)
            }

            it("only align line which has common token") {
                val input: String = """
                    listOf(1,2,3)
                    let i = 1
                    let defaultParam=DiscoverListDC.create({
                      title:title,
                    });
                """.trimIndent()
                val expected: String = """
                    listOf(1,2,3)
                    let i            = 1
                    let defaultParam = DiscoverListDC.create({
                      title:title,
                    });
                """.trimIndent()
                assertAlignEquals(input, 1, expected)
            }

        }

        describe("can align") {
            it("two lines") {
                val input = """
                    var index = 0;
                    j = 1;
                """.trimIndent()
                val expected = """
                    var index = 0;
                    j         = 1;
                """.trimIndent()

                assertAlignEquals(input, 0, expected)
            }

            // TODO pass this test
//            it("assignment takes precedence over left hand comma") {
//                val input = """
//                    var index = 0;
//                    i, j = 1;
//                """.trimIndent()
//                val expected = """
//                    var index = 0;
//                    i, j      = 1;
//                """.trimIndent()
//
//                assertAlignEquals(input, 0, expected)
//            }

            it("don't align last comma.") {
                val input = """
                    listOf(
                        listOf(1,"hello",2),
                        listOf(1, "hello world", "good", "bye"),
                    )
                """.trimIndent()
                val expected = """
                    listOf(
                        listOf(1, "hello"      , 2),
                        listOf(1, "hello world", "good", "bye"),
                    )
                """.trimIndent()

                assertAlignEquals(input, 1, expected)
            }

            it("two white spaced lines") {
                val input = """
                    var index          = 0;
                    j      = 1;
                """.trimIndent()
                val expected = """
                    var index = 0;
                    j         = 1;
                """.trimIndent()

                assertAlignEquals(input, 1, expected)
            }

            it("no delimiters at specified line") {
                val input = """
                    var index          = 0;
                    j      = 1;
                    noop;
                """.trimIndent()

                assertNotAlign(input, 2)
            }

            it("enforces 1 space after delimiter") {
                val input = """
                    var index=0;
                    j=1;
                """.trimIndent()
                val expected = """
                    var index = 0;
                    j         = 1;
                """.trimIndent()

                assertAlignEquals(input, 0, expected)
            }

            it("align any assignment") {
                val inputTemplate = """
                    var index{{OPERATOR}}0;
                    j {{OPERATOR}}1;
                """.trimIndent()
                val expectedTemplate = """
                    var index {{OPERATOR}} 0;
                    j         {{OPERATOR}} 1;
                """.trimIndent()

                listOf("=", "+=", "-=", "*=", "/=").forEach { operator ->
                    val input = inputTemplate.replace("{{OPERATOR}}", operator)
                    val expected = expectedTemplate.replace("{{OPERATOR}}", operator)

                    assertAlignEquals(input, 0, expected)
                }
            }

            it("multiple delimiters in one line") {
                val input = """
                    var index = 123 += 123;
                    j = 1=1234;
                """.trimIndent()
                val expected = """
                    var index = 123 += 123;
                    j         = 1    = 1234;
                """.trimIndent()
                assertAlignEquals(input, 0, expected)
            }

            it("align colon") {
                val input: String = """
                    tagName: 'tile-hero'
                    Names: ['hero']
                """.trimIndent()
                val expected: String = """
                    tagName: 'tile-hero'
                    Names  : ['hero']
                """.trimIndent()

                assertAlignEquals(input, 0, expected)
            }

            it("json") {
                val input: String = """
                    {
                      "HOME_HERO_AREA" : "home__hero-area",
                      "PDP_HERO_AREA" : "[data-element-id='pdp__hero-area']",
                    }
                """.trimIndent()
                val expected: String = """
                    {
                      "HOME_HERO_AREA": "home__hero-area",
                      "PDP_HERO_AREA" : "[data-element-id='pdp__hero-area']",
                    }
                """.trimIndent()
                assertAlignEquals(input, 1, expected)
            }
        }

        describe("ignore lines") {

            it("don't have delimiters") {
                val input = "(0;i<1;++i){"
                assertNotAlign(input, 0)
            }

            it("contain with css state") {
                val input = """
                    .dev-summary.tile::hover .dev-cover-image {}
                    .live-summary.tile::hover .dev-cover-image {
                    }
                """.trimIndent()

                assertNotAlign(input, 0)
            }

            // FIXME
//            it("starts with css ampersand selector") {
//                val input: String = """
//                    .someclass {
//                      &:not(.somethingElse) {
//                      }
//                    }
//                """.trimIndent()
//                assertNotAlign(input, 1)
//            }

            it("contains '=>' function syntax") {
                val text: String = """
                      some.then((response)=>{
                        let data = response.data;
                """.trimIndent()
                assertNotAlign(text, 0)
            }

            it("contains '->' function syntax") {
                val text: String = """
                      some.then((response)->{
                        let data = response.data;
                """.trimIndent()
                assertNotAlign(text, 0)
            }

            it("end with delimiter") {
                val text: String = """
                    switch (true) {
                      case false:
                        return null;
                    }
                """.trimIndent()
                assertNotAlign(text, 1)
            }
        }

        describe("token in string literal") {
            it("ignore first colon in quote") {
                val input: String = """
                    mp('my,key', 'my,other')
                    mp('my,other', 'my,other')
                """.trimIndent()
                val expected: String = """
                    mp('my,key'  , 'my,other')
                    mp('my,other', 'my,other')
                """.trimIndent()

                assertAlignEquals(input, 0, expected)
            }

            it("ignore lines that have assignment in opened quotes") {
                val input: String = """
                    mp("my=other
                    var mpItem = {
                """.trimIndent()
                val expected: String = """
                    mp("my=other
                    var mpItem = {
                """.trimIndent()

                assertAlignEquals(input, 0, expected)
            }
        }

        describe("comment") {
            it("start with eol comment doesn't aligned") {
                val template: String = """
                    {{KEYWORD}}let i = 1;
                    let jj=0;
                    }
                """.trimIndent()

                listOf("//", "/*").forEach { keyword ->
                    val input = template.replace("{{KEYWORD}}", keyword)
                    assertNotAlign(input, 0)
                }
            }

            it("don't align comment only line") {
                val input = """
                    // @formatter:off
                    var index = 0; // comment1
                    j = 1000; // comment2
                    // @formatter:on
                """.trimIndent()
                val expected = """
                    // @formatter:off
                    var index = 0;    // comment1
                    j         = 1000; // comment2
                    // @formatter:on
                """.trimIndent()

                assertAlignEquals(input, 1, expected)
            }

            it("align comments in same column") {
                val input = """
                    var index = 0; // comment1
                    j = 1000; // comment2
                """.trimIndent()
                val expected = """
                    var index = 0;    // comment1
                    j         = 1000; // comment2
                """.trimIndent()

                assertAlignEquals(input, 0, expected)
            }

            it("align comments in same column") {
                val input = """
                    var index = 0; // comment1
                    j = 1000; // comment2
                    k = 10;
                """.trimIndent()
                val expected = """
                    var index = 0;    // comment1
                    j         = 1000; // comment2
                    k         = 10;
                """.trimIndent()

                assertAlignEquals(input, 1, expected)
            }
        }

        describe("align selected text") {
            it("align range is correct") {
                val input = """
                    noalign = 1;
                    var index = 0; // comment1
                    j = 1000; // comment2
                    
                    k = 10;
                    index = 10;
                    noalign = 1;
                """.trimIndent()
                val expected = """
                    noalign = 1;
                    var index = 0;    // comment1
                    j         = 1000; // comment2
                    
                    k         = 10;
                    index     = 10;
                    noalign = 1;
                """.trimIndent()

                val actual = Aligner.align(input, IntRange(1, 5))
                assertThat(actual).isEqualTo(expected)
            }

            it("don't align comment only line") {
                val input = """
                    noalign = 1;
                    var index = 0; // comment1
                    j = 1000; // comment2
                      // noalign
                    k = 10;
                    index = 10;
                    noalign = 1;
                """.trimIndent()
                val expected = """
                    noalign = 1;
                    var index = 0;    // comment1
                    j         = 1000; // comment2
                      // noalign
                    k         = 10;
                    index     = 10;
                    noalign = 1;
                """.trimIndent()

                val actual = Aligner.align(input, IntRange(1, 5))
                assertThat(actual).isEqualTo(expected)
            }


        }

        describe("first token") {
            it("comma first style") {
                val input = """
                    0
                        ,1
                        ,23,12
                        ,123,413,515
                """.trimIndent()
                val expected = """
                    0
                        , 1
                        , 23 , 12
                        , 123, 413, 515
                """.trimIndent()

                assertAlignEquals(input, 1, expected)
            }
        }
    }
})
