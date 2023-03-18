import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import org.jetbrains.kotlin.util.prefixIfNot

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
    kotlin("jvm") version "1.7.0"
    kotlin("kapt") version "1.7.0"
    id("org.jetbrains.intellij") version "1.13.2"
    id("com.github.breadmoirai.github-release") version "2.2.11"
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

group = "com.github.ttyniwa"
version = "1.0.0"

repositories {
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlinx/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.commons:commons-lang3:3.12.0")
    ksp("com.bennyhuo.kotlin:deepcopy-compiler-ksp:1.7.10.0")
    implementation("com.bennyhuo.kotlin:deepcopy-runtime:1.7.10.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
}

fun readmeXmlAsHtml(): String {
    val parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().build()
    var readmeContent = File(rootProject.uri("README.md")).readText()
    // since these images needs to shown from within intellij, lest put absolute urls so that the images & changelog will be visible
    readmeContent = readmeContent.replace(
        "screen_shots/selected_text.gif",
        "https://raw.githubusercontent.com/ttyniwa/intellij-smart-align/$version/screen_shots/selected_text.gif"
    )
    readmeContent = readmeContent.replace(
        "screen_shots/around_cursor.gif",
        "https://raw.githubusercontent.com/ttyniwa/intellij-smart-align/$version/screen_shots/around_cursor.gif"
    )
    readmeContent = readmeContent.replace(
        "CHANGELOG.md",
        "https://github.com/ttyniwa/intellij-smart-align/blob/$version/CHANGELOG.md"
    )
    val readmeDocument = parser.parse(readmeContent)
    return renderer.render(readmeDocument)
}

fun changeLogAsHtml(): String {
    val parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().build()
    val changeLogDocument = parser.parse(File(rootProject.uri("CHANGELOG.md")).readText())
    return renderer.render(changeLogDocument)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    withType<Test>().configureEach {
        useJUnitPlatform()
    }
    patchPluginXml {
//        version.set(project.version)
        pluginDescription.set(readmeXmlAsHtml())
        changeNotes.set(changeLogAsHtml())
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
    version.set("2022.3.3")
    instrumentCode.set(true)
    updateSinceUntilBuild.set(false)
}
