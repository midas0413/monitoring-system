plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":monitoring-common"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web") // Aligo REST API 호출용
    implementation("org.springframework.boot:spring-boot-starter-actuator") // Docker healthcheck용
    implementation("org.springframework.boot:spring-boot-starter-mail") // Gmail SMTP

    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.oracle.database.jdbc:ojdbc11")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.springframework:spring-jdbc")

    implementation("com.hierynomus:sshj:0.39.0")

    implementation("org.apache.sshd:sshd-core:2.12.1")
    implementation("org.apache.sshd:sshd-common:2.12.1")

    // (선택) 키 포맷/암호화 처리 폭 넓게 하려면 추천
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
}

tasks.bootJar {
    enabled = true
}
tasks.jar {
    enabled = false
}


