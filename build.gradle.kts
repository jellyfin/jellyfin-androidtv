plugins {
	alias(libs.plugins.detekt)
}

buildscript {
	dependencies {
		classpath(libs.android.gradle)
		classpath(libs.kotlin.gradle)
	}
}

subprojects {
	apply<io.gitlab.arturbosch.detekt.DetektPlugin>()

	detekt {
		buildUponDefaultConfig = true
		ignoreFailures = true
		config = files("$rootDir/detekt.yaml")
		basePath = rootDir.absolutePath

		reports {
			sarif.enabled = true
		}
	}
}
