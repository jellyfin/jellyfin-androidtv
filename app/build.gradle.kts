plugins {
	id("com.android.application")
	kotlin("android")
	kotlin("android.extensions")
}

android {
	compileSdkVersion(29)
	// Explicitly specify ndk version for Azure
	// Can be removed when version 4.1.x of the Android Gradle plugin is released
	ndkVersion = "21.3.6528147"

	defaultConfig {
		// Android version targets
		minSdkVersion(21)
		targetSdkVersion(29)

		// Release version
		versionName = project.getVersionName()
		versionCode = getVersionCode(versionName)
	}

	compileOptions {
		// Use Java 1.8 features
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}

	kotlinOptions {
		jvmTarget = compileOptions.targetCompatibility.toString()
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

	applicationVariants.all {
		val variant = this
		variant.outputs.all {
			val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
			output.outputFileName = output.outputFileName
				.replace("app-", "jellyfin-androidtv_")
				.replace(".apk", "_v${variant.versionName}.apk")
		}
	}
}

dependencies {
	// Jellyfin
	implementation("org.jellyfin.apiclient:android:0.7.4")

	// Kotlin
	implementation(kotlin("stdlib-jdk8"))

	val kotlinCoroutinesVersion = "1.3.3"
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinCoroutinesVersion")

	// Android(x)
	implementation("androidx.core:core-ktx:1.3.1")
	implementation("androidx.fragment:fragment-ktx:1.2.5")
	val androidxLeanbackVersion = "1.1.0-alpha03"
	implementation("androidx.leanback:leanback:$androidxLeanbackVersion")
	implementation("androidx.leanback:leanback-preference:$androidxLeanbackVersion")
	val androidxPreferenceVersion = "1.1.1"
	implementation("androidx.preference:preference:$androidxPreferenceVersion")
	implementation("androidx.preference:preference-ktx:$androidxPreferenceVersion")
	implementation("androidx.appcompat:appcompat:1.2.0")
	implementation("androidx.tvprovider:tvprovider:1.0.0")
	implementation("androidx.constraintlayout:constraintlayout:1.1.3")
	implementation("androidx.recyclerview:recyclerview:1.1.0")
	implementation("com.google.android:flexbox:2.0.1")

	// Dependency Injection
	val koinVersion = "2.1.6"
	implementation("org.koin:koin-android-viewmodel:$koinVersion")

	// Media players
	implementation("com.amazon.android:exoplayer:2.11.3")
	implementation("org.videolan.android:libvlc-all:3.2.5")

	// Image utility
	implementation("com.github.bumptech.glide:glide:4.11.0")
	implementation("com.flaviofaria:kenburnsview:1.0.6")

	// Crash Reporting
	val acraVersion = "5.4.0"
	implementation("ch.acra:acra-http:$acraVersion")
	implementation("ch.acra:acra-dialog:$acraVersion")
	implementation("ch.acra:acra-limiter:$acraVersion")

	// Logging
	implementation("com.jakewharton.timber:timber:4.7.1")

	// Testing
	testImplementation("junit:junit:4.12")
	testImplementation("org.mockito:mockito-core:3.2.4")
}
