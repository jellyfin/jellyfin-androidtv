buildscript {
	repositories {
		jcenter()
		google()
	}

	dependencies {
		val kotlinVersion: String by project
		classpath("com.android.tools.build:gradle:4.0.1")
		classpath(kotlin("gradle-plugin", kotlinVersion))
	}
}

allprojects {
	// Dependencies
	repositories {
		jcenter()
		google()
	}
}

plugins {
	id("io.gitlab.arturbosch.detekt").version("1.9.1")
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
