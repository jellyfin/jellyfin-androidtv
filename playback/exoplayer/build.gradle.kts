plugins {
	id("com.android.library")
	kotlin("android")
}

android {
	namespace = "org.jellyfin.playback.exoplayer"
	compileSdk = 34

	defaultConfig {
		minSdk = 21
	}

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

	// AndroidX
	implementation(libs.androidx.core)

	// ExoPlayer
	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.jellyfin.androidx.media3.ffmpeg.decoder)

	// Logging
	implementation(libs.timber)

	// Testing
	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions)
	testImplementation(libs.mockk)
}
