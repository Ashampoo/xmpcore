import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform") version "2.2.0"
    id("com.android.library") version "8.9.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    id("com.asarkar.gradle.build-time-tracker") version "5.0.1"
    id("me.qoomon.git-versioning") version "6.4.4"
    id("com.goncalossilva.resources") version "0.10.1"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.vanniktech.maven.publish") version "0.34.0"
}

repositories {
    google()
    mavenCentral()
}

val productName = "Ashampoo XMP Core"

val xmlUtilVersion: String = "0.91.1"
val kotlinxIoVersion: String = "0.8.0"

description = productName
group = "com.ashampoo"
version = "0.0.0"

gitVersioning.apply {

    refs {
        /* Main branch contains the current dev version */
        branch("main") {
            version = "\${commit.short}"
        }
        /* Release / tags have real version numbers */
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }

    /* Fallback if branch was not found (for feature branches) */
    rev {
        version = "\${commit.short}"
    }
}

apply(plugin = "io.gitlab.arturbosch.detekt")

buildTimeTracker {
    sortBy.set(com.asarkar.gradle.buildtimetracker.Sort.DESC)
}

detekt {
    source.from("src", "build.gradle.kts")
    allRules = true
    config.setFrom("$projectDir/detekt.yml")
    parallel = true
    ignoreFailures = true
    autoCorrect = true
}

kover {
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
}

kotlin {

    explicitApi()

    androidTarget {

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        publishLibraryVariants("release")
    }

    mingwX64("win") {
        binaries {
            executable(setOf(NativeBuildType.RELEASE)) {
                entryPoint = "com.ashampoo.xmp.main"
            }
        }
    }

    linuxX64 {
        binaries {
            executable(setOf(NativeBuildType.RELEASE)) {
                entryPoint = "com.ashampoo.xmp.main"
            }
        }
    }

    linuxArm64 {
        binaries {
            executable(setOf(NativeBuildType.RELEASE)) {
                entryPoint = "com.ashampoo.xmp.main"
            }
        }
    }

    jvm {

        java {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }

    js()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs()

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi()

    @Suppress("UnusedPrivateMember") // False positive
    val commonMain by sourceSets.getting {

        dependencies {

            /* Needed to parse XML and create a DOM Document */
            implementation("io.github.pdvrieze.xmlutil:core:$xmlUtilVersion")
        }
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val commonTest by sourceSets.getting {
        dependencies {

            /* Kotlin Test */
            implementation(kotlin("test"))

            /* Multiplatform file access */
            implementation("org.jetbrains.kotlinx:kotlinx-io-core:$kotlinxIoVersion")
        }
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val jvmMain by sourceSets.getting

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val jvmTest by sourceSets.getting {
        dependencies {
            implementation(kotlin("test-junit"))
        }
    }

    val xcf = XCFramework()

    listOf(
        /* App Store */
        iosArm64(),
        /* Apple Intel iOS Simulator */
        iosX64(),
        /* Apple Silicon iOS Simulator */
        iosSimulatorArm64(),
        /* macOS Devices */
        macosX64(),
        macosArm64()
    ).forEach {

        it.binaries.executable(setOf(NativeBuildType.RELEASE)) {
            baseName = "xmpcore"
            entryPoint = "com.ashampoo.xmp.main"
        }

        it.binaries.framework(setOf(NativeBuildType.RELEASE)) {
            baseName = "xmpcore"
            /* Part of the XCFramework */
            xcf.add(this)
        }
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val androidMain by sourceSets.getting

    val posixMain by sourceSets.creating {
        dependsOn(commonMain)
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val winMain by sourceSets.getting {
        dependsOn(posixMain)
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val linuxX64Main by sourceSets.getting {
        dependsOn(posixMain)
    }

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val linuxArm64Main by sourceSets.getting {
        dependsOn(posixMain)
    }

    val iosArm64Main by sourceSets.getting
    val iosX64Main by sourceSets.getting
    val iosSimulatorArm64Main by sourceSets.getting
    val macosX64Main by sourceSets.getting
    val macosArm64Main by sourceSets.getting

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val appleMain by sourceSets.creating {

        dependsOn(commonMain)
        dependsOn(posixMain)

        iosArm64Main.dependsOn(this)
        iosX64Main.dependsOn(this)
        iosSimulatorArm64Main.dependsOn(this)
        macosX64Main.dependsOn(this)
        macosArm64Main.dependsOn(this)
    }

    val iosArm64Test by sourceSets.getting
    val iosX64Test by sourceSets.getting
    val iosSimulatorArm64Test by sourceSets.getting
    val macosX64Test by sourceSets.getting
    val macosArm64Test by sourceSets.getting

    @Suppress("UnusedPrivateMember", "UNUSED_VARIABLE") // False positive
    val appleTest by sourceSets.creating {

        dependsOn(commonTest)

        iosArm64Test.dependsOn(this)
        iosX64Test.dependsOn(this)
        iosSimulatorArm64Test.dependsOn(this)
        macosX64Test.dependsOn(this)
        macosArm64Test.dependsOn(this)
    }
}

// region Writing version.txt for GitHub Actions
val writeVersion = tasks.register("writeVersion") {
    doLast {
        File("build/version.txt").writeText(project.version.toString())
    }
}

tasks.getByPath("build").finalizedBy(writeVersion)
// endregion

// region Android setup
android {

    namespace = "com.ashampoo.xmpcore"

    compileSdk = 36

    sourceSets["main"].res.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}
// endregion

// region Maven publish

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

val signingEnabled: Boolean = System.getenv("SIGNING_ENABLED")?.toBoolean() ?: false

mavenPublishing {

    publishToMavenCentral()

    if (signingEnabled)
        signAllPublications()

    coordinates(
        groupId = "com.ashampoo",
        artifactId = "xmpcore",
        version = version.toString()
    )

    pom {

        name = productName
        description = "XMP Core for Kotlin Multiplatform"
        url = "https://github.com/Software-Rangers/xmpcore"

        licenses {
            license {
                name = "The BSD License"
                url = "https://github.com/Software-Rangers/xmpcore/blob/main/original_source/original_license.txt"
            }
        }

        developers {
            developer {
                name = "Software Rangers GmbH"
                url = "https://software-rangers.com/"
            }
        }

        scm {
            url = "https://github.com/Software-Rangers/xmpcore"
            connection = "scm:git:git://github.com/Software-Rangers/xmpcore.git"
        }
    }
}
// endregion
