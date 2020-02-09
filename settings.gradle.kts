include(":app")

val enableDependencySubstitution = true

val apiclientLocation = "../jellyfin-apiclient-java"
if (File(apiclientLocation).exists() && enableDependencySubstitution) {
	includeBuild(apiclientLocation) {
		dependencySubstitution {
			substitute(module("com.github.jellyfin.jellyfin-apiclient-java:library")).with(project(":library"))
		}
	}
}
