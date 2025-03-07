enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "jellyfin-androidtv"

// Application
include(":app")

// Modules
include(":playback:core")
include(":playback:jellyfin")
include(":playback:media3:exoplayer")
include(":playback:media3:session")
include(":preference")

pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		google()
	}
}

dependencyResolutionManagement {
	repositories {
		mavenLocal()
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

		maven("https://maven.aliyun.com/repository/public") {
//			content {
//				includeVersionByRegex("com.github.CarGuo", ".*", ".*")
//			}
		}
		maven("https://jitpack.io") {
			content {
				includeVersionByRegex("com.github.fengymi.*", ".*", ".*")
			}
		}
	}
}
