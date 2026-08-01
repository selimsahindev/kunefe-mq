import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.google.protobuf") version "0.10.0"
    id("com.vanniktech.maven.publish")
}

dependencies {
    implementation(project(":kunefe-proto"))

    // gRPC
    implementation("io.grpc:grpc-netty-shaded:1.83.0")
    implementation("io.grpc:grpc-protobuf:1.83.0")
    implementation("io.grpc:grpc-stub:1.83.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.18")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("dev.selimsahin.kunefe", "kunefe-client", project.version.toString())

    pom {
        name.set("Kunefe Client")
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
