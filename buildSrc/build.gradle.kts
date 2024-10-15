plugins {
	`kotlin-dsl`
}

kotlin {
	jvmToolchain {
		languageVersion.set(JavaLanguageVersion.of(libs.versions.java.jdk.get()))
	}
}

repositories {
	mavenCentral()
}
