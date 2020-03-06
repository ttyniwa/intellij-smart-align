import org.jetbrains.kotlin.util.prefixIfNot
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.html.HtmlRenderer

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.vladsch.flexmark:flexmark:0.60.2")
    }
}

plugins {
    java
    kotlin("jvm") version "1.3.70"
    kotlin("kapt") version "1.3.70"
    id("org.jetbrains.intellij") version "0.4.16"
    id("com.github.breadmoirai.github-release") version "2.2.11"
}

group = "com.github.ttyniwa"
version = "1.0.0"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx/")
}

dependencies {
    val spekVersion = "2.0.9"

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.commons:commons-lang3:3.9")
    kapt("com.bennyhuo.kotlin:deepcopy-compiler:1.3.0-rc1")
    implementation("com.bennyhuo.kotlin:deepcopy-runtime:1.3.0-rc1")
    implementation("com.bennyhuo.kotlin:deepcopy-annotations:1.3.0-rc1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.assertj:assertj-core:3.15.0")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

fun readmeXmlAsHtml():String {
    val parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().build()
    var readmeContent = File(rootProject.uri("README.md")).readText()
    // since these images needs to shown from within intellij, lest put absolute urls so that the images & changelog will be visible
    readmeContent = readmeContent.replace("screen_shots/selected_text.gif", "https://raw.githubusercontent.com/ttyniwa/intellij-smart-align/$version/screen_shots/selected_text.gif")
    readmeContent = readmeContent.replace("screen_shots/around_cursor.gif", "https://raw.githubusercontent.com/ttyniwa/intellij-smart-align/$version/screen_shots/around_cursor.gif")
    readmeContent = readmeContent.replace("CHANGELOG.md", "https://github.com/ttyniwa/intellij-smart-align/blob/$version/CHANGELOG.md")
    val readmeDocument = parser.parse(readmeContent)
    return renderer.render(readmeDocument)
}

fun changeLogAsHtml():String {
    val parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().build()
    val changeLogDocument = parser.parse(File(rootProject.uri("CHANGELOG.md")).readText())
    return renderer.render(changeLogDocument)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    test {
        useJUnitPlatform {
            includeEngines("spek2")
        }
    }
    patchPluginXml {
        version(project.version)
        pluginDescription(readmeXmlAsHtml())
        changeNotes(changeLogAsHtml())
    }

    val githubToken: String = findProperty("githubToken") as String? ?: System.getenv("GITHUB_TOKEN") ?: ""
    githubRelease {
        token(githubToken)
        owner("ttyniwa")
        repo("intellij-smart-align")
        body {
            projectDir.resolve("CHANGELOG.md")
                    .readText()
                    .substringAfter("## ")
                    .substringBefore("## [")
                    .prefixIfNot("## ")
        }
        draft(false)
        prerelease(false)
        releaseAssets(buildDir.resolve("distributions").listFiles())
        overwrite(true)
        dryRun(false)
        tagName(project.version.toString())
        releaseName(project.version.toString())
    }
}

intellij {
    version = "2019.3.2"
    instrumentCode = true
    updateSinceUntilBuild = false
}
