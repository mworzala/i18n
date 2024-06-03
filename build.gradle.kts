plugins {
    `java-library`
    id("me.champeau.jmh") version "0.7.2"
}

group = "net.hollowcube"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnlyApi("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.17.0")

    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    testImplementation("net.kyori:adventure-api:4.17.0")
    testImplementation("net.kyori:adventure-text-minimessage:4.17.0")
    testImplementation("net.kyori:adventure-text-serializer-plain:4.17.0")
    testImplementation("net.kyori:adventure-text-serializer-legacy:4.17.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

jmh {
    excludes = listOf("NoArgsParse")
}
