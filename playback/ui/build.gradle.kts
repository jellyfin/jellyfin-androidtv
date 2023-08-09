plugins {
	id("com.android.library")
	kotlin("android")
}

android {
	namespace = "org.jellyfin.playback.ui"
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
	// Jellyfin
	implementation(projects.playback.core)
	implementation(projects.playback.jellyfin)
	implementation(libs.jellyfin.sdk) {
		// Change version if desired
		val sdkVersion = findProperty("sdk.version")?.toString()
		when (sdkVersion) {
			"local" -> version { strictly("latest-SNAPSHOT") }
			"snapshot" -> version { strictly("master-SNAPSHOT") }
			"unstable-snapshot" -> version { strictly("openapi-unstable-SNAPSHOT") }
		}
	}

	// Android(x)
	implementation(libs.androidx.core)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.bundles.androidx.lifecycle)
	implementation(libs.androidx.media3.session)

	// Dependency Injection
	implementation(libs.bundles.koin)

	// Logging
	implementation(libs.timber)

	// Testing
	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions)
	testImplementation(libs.mockk)
}
