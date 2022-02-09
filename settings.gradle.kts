enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Application
include(":app")

// Modules
include(":playback")

pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		google()
	}
}

dependencyResolutionManagement {
	repositories {
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
}
