plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.0")
    }
}

dependencies {
    implementation(project(":kunefe-broker"))
    implementation(project(":kunefe-client"))
    implementation(project(":kunefe-proto"))

    // gRPC
    implementation("io.grpc:grpc-netty-shaded:1.83.0")
    implementation("io.grpc:grpc-protobuf:1.83.0")
    implementation("io.grpc:grpc-stub:1.83.0")

    // JMH
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    // Spring Boot Test
    implementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.register<JavaExec>("benchmark") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    args = listOf(".*Benchmark.*", "-rf", "json", "-rff", "benchmark-results.json")
}
