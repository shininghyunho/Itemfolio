plugins {
    kotlin("jvm") version "1.8.10"
    // shadowJar
    id("com.github.johnrengelman.shadow") version "7.0.0"
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
    implementation(kotlin("stdlib"))
    implementation("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    testImplementation(kotlin("test"))
}

tasks.processResources {
    // UTF-8
    filteringCharset = "UTF-8"
    filesMatching("**/*.yml") {
        expand(mapOf("version" to version))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// shadowJar
tasks.shadowJar {
    archiveBaseName.set("Itemfolio")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "MainKt"
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