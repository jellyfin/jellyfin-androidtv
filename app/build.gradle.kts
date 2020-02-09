plugins {
	id("com.android.application")
	id("kotlin-android")
	id("kotlin-android-extensions")
}

val version = "0.11.0"

android {
	compileSdkVersion(28)
	buildToolsVersion = "28.0.3"

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}

	defaultConfig {
		minSdkVersion(21)
		targetSdkVersion(28)
		versionCode = 905
		versionName = version
	}

	buildTypes {
		getByName("release") {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
		}

		getByName("debug") {
			applicationIdSuffix = ".debug"
			isDebuggable = true
		}
	}
}

dependencies {
	// This is split like this because this is a modular gradle project on jitpack
	// see https://github.com/jitpack/gradle-modular
	// The split allows dependency substitution to work in the settings.gradle file,
	// at least for the 'library' part. 'android' part is still broken somehow, but this
	// is better than nothing.
	implementation("com.github.jellyfin.jellyfin-apiclient-java:android:master-SNAPSHOT")
	implementation("com.github.jellyfin.jellyfin-apiclient-java:library:master-SNAPSHOT")
	implementation("com.amazon.android:exoplayer:2.10.6")
	implementation("androidx.recyclerview:recyclerview:1.0.0")
	implementation("androidx.leanback:leanback:1.0.0")
	implementation("androidx.appcompat:appcompat:1.0.2")
	implementation("androidx.palette:palette:1.0.0")
	implementation("com.squareup.picasso:picasso:2.3.2")
	implementation("com.github.bumptech.glide:glide:3.7.0")
	implementation("com.squareup.okhttp:okhttp:2.7.0")
	implementation("com.squareup.okhttp:okhttp-urlconnection:2.7.0")
	implementation("com.google.guava:guava:18.0")
	implementation("com.flaviofaria:kenburnsview:1.0.6")
	implementation("org.videolan.android:libvlc-all:3.1.12")
	implementation("ch.acra:acra-http:5.4.0")
	implementation("ch.acra:acra-dialog:5.4.0")
	implementation("ch.acra:acra-limiter:5.4.0")
	testImplementation("junit:junit:4.12")
	implementation("androidx.core:core-ktx:1.1.0")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.61")
	implementation("androidx.constraintlayout:constraintlayout:1.1.3")
	implementation("androidx.tvprovider:tvprovider:1.0.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.2")
}
