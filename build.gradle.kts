import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.0.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
    }
}

allprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += "-Xopt-in=" +
                "kotlin.ExperimentalStdlibApi," +
                "kotlinx.coroutines.ExperimentalCoroutinesApi"
    }

    repositories {
        google()
        mavenCentral()
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}