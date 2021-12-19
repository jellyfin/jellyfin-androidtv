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
		isAbortOnError = false
	}
}

dependencies {
	
}
