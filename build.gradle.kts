plugins {
    java
    id("org.springframework.boot") version "3.4.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "dev.selimsahin.kunefe"
    version = project.findProperty("releaseVersion")?.toString()
        ?: "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        withJavadocJar()
        withSourcesJar()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
