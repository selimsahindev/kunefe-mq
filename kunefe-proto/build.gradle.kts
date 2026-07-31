import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.google.protobuf") version "0.9.4"
    id("com.vanniktech.maven.publish")
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

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("dev.selimsahin.kunefe", "kunefe-proto", project.version.toString())

    pom {
        name.set("Kunefe Proto")
        description.set("Protobuf contracts for Kunefe MQ — BrokerService, ProducerService, ConsumerService")
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
