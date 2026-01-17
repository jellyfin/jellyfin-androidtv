plugins {
	id("com.android.library")
	kotlin("android")
}

android {
	namespace = "org.jellyfin.design"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
	}

	lint {
		lintConfig = file("$rootDir/android-lint.xml")
		abortOnError = false
	}
}

dependencies {
	implementation(libs.androidx.compose.ui.graphics)
}
