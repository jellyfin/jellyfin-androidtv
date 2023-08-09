plugins {
	id("com.android.library")
	kotlin("android")
}

android {
	namespace = "org.jellyfin.preference"
	compileSdk = 34

	defaultConfig {
		minSdk = 21
	}

	buildFeatures {
		viewBinding = true
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
	// Kotlin
	implementation(libs.kotlinx.coroutines)

	// Logging
	implementation(libs.timber)

	// Testing
	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions)
	testImplementation(libs.mockk)
}
