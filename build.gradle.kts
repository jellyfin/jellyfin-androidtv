buildscript {
	repositories {
		jcenter()
		mavenCentral()
		google()
	}

	dependencies {
		classpath("com.android.tools.build:gradle:4.0.0")
		classpath(kotlin("gradle-plugin", "1.3.72"))
	}
}

allprojects {
	repositories {
		jcenter()
		mavenCentral()
		google()
		maven("https://jitpack.io")
		maven("https://dl.bintray.com/videolan/Android")
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
		config = files("$rootDir/detekt.yml")
	}
}
