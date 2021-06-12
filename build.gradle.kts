buildscript {
	repositories {
		mavenCentral()
		google()
	}

	dependencies {
		val kotlinVersion = getProperty("kotlin.version")
		classpath("com.android.tools.build:gradle:4.2.1")
		classpath(kotlin("gradle-plugin", kotlinVersion))
		classpath(kotlin("serialization", kotlinVersion))
	}
}

allprojects {
	// Dependencies
	repositories {
		mavenCentral()
		google()
		maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
			content {
				// Only allow SDK snapshots
				includeVersionByRegex("org\\.jellyfin\\.sdk", ".*", ".*-SNAPSHOT")
			}
		}
		maven("https://jitpack.io") {
			content {
				// Only allow legacy apiclient
				includeVersionByRegex("com.github.jellyfin.jellyfin-sdk-kotlin", ".*", "v0.7.10")
			}
		}
	}
}

plugins {
	id("io.gitlab.arturbosch.detekt").version("1.17.1")
}

// Detekt configuration
subprojects {
	plugins.apply("io.gitlab.arturbosch.detekt")

	detekt {
		buildUponDefaultConfig = true
		ignoreFailures = true
		config = files("$rootDir/detekt.yml")
		basePath = rootDir.absolutePath

		reports {
			sarif.enabled = true
		}
	}
}
