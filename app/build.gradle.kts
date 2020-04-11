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
		versionCode = 908
		versionName = "0.11.3"
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
		}

		getByName("debug") {
			// Use different application id to run release and debug at the same time
			applicationIdSuffix = ".debug"
		}
	}
}

dependencies {
	// Jellyfin
	val jellyfinApiclientVersion= "v0.6.0"
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
	implementation("com.amazon.android:exoplayer:2.11.3")
	implementation("org.videolan.android:libvlc-all:3.2.5")

	// Image utility
	implementation("com.squareup.picasso:picasso:2.3.2")
	implementation("com.github.bumptech.glide:glide:3.7.0")
	implementation("com.flaviofaria:kenburnsview:1.0.6")

	// HTTP utility
	// NOTE: This is used by Picasso through reflection and can cause weird caching issues if removed!
	val okhttpVersion = "2.7.0"
	implementation("com.squareup.okhttp:okhttp:$okhttpVersion")
	implementation("com.squareup.okhttp:okhttp-urlconnection:$okhttpVersion")

	// Crash Reporting
	val acraVersion = "5.4.0"
	implementation("ch.acra:acra-http:$acraVersion")
	implementation("ch.acra:acra-dialog:$acraVersion")
	implementation("ch.acra:acra-limiter:$acraVersion")

	// Testing
	testImplementation("junit:junit:4.12")
	testImplementation("org.mockito:mockito-core:3.2.4")
}
