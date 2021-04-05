buildscript {
	repositories {
		jcenter()
		google()
	}

	dependencies {
		val kotlinVersion: String by project
		classpath("com.android.tools.build:gradle:4.1.2")
		classpath(kotlin("gradle-plugin", kotlinVersion))
		classpath(kotlin("serialization", kotlinVersion))
	}
}

allprojects {
	// Dependencies
	repositories {
		jcenter()
		google()
		maven { setUrl("https://jitpack.io") }
	}
}

plugins {
	id("io.gitlab.arturbosch.detekt").version("1.14.2")
}

// Detekt configuration
subprojects {
	plugins.apply("io.gitlab.arturbosch.detekt")

	detekt {
		buildUponDefaultConfig = true
		ignoreFailures = true
		config = files("$rootDir/detekt.yml")
	}
}
