plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("maven-publish")
}

group = "com.zhzc0x.compose.chart"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
}