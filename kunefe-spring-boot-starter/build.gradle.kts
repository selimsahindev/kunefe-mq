import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    id("com.vanniktech.maven.publish")
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

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("dev.selimsahin.kunefe", "kunefe-client", "0.1.0")

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
