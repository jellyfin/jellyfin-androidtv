plugins {
	id("io.gitlab.arturbosch.detekt").version(Plugins.Versions.detekt)
}

allprojects {
	repositories.defaultRepositories()
}

buildscript {
	repositories.defaultRepositories()

	dependencies {
		classpath(Plugins.androidBuildTools)
		classpath(Plugins.kotlin)
	}
}


// Detekt configuration
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
