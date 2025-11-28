plugins {
    id("java")
    antlr
}

group = "de.flo-gehring"
version = "1.0-SNAPSHOT"

val generatedAntlrDir = layout.buildDirectory.dir("generated-src/antlr/main/")

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.13.1")
    implementation("org.antlr:antlr4-runtime:4.13.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.11.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

// Modern Gradle: configure ANTLR task using named<GenerateGrammarSource>
tasks.named<AntlrTask>("generateGrammarSource") {
    outputDirectory = generatedAntlrDir.get().asFile
    maxHeapSize = "64m"

    arguments.addAll(
        listOf(
            "-visitor",
            "-long-messages"
        )
    )
}

sourceSets.named("main") {
    java.srcDir(generatedAntlrDir)
}

tasks.named("compileJava") {
    dependsOn("generateGrammarSource")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging { events("passed") }
}
