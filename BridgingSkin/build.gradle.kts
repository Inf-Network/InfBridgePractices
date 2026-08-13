import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar

group = "net.infnetwork.snowball"
version = "4-1.21.11"

repositories {
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly(project(":BridgingAnalyzer"))
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.19")
    implementation("org.postgresql:postgresql:42.7.4") {
        isTransitive = false
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.xerial:sqlite-jdbc:3.49.1.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.startsWith("postgresql-") }
            .map { zipTree(it) }
    })
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
}
