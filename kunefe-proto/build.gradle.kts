plugins {
    id("com.google.protobuf") version "0.9.4"
    `maven-publish`
    signing
}

dependencies {
    implementation("io.grpc:grpc-protobuf:1.82.2")
    implementation("io.grpc:grpc-stub:1.82.2")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.82.2"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "kunefe-proto"

            pom {
                name.set("Kunefe Proto")
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
