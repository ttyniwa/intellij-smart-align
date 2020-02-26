package com.niwatty.intellij.plugin.align

import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek

/**
 * GOALS
 * + align code by any '=,:,+=,-=,*=,/=,%=,^=,|=,&=,<<=,>>=,>>>='
 * + ignore lines with statements for,while,do,if,else,switch
 * + ignore lines with keywords assert,abstract,class,goto,implements,interface,enum,package,private,public,protected,void,Void,return,super,synchronized,throw
 *
 * STEPS
 * + align 2 lines
 * + enforce 1 space after delimiter
 * + add recognition for multiple operators
 * + ignore keywords
 * + no delimiters found
 *
 * ON HOLD
 * - determine how to handle whitespace at beginning of the line
 */
object TokenAlignerSpec : Spek({

    group(".align(input)") {

        fun assertAlignEquals(input: String, lineNum: Int, expected: String) {
            val actual = TokenAligner.align(input, lineNum)
            assertThat(actual).isEqualTo(expected)
        }

        fun assertNotAlign(input: String, lineNum: Int) = assertAlignEquals(input, lineNum, input)

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

//            test("split") {
//                val str = """
//                        i+=j=11;
//                        hoge+=junk=11231;
//                        """.trimIndent()
//
//                val strings = str.splitDelimiter("=", "+=")
//                assertThat(strings).isEqualTo(listOf("i", "+=", "j", "=", "11"))
//            }

            test("only align line which has common token") {
                val input: String = """
                    let defaultParam = DiscoverListDC.create({
                      title      : title,
                    });
                """.trimIndent()
                val expected: String = """
                    let defaultParam = DiscoverListDC.create({
                      title: title,
                    });
                """.trimIndent()
                assertAlignEquals(input, 1, expected)
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
                      "HOME_HERO_AREA": "home__hero-area"                   ,
                      "PDP_HERO_AREA" : "[data-element-id='pdp__hero-area']",
                    }
                """.trimIndent()
                assertAlignEquals(input, 1, expected)
            }

            test("uses first index of delimiter for auto alignment") {
                val input: String = """
                    {
                    hello: '(something=cool())'
                    arnoldLovesCake: 'oh yeah'
                    }
                """.trimIndent()
                val expected: String = """
                    {
                    hello          : '(something=cool())'
                    arnoldLovesCake: 'oh yeah'
                    }
                """.trimIndent()

                assertAlignEquals(input, 1, expected)
            }
        }

        group("ignore lines") {

            test("start with keyword") {
                // FIXME 括弧の数が異なる場合は別の行グループとして判定する
                val input: String = """
                    for (i=0;i<1;++i) {
                    let joey=0;
                    }
                """.trimIndent()

                assertNotAlign(input, 0)
            }

            test("start with comment") {
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

            test("starts with css ampersand selector") {
                val input: String = """
                    .someclass {
                      &:not(.somethingElse) {
                      }
                    }
                """.trimIndent()
                assertNotAlign(input, 1)
            }

            test("contains '=>' function syntax") {
                val text: String = """
                      some.then((response) => {
                        let data = response.data;
                """.trimIndent()
                assertNotAlign(text, 0)
            }

            test("contains '->' function syntax") {
                val text: String = """
                      some.then((response) -> {
                        let data = response.data;
                """.trimIndent()
                assertNotAlign(text, 0)
            }

            test("end with delimiter") {
                val text: String = """
                    switch (true) {
                      case false:
                        return null;
                      default:
                        return null;
                    }
                """.trimIndent()
                assertNotAlign(text, 1)
            }

            test("ignore first delimiter in quote") {
                val input: String = """
                    mp('my : key', 'my = other').item = 'hello';
                    var mpItem = {
                      item1 : 'hi'
                """.trimIndent()
                val expected: String = """
                    mp('my : key', 'my = other').item = 'hello';
                    var mpItem                        = {
                      item1 : 'hi'
                """.trimIndent()

                assertAlignEquals(input, 0, expected)
            }

            test("ignore lines that have opened quotes") {
                val input: String = """
                    mp('my : key', 'my=other
                    var mpItem = {
                      item1: 'hi'
                """.trimIndent()
                val expected: String = """
                    mp('my : key', 'my=other
                    var mpItem = {
                      item1: 'hi'
                """.trimIndent()

                assertAlignEquals(input, 0, expected)
            }
        }
    }
})
