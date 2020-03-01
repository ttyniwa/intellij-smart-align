package com.github.ttyniwa.intellij.plugin.align

import com.github.ttyniwa.intellij.plugin.align.aligner.Aligner
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek

object AlignerSpek : Spek({

    group(".align(input)") {

        fun assertAlignEquals(input: String, lineNum: Int, expected: String) {
            val actual = Aligner.align(input, lineNum)
            assertThat(actual).isEqualTo(expected)
        }

        fun assertNotAlign(input: String, lineNum: Int) = assertAlignEquals(input, lineNum, input)

        group("detect lines to align") {

            // TODO pass this test
//            test("align only same bracket structure lines.") {
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

            test("align only continuous lines") {
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

            test("only align line which has common token") {
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

        group("can align") {
            test("two lines") {
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
//            test("assignment takes precedence over left hand comma") {
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

            test("don't align last comma.") {
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

            test("two white spaced lines") {
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

            test("no delimiters at specified line") {
                val input = """
                    var index          = 0;
                    j      = 1;
                    noop;
                """.trimIndent()

                assertNotAlign(input, 2)
            }

            test("enforces 1 space after delimiter") {
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

            test("align any assignment") {
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

            test("multiple delimiters in one line") {
                val input = """
                    var index = 123 += 123;
                    j = 1=1234;
                """.trimIndent()
                val expected = """
                    var index = 123 += 123;
                    j         = 1   = 1234;
                """.trimIndent()

                assertAlignEquals(input, 0, expected)
            }

            test("align colon") {
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

            test("json") {
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

        group("ignore lines") {

            test("don't have delimiters") {
                val input = "(0;i<1;++i){"
                assertNotAlign(input, 0)
            }

            test("contain with css state") {
                val input = """
                    .dev-summary.tile::hover .dev-cover-image {}
                    .live-summary.tile::hover .dev-cover-image {
                    }
                """.trimIndent()

                assertNotAlign(input, 0)
            }

            // FIXME
//            test("starts with css ampersand selector") {
//                val input: String = """
//                    .someclass {
//                      &:not(.somethingElse) {
//                      }
//                    }
//                """.trimIndent()
//                assertNotAlign(input, 1)
//            }

            test("contains '=>' function syntax") {
                val text: String = """
                      some.then((response)=>{
                        let data = response.data;
                """.trimIndent()
                assertNotAlign(text, 0)
            }

            test("contains '->' function syntax") {
                val text: String = """
                      some.then((response)->{
                        let data = response.data;
                """.trimIndent()
                assertNotAlign(text, 0)
            }

            test("end with delimiter") {
                val text: String = """
                    switch (true) {
                      case false:
                        return null;
                    }
                """.trimIndent()
                assertNotAlign(text, 1)
            }
        }

        group("token in string literal") {
            test("ignore first colon in quote") {
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

            test("ignore lines that have assignment in opened quotes") {
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

        group("comment") {
            test("start with one line comment doesn't aligned") {
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

            test("align comments in same column") {
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

            test("align comments in same column") {
                val input = """
                    listOf(
                        "hello", "world", // comment
                        "hello world", "yes", // comment
                        "good bye", // comment
                        "bye"     // comment
                    )
                """.trimIndent()
                val expected = """
                    listOf(
                        "hello"      , "world", // comment
                        "hello world", "yes",   // comment
                        "good bye",             // comment
                        "bye"                   // comment
                    )
                """.trimIndent()

                assertAlignEquals(input, 1, expected)
            }
        }
    }
})
