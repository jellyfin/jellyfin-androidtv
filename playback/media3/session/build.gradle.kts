plugins {
	id("com.android.library")
	kotlin("android")
}

android {
	namespace = "org.jellyfin.playback.media3.session"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
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

	// media3
	implementation(libs.androidx.media3.session)

	// Logging
	implementation(libs.timber)

	// Compatibility (desugaring)
	coreLibraryDesugaring(libs.android.desugar)

	// Testing
	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions)
	testImplementation(libs.mockk)
}
