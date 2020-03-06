import org.jetbrains.kotlin.util.prefixIfNot

plugins {
    java
    kotlin("jvm") version "1.3.70"
    kotlin("kapt") version "1.3.70"
    id("org.jetbrains.intellij") version "0.4.16"
    id("com.github.breadmoirai.github-release") version "2.2.11"
}

group = "com.github.ttyniwa"
version = "1.0-SNAPSHOT"

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
        prerelease(true)
        releaseAssets(buildDir.resolve("distributions").listFiles())
        overwrite(true)
        dryRun(false)
    }
}

intellij {
    version = "2019.3.2"
    instrumentCode = true
    updateSinceUntilBuild = false
}
