plugins {
	id("com.android.library")
	kotlin("android")
}

android {
	compileSdk = 31

	defaultConfig {
		minSdk = 21
		targetSdk = 31
	}

	buildFeatures {
		viewBinding = true
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
	compileOnly(libs.jellyfin.sdk)

	// Kotlin
	implementation(libs.kotlinx.coroutines)
	implementation(libs.kotlinx.coroutines.guava)
	implementation(libs.kotlinx.serialization.json)

	// Android(x)
	implementation(libs.androidx.core)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.bundles.androidx.lifecycle)

	// Dependency Injection
	implementation(libs.bundles.koin)

	// Media
	implementation(libs.exoplayer)
	implementation(libs.androidx.media2.session)

	// Logging
	implementation(libs.timber)

	// Testing
	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions)
	testImplementation(libs.mockk)
}
