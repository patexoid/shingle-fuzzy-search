
plugins {
    `java-library`
    `maven-publish`
    id("com.palantir.git-version") version "0.15.0"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    api("com.google.guava:guava:29.0-jre")
    api("org.slf4j:slf4j-api:1.7.30")
    api("org.yaml:snakeyaml:1.26")
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.apache.commons:commons-text:1.4")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
}

val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val details = versionDetails()

group = "com.patex"
version =
    if (details.commitDistance == 0) details.lastTag else (details.lastTag + "-" + details.commitDistance + "-" + details.lastTag)
description = "fuzzysearch"
java.sourceCompatibility = JavaVersion.VERSION_11

println(version)
java {
    withSourcesJar()
    withJavadocJar()
}
if(details.commitDistance==0) {
    publishing {
        repositories {
            maven {
                name = "github"
                url = uri("https://maven.pkg.github.com/patexoid/repo")
                credentials {
                    username = System.getenv("USERNAME")
                    password = System.getenv("TOKEN")
                }
            }
        }
        publications.create<MavenPublication>("github") {
            from(components["java"])
        }
    }
}
tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}
