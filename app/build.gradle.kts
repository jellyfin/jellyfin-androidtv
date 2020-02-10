plugins {
	id("com.android.application")
	id("kotlin-android")
	id("kotlin-android-extensions")
}

android {
	compileSdkVersion(29)

	defaultConfig {
		// Android version targets
		minSdkVersion(21)
		targetSdkVersion(29)

		// Release version
		versionCode = 905
		versionName = "0.11.0"
	}

	compileOptions {
		// Use Java 1.8 features
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}

	buildTypes {
		getByName("release") {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
			resValue("string", "app_name", "@string/app_name_release")
		}

		getByName("debug") {
			// Use different application id to run release and debug at the same time
			applicationIdSuffix = ".debug"
			resValue("string", "app_name", "@string/app_name_debug")
		}
	}
}

dependencies {
	// Jellyfin
	val jellyfinApiclientVersion= "master-SNAPSHOT"
	implementation("com.github.jellyfin.jellyfin-apiclient-java:android:$jellyfinApiclientVersion")
	implementation("com.github.jellyfin.jellyfin-apiclient-java:library:$jellyfinApiclientVersion")

	// Kotlin
	implementation(kotlin("stdlib-jdk8"))
	val kotlinCoroutinesVersion = "1.3.3"
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinCoroutinesVersion")

	// Android(x)
	implementation("androidx.core:core-ktx:1.2.0")
	implementation("androidx.leanback:leanback:1.0.0")
	implementation("androidx.appcompat:appcompat:1.1.0")
	implementation("androidx.tvprovider:tvprovider:1.0.0")
	implementation("androidx.palette:palette:1.0.0")
	implementation("androidx.constraintlayout:constraintlayout:1.1.3")
	implementation("androidx.recyclerview:recyclerview:1.1.0")

	// Media players
	implementation("com.amazon.android:exoplayer:2.10.6")
	implementation("org.videolan.android:libvlc-all:3.1.12")

	// Image utility
	implementation("com.squareup.picasso:picasso:2.3.2")
	implementation("com.github.bumptech.glide:glide:3.7.0")
	implementation("com.flaviofaria:kenburnsview:1.0.6")

	// Crash Reporting
	val acraVersion = "5.4.0"
	implementation("ch.acra:acra-http:$acraVersion")
	implementation("ch.acra:acra-dialog:$acraVersion")
	implementation("ch.acra:acra-limiter:$acraVersion")

	// Testing
	testImplementation("junit:junit:4.12")
	testImplementation("org.mockito:mockito-core:3.2.4")
}
