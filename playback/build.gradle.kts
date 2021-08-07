plugins {
	id("com.android.library")
	kotlin("android")
}

android {
	compileSdkVersion(30)

	defaultConfig {
		minSdkVersion(21)
		targetSdkVersion(30)
	}

	buildFeatures {
		viewBinding = true
	}

	sourceSets["main"].java.srcDirs("src/main/kotlin")
	sourceSets["test"].java.srcDirs("src/test/kotlin")
}

dependencies {
	
}
