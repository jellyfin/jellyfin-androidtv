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
				includeVersionByRegex("org\\.jellyfin\\.sdk", ".*", "latest-SNAPSHOT")
			}
		}
		jcenter()
	}
}

plugins {
	id("io.gitlab.arturbosch.detekt").version("1.16.0")
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
