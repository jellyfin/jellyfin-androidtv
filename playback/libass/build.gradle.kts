plugins {
	alias(libs.plugins.android.library)
}

android {
	namespace = "org.jellyfin.playback.libass"
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
	implementation(libs.jellyfin.sdk)

	// Kotlin
	implementation(libs.kotlinx.coroutines)

	// Media3
	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.androidx.media3.ui)
	implementation(libs.libass.kt)
	implementation(libs.libass.media3)

	// Logging
	implementation(libs.timber)

	// Compatibility (desugaring)
	coreLibraryDesugaring(libs.android.desugar)

	// Testing
	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions)
}
