plugins {
	id(libs.plugins.android.library.get().pluginId)
	id(libs.plugins.kotlin.android.get().pluginId)
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
}

dependencies {
	
}
