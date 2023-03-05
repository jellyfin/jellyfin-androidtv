plugins {
	`kotlin-dsl`
}

kotlin {
	jvmToolchain {
		languageVersion.set(JavaLanguageVersion.of("17"))
	}
}

repositories {
	mavenCentral()
}
