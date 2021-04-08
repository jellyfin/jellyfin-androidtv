plugins {
	id("com.android.application")
	kotlin("android")
	kotlin("plugin.serialization")
}

android {
	compileSdkVersion(30)

	defaultConfig {
		// Android version targets
		minSdkVersion(21)
		targetSdkVersion(30)

		// Release version
		versionName = project.getVersionName()
		versionCode = getVersionCode(versionName!!)
	}

	buildFeatures {
		viewBinding = true
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

			// Add applicationId as string for XML resources
			resValue("string", "app_id", "org.jellyfin.androidtv")

			// Set flavored application name
			resValue("string", "app_name", "@string/app_name_release")
		}

		getByName("debug") {
			// Use different application id to run release and debug at the same time
			applicationIdSuffix = ".debug"

			// Add applicationId as string for XML resources
			resValue("string", "app_id", "org.jellyfin.androidtv.debug")

			// Set flavored application name
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
	implementation("org.jellyfin.apiclient:android:0.7.9")

	// Kotlin
	val kotlinxCoroutinesVersion = "1.4.3"
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxCoroutinesVersion")

	val kotlinxSerializationVersion = "1.1.0"
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

	// Android(x)
	implementation("androidx.core:core-ktx:1.3.2")
	implementation("androidx.fragment:fragment-ktx:1.3.2")
	val androidxLeanbackVersion = "1.1.0-rc01"
	implementation("androidx.leanback:leanback:${androidxLeanbackVersion}")
	implementation("androidx.leanback:leanback-preference:${androidxLeanbackVersion}")
	val androidxPreferenceVersion = "1.1.1"
	implementation("androidx.preference:preference:$androidxPreferenceVersion")
	implementation("androidx.preference:preference-ktx:$androidxPreferenceVersion")
	implementation("androidx.appcompat:appcompat:1.2.0")
	implementation("androidx.tvprovider:tvprovider:1.1.0-alpha01")
	implementation("androidx.constraintlayout:constraintlayout:2.0.4")
	implementation("androidx.recyclerview:recyclerview:1.1.0")
	implementation("androidx.work:work-runtime-ktx:2.5.0")
	val androidxLifecycleVersion = "2.3.1"
	implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$androidxLifecycleVersion")
	implementation("androidx.lifecycle:lifecycle-livedata-ktx:$androidxLifecycleVersion")
	implementation("androidx.window:window:1.0.0-alpha05")
	implementation("androidx.viewpager:viewpager:1.0.0")

	// Dependency Injection
	val koinVersion = "2.2.0"
	implementation("org.koin:koin-android:$koinVersion")
	implementation("org.koin:koin-androidx-viewmodel:$koinVersion")
	implementation("org.koin:koin-androidx-fragment:$koinVersion")
	implementation("org.koin:koin-androidx-workmanager:$koinVersion")

	// GSON
	implementation("com.google.code.gson:gson:2.8.6")

	// Media players
	implementation("com.amazon.android:exoplayer:2.11.3")
	implementation("org.videolan.android:libvlc-all:3.3.14")

	// Image utility
	implementation("com.github.bumptech.glide:glide:4.12.0")
	implementation("com.flaviofaria:kenburnsview:1.0.7")

	// Crash Reporting
	val acraVersion = "5.7.0"
	implementation("ch.acra:acra-http:$acraVersion")
	implementation("ch.acra:acra-dialog:$acraVersion")
	implementation("ch.acra:acra-limiter:$acraVersion")

	// Logging
	implementation("com.jakewharton.timber:timber:4.7.1")

	// Debugging
	if (getProperty("leakcanary.enable")?.toBoolean() == true)
		debugImplementation("com.squareup.leakcanary:leakcanary-android:2.6")

	// Testing
	testImplementation("junit:junit:4.13.1")
	testImplementation("org.mockito:mockito-core:3.2.4")
}
