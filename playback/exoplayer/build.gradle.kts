plugins {
	id("com.android.library")
	kotlin("android")
}

android {
	compileSdk = 33

	defaultConfig {
		minSdk = 21
		targetSdk = 33
	}

	sourceSets["main"].java.srcDirs("src/main/kotlin")
	sourceSets["test"].java.srcDirs("src/test/kotlin")

	lint {
		lintConfig = file("$rootDir/android-lint.xml")
		abortOnError = false
	}

	testOptions.unitTests.all {
		it.useJUnitPlatform()
	}
}

dependencies {
	// Jellyfin
	implementation(projects.playback.core)

	// Kotlin
	implementation(libs.kotlinx.coroutines)
	implementation(libs.kotlinx.coroutines.guava)

	// ExoPlayer
	implementation(libs.exoplayer)

	// Logging
	implementation(libs.timber)

	// Testing
	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions)
	testImplementation(libs.mockk)
}
