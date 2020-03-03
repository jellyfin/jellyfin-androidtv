buildscript {
	repositories {
		jcenter()
		mavenCentral()
		google()
	}

	dependencies {
		classpath("com.android.tools.build:gradle:3.6.0")
		classpath(kotlin("gradle-plugin", "1.3.70"))
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
