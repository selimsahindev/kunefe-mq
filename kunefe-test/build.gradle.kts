plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.5")
    }
}

dependencies {
    implementation(project(":kunefe-broker"))
    implementation(project(":kunefe-client"))
    testImplementation(project(":kunefe-proto"))

    // Spring Boot Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // gRPC
    testImplementation("io.grpc:grpc-netty-shaded:1.64.0")
    testImplementation("io.grpc:grpc-protobuf:1.64.0")
    testImplementation("io.grpc:grpc-stub:1.64.0")

    // JUnit Platform
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        systemProperty("junit.platform.discovery.failOnNoTests", "false")
    }
}