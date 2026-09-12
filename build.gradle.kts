plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "world.elyona"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    compileOnly(files("libs/ElyonaCore.jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
    reobfJar {
        inputJar.set(shadowJar.flatMap { it.archiveFile })
    }
    shadowJar {
        archiveClassifier.set("shadow")
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
