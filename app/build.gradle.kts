import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
	id("com.android.application")
	kotlin("android")
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.aboutlibraries)
}

fun releaseTime(): String {
	val now = LocalDateTime.now()
	val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
	return now.format(formatter)
}

android {
	val projectVersion = "1150.18.8"
	namespace = "org.jellyfin.androidtv"
//		val selfAppId = namespace!!;
	val selfAppId = "com.fengymi.jellyfin.androidtv"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()

		// Release version
		applicationId = selfAppId
		versionName = project.getVersionName(projectVersion)
		versionCode = getVersionCode(versionName!!)
	}

	buildFeatures {
		buildConfig = true
		viewBinding = true
		compose = true
	}

	compileOptions {
		isCoreLibraryDesugaringEnabled = true
	}

	buildTypes {

		release {
			isMinifyEnabled = false

			// Set package names used in various XML files
			resValue("string", "app_id", selfAppId)
			resValue("string", "app_search_suggest_authority", "${selfAppId}.content")
			resValue("string", "app_search_suggest_intent_data", "content://${selfAppId}.content/intent")

			// Set flavored application name
			resValue("string", "app_name", "@string/app_name_release")

			resValue("string", "aliplayer_crt", "assets/cert/aliplayer.release.crt")

			buildConfigField("boolean", "DEVELOPMENT", "false")
		}

		debug {
			// Use different application id to run release and debug at the same time
			applicationIdSuffix = ".debug"

			// Set package names used in various XML files
			resValue("string", "app_id", selfAppId + applicationIdSuffix)
			resValue("string", "app_search_suggest_authority", "${selfAppId + applicationIdSuffix}.content")
			resValue("string", "app_search_suggest_intent_data", "content://${selfAppId + applicationIdSuffix}.content/intent")

			// Set flavored application name
			resValue("string", "app_name", "@string/app_name_debug")

			buildConfigField("boolean", "DEVELOPMENT", (defaultConfig.versionCode!! < 100).toString())
		}
	}

	lint {
		lintConfig = file("$rootDir/android-lint.xml")
		abortOnError = false
		sarifReport = true
		checkDependencies = true
	}

	testOptions.unitTests.all {
		it.useJUnitPlatform()
	}
}

base.archivesName.set("jellyfin-androidtv-v${project.getVersionName()}")

tasks.register("versionTxt") {
	val path = layout.buildDirectory.asFile.get().resolve("version.txt")

	doLast {
		val versionString = "v${android.defaultConfig.versionName}=${android.defaultConfig.versionCode}"
		logger.info("Writing [$versionString] to $path")
		path.writeText("$versionString\n")
	}
}

dependencies {
	// Jellyfin
	implementation(projects.playback.core)
	implementation(projects.playback.jellyfin)
	implementation(projects.playback.media3.exoplayer)
	implementation(projects.playback.media3.session)
	implementation(projects.preference)
	implementation(libs.jellyfin.sdk) {
		// Change version if desired
		val sdkVersion = findProperty("sdk.version")?.toString()
		when (sdkVersion) {
			"local" -> version { strictly("latest-SNAPSHOT") }
			"snapshot" -> version { strictly("master-SNAPSHOT") }
			"unstable-snapshot" -> version { strictly("openapi-unstable-SNAPSHOT") }
		}
	}

	// Kotlin
	implementation(libs.kotlinx.coroutines)
	implementation(libs.kotlinx.serialization.json)

	// Android(x)
	implementation(libs.androidx.core)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.fragment)
	implementation(libs.androidx.fragment.compose)
	implementation(libs.androidx.leanback.core)
	implementation(libs.androidx.leanback.preference)
	implementation(libs.androidx.preference)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.tvprovider)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.recyclerview)
	implementation(libs.androidx.work.runtime)
	implementation(libs.bundles.androidx.lifecycle)
	implementation(libs.androidx.window)
	implementation(libs.androidx.cardview)
	implementation(libs.androidx.startup)
	implementation(libs.bundles.androidx.compose)
	implementation(libs.accompanist.permissions)

	// Dependency Injection
	implementation(libs.bundles.koin)

	// Media players
	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.androidx.media3.datasource.okhttp)
	implementation(libs.androidx.media3.exoplayer.hls)
	implementation(libs.androidx.media3.ui)
	implementation(libs.jellyfin.androidx.media3.ffmpeg.decoder)

	// Markdown
	implementation(libs.bundles.markwon)

	// Image utility
	implementation(libs.bundles.coil)

	// Crash Reporting
	implementation(libs.bundles.acra)

	// Licenses
	implementation(libs.aboutlibraries)

	// Logging
	implementation(libs.timber)
	implementation(libs.slf4j.timber)

	// Compatibility (desugaring)
	coreLibraryDesugaring(libs.android.desugar)

	// bilibili 弹幕
	implementation(libs.danmaku.render.engine)
	implementation(libs.danmaku.render.engine.ndk.armv7a)
	implementation(libs.danmaku.render.engine.ndk.x86)
	implementation(libs.danmaku.render.engine.ndk.armv5)

	// json
	implementation(libs.alibaba.json)

	// Testing
	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions)
	testImplementation(libs.mockk)
}
