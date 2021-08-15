import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

fun RepositoryHandler.defaultRepositories() {
	mavenCentral()
	google()

	// Jellyfin SDK
	mavenLocal {
		content {
			includeVersionByRegex("org.jellyfin.sdk", ".*", "latest-SNAPSHOT")
		}
	}
	maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
		content {
			includeVersionByRegex("org.jellyfin.sdk", ".*", "master-SNAPSHOT")
			includeVersionByRegex("org.jellyfin.sdk", ".*", "openapi-unstable-SNAPSHOT")
		}
	}

	// Jellyfin apiclient
	maven("https://jitpack.io") {
		content {
			// Only allow legacy apiclient
			includeVersionByRegex("com.github.jellyfin.jellyfin-sdk-kotlin", ".*", "v0.7.10")
		}
	}
}

object Plugins {
	object Versions {
		const val kotlin = "1.5.10"
		const val detekt = "1.17.1"
		const val androidBuildTools = "7.0.0"
	}

	const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
	const val androidBuildTools = "com.android.tools.build:gradle:${Versions.androidBuildTools}"
}
