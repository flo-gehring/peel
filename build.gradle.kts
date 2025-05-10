plugins {
    id("java")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}
group = "de.flo-gehring"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}




dependencies {
    implementation(project(":jetpackparser"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.11.1")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed")
    }
}