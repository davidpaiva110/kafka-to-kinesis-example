plugins {
    id("scala")
    id("idea")
    application
}

group = "david.code"
version = "1.0-SNAPSHOT"


scala {
    scalaVersion = libs.versions.scala.get()
}

repositories {
    mavenCentral()
}

val pekkoVersion = "1.2.0"
val pekkoConnectorsKafkaVersion = "1.0.0"
val circeVersion = "0.14.6"
val ironVersion = "2.4.0"

dependencies {
    // Scala
    implementation("org.scala-lang:scala3-library_3:3.3.1")

    // Pekko
    implementation("org.apache.pekko:pekko-stream_3:$pekkoVersion")
    implementation("org.apache.pekko:pekko-actor-typed_3:$pekkoVersion")
    implementation("org.apache.pekko:pekko-slf4j_3:$pekkoVersion")

    // Pekko Kafka Connector
    implementation(dependencyNotation = "org.apache.pekko:pekko-connectors-kafka_3:$pekkoConnectorsKafkaVersion")

    // JSON with Circe
    implementation("io.circe:circe-core_3:$circeVersion")
    implementation("io.circe:circe-generic_3:$circeVersion")
    implementation("io.circe:circe-parser_3:$circeVersion")

    // Iron for refined types and Either
    implementation("io.github.iltotore:iron_3:$ironVersion")
    implementation("io.github.iltotore:iron-circe_3:$ironVersion")

    // Config
    implementation("com.typesafe:config:1.4.3")

    // AWS SDK for Kinesis
    implementation("software.amazon.awssdk:kinesis:2.21.26")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation("org.scalatest:scalatest_3:3.2.17")
}


application {
    mainClass.set("Main")
}

tasks.test {
    useJUnitPlatform()
}

// Custom task to copy distribution to a specific directory with separated layers
tasks.register<Copy>("publishApp") {
    dependsOn("installDist")

    // Copy all libraries except the main app jar to out/docker/lib
    from("build/install/${rootProject.name}/lib") {
        exclude("${rootProject.name}.jar")
        into("lib")
    }

    // Copy only the main app jar to out/docker/app and rename it to application.jar
    from("build/install/${rootProject.name}/lib") {
        include("${rootProject.name}.jar")
        rename("${rootProject.name}.jar", "application.jar")
        into("app")
    }

    into("out/docker")

    doLast {
        println("Libraries published to out/docker/lib/ and application to out/docker/app/application.jar")
    }
}

tasks.clean {
    delete("out/docker")
}

repositories {
    mavenCentral()
}
