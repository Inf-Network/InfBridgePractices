import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    base
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/releases/")
        maven("https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "java")

    dependencies {
        add("compileOnly", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<ProcessResources>().configureEach {
        val pluginVersion = project.version.toString()
        inputs.property("pluginVersion", pluginVersion)
        filesMatching("plugin.yml") {
            expand("project" to mapOf("version" to pluginVersion))
        }
    }
}

tasks.named("build") {
    dependsOn(subprojects.map { "${it.path}:build" })
}
