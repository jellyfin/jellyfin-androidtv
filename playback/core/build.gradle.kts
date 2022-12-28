plugins {
	id("com.android.library")
	kotlin("android")
	alias(libs.plugins.kotlin.serialization)
}

android {
	namespace = "org.jellyfin.playback.core"
	compileSdk = 33

	defaultConfig {
		minSdk = 21
		targetSdk = 33
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
	implementation(libs.kotlinx.coroutines.guava)
	implementation(libs.kotlinx.serialization.json)

	// Android(x)
	implementation(libs.androidx.core)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.bundles.androidx.lifecycle)
	implementation(libs.androidx.media2.session)

	// Dependency Injection
	implementation(libs.bundles.koin)

	// Logging
	implementation(libs.timber)

	// Testing
	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions)
	testImplementation(libs.mockk)
}
