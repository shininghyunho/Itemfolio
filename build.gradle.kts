plugins {
    kotlin("jvm") version "1.8.20"
    application
}

group = "chaegang"
version = "1.0"

repositories {
    mavenCentral()
    // papermc-ropo
    maven {
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
    // sonatype
    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    testImplementation(kotlin("test"))
}

tasks.processResources {
    filesMatching("**/*.yml") {
        expand(mapOf("version" to version))
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}