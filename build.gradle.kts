plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.0.0"
}

group = "com.yuunus90"
version = "0.1.0"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")

    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        instrumentationTools()
    }
    
    // Kotlin stdlib'i kaldırıyoruz çünkü IntelliJ Platform otomatik olarak sağlıyor
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib") {
            version {
                strictly("1.9.22")
            }
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib-common") {
            version {
                strictly("1.9.22")
            }
        }
    }
}

// Configure Gradle IntelliJ Platform Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-getting-started.html
intellijPlatform {
    pluginConfiguration {
        name.set("Line Comment")
        vendor {
            name.set("yuunus90")
        }
        description.set("Add comments to specific code lines with icons")
    }
    
    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }
    
    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
    
    patchPluginXml {
        sinceBuild.set("251")
        untilBuild.set("251.*")
    }
    
    buildSearchableOptions {
        enabled = false
    }
}

// All other configurations like patchPluginXml, runIde, etc. that were here are implicitly configured by the plugin now.
// We only need to override them if we need specific settings. 