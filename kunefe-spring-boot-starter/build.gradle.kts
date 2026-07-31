plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    `maven-publish`
    signing
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.5")
    }
}

dependencies {
    implementation(project(":kunefe-proto"))
    implementation(project(":kunefe-client"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // gRPC
    implementation("io.grpc:grpc-netty-shaded:1.82.2")
    implementation("io.grpc:grpc-protobuf:1.82.2")
    implementation("io.grpc:grpc-stub:1.82.2")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.13")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "kunefe-spring-boot-starter"

            pom {
                name.set("Kunefe Spring Boot Starter")
                description.set("Core Java client for Kunefe MQ — lightweight gRPC-based message broker")
                url.set("https://github.com/selimsahindev/kunefe-mq")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("selimsahindev")
                        name.set("Selim Şahin")
                        url.set("https://selimsahin.dev")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/selimsahindev/kunefe-mq.git")
                    developerConnection.set("scm:git:ssh://github.com/selimsahindev/kunefe-mq.git")
                    url.set("https://github.com/selimsahindev/kunefe-mq")
                }
            }
        }
    }

    repositories {
        maven {
            name = "SonatypeCentral"
            url = uri("https://central.sonatype.com/api/v1/publisher/upload")
            credentials {
                username = System.getenv("SONATYPE_USERNAME")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
}

signing {
    val gpgKey = System.getenv("MAVEN_GPG_PRIVATE_KEY")
    val gpgPassphrase = System.getenv("MAVEN_GPG_PASSPHRASE")
    if (gpgKey != null && gpgPassphrase != null) {
        useInMemoryPgpKeys(gpgKey, gpgPassphrase)
        sign(publishing.publications["mavenJava"])
    }
}
