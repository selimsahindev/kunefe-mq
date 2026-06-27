plugins {
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    implementation(project(":kunefe-proto"))

    // gRPC
    implementation("io.grpc:grpc-netty-shaded:1.64.0")
    implementation("io.grpc:grpc-protobuf:1.64.0")
    implementation("io.grpc:grpc-stub:1.64.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.13")
}