// 参考: https://github.com/kropp/intellij-makefile/blob/master/build.gradle.kts
plugins {
    java
    kotlin("jvm") version "1.3.61"
    kotlin("kapt") version "1.3.61"
    id("org.jetbrains.intellij") version "0.4.16"
}

group = "com.niwatty"
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

    testCompile("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testCompile("org.assertj:assertj-core:3.15.0")
    testCompile("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect")
}

tasks.test {
    useJUnitPlatform {
        includeEngines("spek2")
    }
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
}

intellij {
    version = "2019.3.2"
    instrumentCode = true
    updateSinceUntilBuild = false
}
tasks.patchPluginXml {
    changeNotes("""
      this is change note.<BR>hoge""")
}