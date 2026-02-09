import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("org.springframework.boot") version "3.2.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

allprojects {
    group = "com.example"
    version = "0.1.0-SNAPSHOT"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }
}