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
