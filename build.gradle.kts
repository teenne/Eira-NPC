plugins {
    id("java")
    id("idea")
    id("eclipse")
    id("maven-publish")
    id("net.neoforged.moddev") version "2.0.42-beta"
}

val modId = "storyteller"
val minecraftVersion = "1.21.4"
val neoForgeVersion = "21.4.75-beta"

version = "1.0.0"
group = "com.storyteller"

base {
    archivesName.set(modId)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases")
    }
}

dependencies {
    // Gson for JSON handling (LLM API communication)
    implementation("com.google.code.gson:gson:2.10.1")

    // OkHttp for async HTTP requests to LLM APIs
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Testing frameworks
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")

    // For mocking OkHttp responses
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    // For async testing
    testImplementation("org.awaitility:awaitility:4.2.0")
}

tasks.test {
    useJUnitPlatform()
}

neoForge {
    version = neoForgeVersion
    
    parchment {
        minecraftVersion = "1.21.4"
        mappingsVersion = "2024.12.07"
    }
    
    runs {
        create("client") {
            client()
            gameDirectory.set(file("run"))
            programArguments.addAll("--username", "Dev", "--uuid", "00000000-0000-0000-0000-000000000000")
        }
        
        create("server") {
            server()
            gameDirectory.set(file("run-server"))
            programArgument("--nogui")
        }
    }
    
    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Specification-Title" to modId,
            "Specification-Version" to project.version,
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = modId
        }
    }
}
